package eu.neydev.expbottle.service;

import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.event.BottleExchangeEvent;
import eu.neydev.expbottle.model.ExchangeOutcome;
import eu.neydev.expbottle.model.ExchangeResult;
import eu.neydev.expbottle.registry.BottleTier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Обмен уровней на бутылку опыта.
 *
 * <p>Сервис ничего не пишет в чат и не играет звуки — он возвращает причину.
 * Так одну и ту же сделку используют меню, команда {@code /neyexpbottle give}
 * и сторонние плагины.</p>
 *
 * <p>Порядок операций защищает от дюпа: сначала валидация, затем событие,
 * затем списание ресурсов и только потом выдача предмета. Если выдача не
 * помещается в инвентарь — предмет падает на землю, опыт не сгорает.</p>
 */
public class ExchangeService {

    /** Значение «сколько сможешь» для аргумента количества. */
    public static final int AMOUNT_ALL = -1;

    /** Верхняя граница разовой операции: девять стаков по 64. */
    public static final int MAX_AMOUNT = 576;


    private final PluginConfig config;
    private final BottleFactory bottleFactory;
    private final ExperienceService experienceService;
    private final InventoryService inventoryService;
    private final PermissionService permissionService;
    private final CooldownService cooldownService;
    private final DiagnosticsService diagnosticsService;

    public ExchangeService(@NotNull PluginConfig config, @NotNull BottleFactory bottleFactory,
                           @NotNull ExperienceService experienceService,
                           @NotNull InventoryService inventoryService,
                           @NotNull PermissionService permissionService,
                           @NotNull CooldownService cooldownService,
                           @NotNull DiagnosticsService diagnosticsService) {
        this.config = config;
        this.bottleFactory = bottleFactory;
        this.experienceService = experienceService;
        this.inventoryService = inventoryService;
        this.permissionService = permissionService;
        this.cooldownService = cooldownService;
        this.diagnosticsService = diagnosticsService;
    }

    /**
     * Обменивает уровни игрока на бутылку.
     *
     * @param player игрок
     * @param tier   уровень обмена из меню
     * @return результат обмена
     */
    public @NotNull ExchangeResult exchange(@NotNull Player player, @NotNull BottleTier tier) {
        return exchange(player, tier.levels());
    }

    /**
     * Обменивает уровни игрока на бутылку.
     *
     * @param player игрок
     * @param levels сколько уровней списать
     * @return результат обмена
     */
    public @NotNull ExchangeResult exchange(@NotNull Player player, int levels) {
        return exchange(player, levels, 1);
    }

    /**
     * Обменивает уровни на несколько бутылок за одну операцию.
     *
     * <p>«Сколько сможешь» при нуле доступных бутылок возвращает настоящую
     * причину отказа ({@link ExchangeOutcome#NOT_ENOUGH_LEVELS} или
     * {@link ExchangeOutcome#NOT_ENOUGH_BOTTLES}), а не
     * {@link ExchangeOutcome#INVALID_AMOUNT}: сообщение в чате совпадает
     * с тем, что игрок видит в лоре витрины.</p>
     *
     * @param player     игрок
     * @param perBottle  уровней в одной бутылке
     * @param amount     сколько бутылок создать, либо {@link #AMOUNT_ALL}
     * @return результат обмена
     */
    public @NotNull ExchangeResult exchange(@NotNull Player player, int perBottle, int amount) {

        Plan plan = plan(player, perBottle, amount, true);

        if (!plan.ok()) {
            return ExchangeResult.of(plan.outcome(), plan.total());
        }

        BottleExchangeEvent event = new BottleExchangeEvent(player, plan.total(), plan.requiredBottles());
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            diagnosticsService.debug("Exchange cancelled by an external plugin: " + player.getName());
            return ExchangeResult.of(ExchangeOutcome.CANCELLED, plan.total());
        }

        if (plan.requiredBottles() > 0
                && !inventoryService.remove(player, config.getEmptyBottleMaterial(), plan.requiredBottles())) {
            // Пузырьки исчезли между проверкой и списанием (крайне редкая гонка)
            return ExchangeResult.of(ExchangeOutcome.NOT_ENOUGH_BOTTLES, plan.total());
        }

        if (!experienceService.removeLevels(player, plan.total())) {
            diagnosticsService.suspicious("Experience changed during the exchange: " + player.getName());
            return ExchangeResult.of(ExchangeOutcome.NOT_ENOUGH_LEVELS, plan.total());
        }

        giveBottles(player, perBottle, plan.count());

        cooldownService.start(player);
        diagnosticsService.incrementBottlesCreated();

