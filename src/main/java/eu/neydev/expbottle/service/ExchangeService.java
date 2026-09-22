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

        if (!config.isEnabled()) {
            return ExchangeResult.of(ExchangeOutcome.PLUGIN_DISABLED);
        }

        if (levels <= 0 || levels > config.getMaxBottleLevels()) {
            diagnosticsService.suspicious("Invalid exchange requested: " + levels
                    + " levels from " + player.getName());
            return ExchangeResult.of(ExchangeOutcome.INVALID_AMOUNT, levels);
        }

        if (!permissionService.canExchange(player)) {
            return ExchangeResult.of(ExchangeOutcome.NO_PERMISSION, levels);
        }

        if (cooldownService.isOnCooldown(player)) {
            return ExchangeResult.of(ExchangeOutcome.ON_COOLDOWN, levels);
        }

        if (!experienceService.hasLevels(player, levels)) {
            return ExchangeResult.of(ExchangeOutcome.NOT_ENOUGH_LEVELS, levels);
        }

        int requiredBottles = config.areEmptyBottlesRequired() ? 1 : 0;

        if (requiredBottles > 0
                && !inventoryService.has(player, config.getEmptyBottleMaterial(), requiredBottles)) {
            return ExchangeResult.of(ExchangeOutcome.NOT_ENOUGH_BOTTLES, levels);
        }

        BottleExchangeEvent event = new BottleExchangeEvent(player, levels, requiredBottles);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            diagnosticsService.debug("Exchange cancelled by an external plugin: " + player.getName());
            return ExchangeResult.of(ExchangeOutcome.CANCELLED, levels);
        }

        if (requiredBottles > 0
                && !inventoryService.remove(player, config.getEmptyBottleMaterial(), requiredBottles)) {
            // Пузырёк исчез между проверкой и списанием (крайне редкая гонка)
            return ExchangeResult.of(ExchangeOutcome.NOT_ENOUGH_BOTTLES, levels);
        }

        if (!experienceService.removeLevels(player, levels)) {
            diagnosticsService.suspicious("Experience changed during the exchange: " + player.getName());
            return ExchangeResult.of(ExchangeOutcome.NOT_ENOUGH_LEVELS, levels);
        }

        ItemStack bottle = bottleFactory.create(levels);
        inventoryService.giveOrDrop(player, bottle);

        cooldownService.start(player);
        diagnosticsService.incrementBottlesCreated();

        return ExchangeResult.success(levels, requiredBottles, experienceService.getExactLevels(player));

    }
}