        return ExchangeResult.success(plan.total(), plan.requiredBottles(), plan.count(),
                experienceService.getExactLevels(player));

    }

    /**
     * Хватит ли игроку ресурсов на обмен выбранным количеством бутылок.
     *
     * @param player    игрок
     * @param perBottle уровней в одной бутылке
     * @param amount    выбранное количество либо {@link #AMOUNT_ALL}
     * @return true если обмен выполним прямо сейчас
     */
    public boolean affordable(@NotNull Player player, int perBottle, int amount) {
        return availability(player, perBottle, amount).isSuccess();
    }

    /**
     * Причина, по которой обмен выбранным количеством бутылок выполним или нет.
     *
     * <p>Единственная точка правды для витрины: статус в лоре, блеск кнопки и
     * сама сделка смотрят на одни и те же проверки, поэтому витрина не может
     * обещать обмен, который завершится отказом, и не называет ложную причину:
     * нехватка уровней и нехватка пустых пузырьков различимы.</p>
     *
     * <p>Порядок проверок тот же, что в {@link #exchange(Player, int, int)}:
     * уровни на все бутылки сразу, затем пустые пузырьки, если они включены.
     * Количество вне допустимых границ даёт {@link ExchangeOutcome#INVALID_AMOUNT}.</p>
     *
     * @param player    игрок
     * @param perBottle уровней в одной бутылке
     * @param amount    выбранное количество либо {@link #AMOUNT_ALL}
     * @return {@link ExchangeOutcome#SUCCESS} либо причину отказа
     */
    public @NotNull ExchangeOutcome availability(@NotNull Player player, int perBottle, int amount) {
        return plan(player, perBottle, amount, false).outcome();
    }

    /**
     * Единый пайплайн проверок сделки: оба публичных пути берут план отсюда,
     * поэтому витрина и сама сделка не могут разойтись ни порядком проверок,
     * ни названной причиной отказа.
     *
     * <p>Порядок: границы уровней кнопки, количество (для {@code all} — сколько
     * потянет игрок), границы количества с настоящей причиной для «сколько
     * сможешь», переполнение суммарных уровней, затем право и кулдаун
     * (только в полной проверке: витрина за них не говорит), уровни на всё
     * количество и пустые пузырьки.</p>
     *
     * @param player    игрок
     * @param perBottle уровней в одной бутылке
     * @param amount    количество либо {@link #AMOUNT_ALL}
     * @param full      полная проверка для сделки либо только ресурсы для витрины
     * @return план сделки с причиной отказа, если сделка невозможна
     */
    private @NotNull Plan plan(@NotNull Player player, int perBottle, int amount, boolean full) {

        if (full && !config.isEnabled()) {
            return new Plan(ExchangeOutcome.PLUGIN_DISABLED, 0, 0, 0);
        }

        if (perBottle <= 0 || perBottle > config.getMaxBottleLevels()) {

            if (full) {
                diagnosticsService.suspicious("Invalid exchange requested: " + perBottle
                        + " levels from " + player.getName());
            }

            return new Plan(ExchangeOutcome.INVALID_AMOUNT, 0, perBottle, 0);

        }

        int count = amount == AMOUNT_ALL ? affordableAmount(player, perBottle) : amount;

        if (count < 1 || count > MAX_AMOUNT) {

            // «Сколько сможешь» обратилось в нуль из-за уровней или пузырьков:
            // называем настоящую причину, иначе игрок получит бессмысленное
            // «invalid value» при допустимом значении
            if (amount == AMOUNT_ALL) {

                ExchangeOutcome reason = experienceService.hasLevels(player, perBottle)
                        ? ExchangeOutcome.NOT_ENOUGH_BOTTLES
                        : ExchangeOutcome.NOT_ENOUGH_LEVELS;
                return new Plan(reason, 0, perBottle, 0);

            }

            // Фиксированное количество вне границ — ошибка конфигурации меню
            return new Plan(ExchangeOutcome.INVALID_AMOUNT, 0, perBottle, 0);

        }

        long totalLevels = (long) perBottle * count;

        // Недостижимо: 576 бутылок по 1000 уровней не переполняют int;
        // защита от конфигураций будущего с поднятыми границами
        if (totalLevels > Integer.MAX_VALUE) {
            return new Plan(ExchangeOutcome.INVALID_AMOUNT, 0, perBottle, 0);
        }

        int total = (int) totalLevels;

        if (full && permissionService.exchangeDenied(player)) {
            return new Plan(ExchangeOutcome.NO_PERMISSION, count, total, 0);
        }

        if (full && cooldownService.isOnCooldown(player)) {
            return new Plan(ExchangeOutcome.ON_COOLDOWN, count, total, 0);
        }

        if (!experienceService.hasLevels(player, total)) {
            return new Plan(ExchangeOutcome.NOT_ENOUGH_LEVELS, count, total, 0);
        }

        int requiredBottles = config.areEmptyBottlesRequired() ? count : 0;

        if (requiredBottles > 0
                && !inventoryService.has(player, config.getEmptyBottleMaterial(), requiredBottles)) {
            return new Plan(ExchangeOutcome.NOT_ENOUGH_BOTTLES, count, total, 0);
        }

        return new Plan(ExchangeOutcome.SUCCESS, count, total, requiredBottles);

    }

    /**
     * План сделки: проверенные количества и суммарные уровни.
     *
     * @param outcome       исход проверки ({@link ExchangeOutcome#SUCCESS} — сделка выполнима)
     * @param count         сколько бутылок создаёт сделка
     * @param total         сколько уровней списывает сделка суммарно
     * @param requiredBottles сколько пустых пузырьков требует сделка
     */
    private record Plan(@NotNull ExchangeOutcome outcome, int count, int total, int requiredBottles) {

        boolean ok() {
            return outcome.isSuccess();
        }
    }

    /**
     * Сколько бутылок игрок может позволить себе прямо сейчас:
     * по уровням и по пустым пузырькам, не больше стака.
     */
    private int affordableAmount(@NotNull Player player, int perBottle) {

        int byLevels = (int) Math.floor(experienceService.getExactLevels(player) / perBottle);

        if (!config.areEmptyBottlesRequired()) {
            return Math.min(byLevels, MAX_AMOUNT);
        }

        int byBottles = inventoryService.count(player, config.getEmptyBottleMaterial());
        return Math.min(Math.min(byLevels, byBottles), MAX_AMOUNT);

    }

    /**
     * Выдаёт бутылки стаками до 64 штук, излишек падает на землю.
     */
    private void giveBottles(@NotNull Player player, int perBottle, int count) {

        int left = count;

        while (left > 0) {

            int stack = Math.min(64, left);
            ItemStack bottle = bottleFactory.create(perBottle);
            bottle.setAmount(stack);

            inventoryService.giveOrDrop(player, bottle);
            left -= stack;

        }

    }
}

