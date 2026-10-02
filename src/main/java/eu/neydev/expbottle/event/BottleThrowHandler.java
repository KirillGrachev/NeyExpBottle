package eu.neydev.expbottle.event;

import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.model.BottleData;
import eu.neydev.expbottle.service.BottleTagService;
import eu.neydev.expbottle.service.DiagnosticsService;
import eu.neydev.expbottle.service.ExperienceService;
import eu.neydev.expbottle.service.MessageService;
import eu.neydev.expbottle.service.SoundService;
import eu.neydev.expbottle.service.PluginServices;
import eu.neydev.expbottle.util.ExperienceFormula;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Бросок бутылки и её поломка.
 *
 * <p>Бутылка ведёт себя как ванильная: правый клик бросает снаряд, при ударе он
 * разбивается. Прямое «впитать по клику» убрано намеренно — один клик не может
 * делать две разные вещи.</p>
 *
 * <p>Награда при поломке — уровни ближайшему игроку в радиусе подбора: сырые очки
 * опыта на высоком уровне не дали бы и одного уровня за бутылку с пятью.
 * Орбы сыпятся только когда рядом никого нет. Количество подменяется в
 * {@link ExpBottleEvent} — единственной точке, где можно изменить награду,
 * не трогая NMS-код.</p>
 *
 * <p>Safe-режим ({@code settings.bottle.safe_mode}) — второе поведение, выводимое
 * в коде, а не отдельным рубильником: бутылка используется в момент клика.
 * Уровни сразу уходят ближайшему игроку в радиусе подбора — то есть самому
 * метнувшему, — снаряд не создаётся вовсе, и орбов не бывает структурно.
 * Обычный режим остаётся ванильным броском.</p>
 */
public class BottleThrowHandler {

    private final PluginConfig config;
    private final BottleTagService tagService;
    private final MessageService messageService;
    private final SoundService soundService;
    private final ExperienceService experienceService;
    private final DiagnosticsService diagnosticsService;

    public BottleThrowHandler(@NotNull PluginServices services) {
        this.config = services.getConfigManager();
        this.tagService = services.getBottleTagService();
        this.messageService = services.getMessageService();
        this.soundService = services.getSoundService();
        this.experienceService = services.getExperienceService();
        this.diagnosticsService = services.getDiagnosticsService();
    }

    /**
     * Решает, разрешить ли бросок нашей бутылки.
     *
     * <p>Событие не отменяется для обычных бутылок и для наших в обычном режиме:
     * отмена нужна только когда бутылка повреждена или подделана, предмет не
     * является снарядом, либо включён safe-режим — тогда бутылка используется
     * в момент клика и ванильный снаряд не создаётся.</p>
     *
     * @param event событие взаимодействия
     */
    public void onThrowAttempt(@NotNull PlayerInteractEvent event) {

        if (!isThrowAction(event.getAction())) {
            return;
        }

        // Правый клик приходит дважды — для основной и для второй руки.
        // Обрабатываем только основную, иначе проверка шла бы дважды.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        BottleData data = tagService.read(item);

        if (!data.bottle()) {
            return;
        }

        Player player = event.getPlayer();

        if (data.forged()) {

            event.setCancelled(true);
            reject(player, data, MessageKey.BOTTLE_FORGED, "forged");

            return;

        }

        if (data.isBroken(config.getMaxBottleLevels())) {

            event.setCancelled(true);
            reject(player, data, MessageKey.BOTTLE_BROKEN, "damaged tag");

            return;

        }

        // Ванильный снаряд существует только у EXPERIENCE_BOTTLE: с другим
        // материалом предмета из конфига клик в обычном режиме просто гасим.
        // Safe-режиму материал не важен: бутылка используется без снаряда
        if (!config.isSafeMode() && item.getType() != Material.EXPERIENCE_BOTTLE) {
            event.setCancelled(true);
            return;
        }

        BottleUseEvent useEvent = new BottleUseEvent(player, item, data.levels());
        Bukkit.getPluginManager().callEvent(useEvent);

        if (useEvent.isCancelled()) {

            event.setCancelled(true);
            diagnosticsService.debug("Throw cancelled by an external plugin: " + player.getName());

            return;

        }

        // Safe-режим: бутылка используется в момент броска, ванильный снаряд
        // не создаётся — вместо него летит наш, уже помеченный использованным
        if (config.isSafeMode()) {
            useOnThrow(event, player, item);
        }

    }

    /**
     * Разбивание бутылки: подменяем ванильную награду на сохранённый опыт.
     *
     * @param event событие разбивания
     */
    public void onBottleBreak(@NotNull ExpBottleEvent event) {

        ThrownExpBottle projectile = event.getEntity();

        Object shooter = projectile.getShooter();
        Player player = shooter instanceof Player owner ? owner : null;

        event.setExperience(breakExperience(projectile.getItem(), projectile.getLocation(),
                player, event.getExperience()));

    }

    /**
     * Награда при поломке снаряда.
     *
     * <p>Использованный снаряд safe-режима не даёт ничего: награда уже ушла
     * метнувшему в момент броска. Ванильная бутылка и любой чужой предмет
     * сохраняют свою собственную награду — мы её не трогаем.</p>
     *
     * @param item              предмет снаряда
     * @param location          точка удара
     * @param thrower           бросивший игрок или {@code null}
     * @param vanillaExperience текущая награда события (ванильная)
     * @return опыт, который нужно оставить в событии
     */
    public int breakExperience(@Nullable ItemStack item, @Nullable Location location,
                               @Nullable Player thrower, int vanillaExperience) {

        if (tagService.isSpent(item)) {
            return 0;
        }

        if (!tagService.isBottle(item)) {
            return vanillaExperience;
        }

        return handleBreak(item, location, thrower);

    }

    /**
     * Safe-режим: бутылка используется прямо в клике.
     *
     * <p>Ванильный снаряд гасится и не создаётся вовсе: один предмет уходит
     * из руки, уровни сразу начисляются ближайшему игроку в радиусе подбора —
     * то есть метнувшему, он всегда стоит в точке клика. Орбов не бывает
     * структурно: ядру просто нечего разбивать и награждать повторно.</p>
     *
     * @param event  событие взаимодействия (гасится)
     * @param player метнувший игрок
     * @param item   бутылка в руке
     */
    private void useOnThrow(@NotNull PlayerInteractEvent event, @NotNull Player player, @NotNull ItemStack item) {

        event.setCancelled(true);

        consumeOne(player);

        // Снаряда в safe-режиме нет вовсе: награда начислена здесь, и ядру
        // нечего разбивать позже — повторное начисление исключено структурно
        handleBreak(item, player.getLocation(), player);
        soundService.play(player, config.getBreakSound());

        diagnosticsService.debug("Safe mode: a bottle was used on throw by " + player.getName());

    }

    /**
     * Убирает одну бутылку из основной руки.
     *
     * @param player метнувший игрок
     */
    private void consumeOne(@NotNull Player player) {

        ItemStack held = player.getInventory().getItemInMainHand();

        if (held.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            return;
        }

        held.setAmount(held.getAmount() - 1);
        player.getInventory().setItemInMainHand(held);

    }

    /**
     * Разбивание бутылки: уровни получает игрок рядом, как если бы он поднял орбы.
     *
     * <p>Сырые очки опыта не подходят: 55 очков на 30 уровне не дают и одного
     * уровня, хотя бутылка хранит 5 уровней. Поэтому рядом стоящему игроку
     * начисляются именно уровни, а орбы сыпятся только когда рядом никого нет —
     * так опыт не теряется.</p>
     *
     * @param item     предмет снаряда
     * @param location точка удара
     * @param thrower  бросивший игрок или {@code null}
     * @return опыт для орбов, если рядом нет игрока, иначе 0
     */
    public int handleBreak(@Nullable ItemStack item, @Nullable Location location, @Nullable Player thrower) {

        BottleData data = tagService.read(item);

        // Чужой предмет: ванильная бутылка и любой предмет без нашей метки
        // не дают награды и не попадают в счётчик
        if (!data.bottle()) {
            return 0;
        }

        if (data.forged()) {
            notify(thrower, MessageKey.BOTTLE_FORGED,
                    "A forged bottle broke without a reward", thrower, data);
            return 0;
        }

        if (data.isBroken(config.getMaxBottleLevels())) {
            notify(thrower, MessageKey.BOTTLE_BROKEN,
                    "A damaged bottle broke without a reward", thrower, data);
            return 0;
        }

        Player receiver = nearestPlayer(location, config.getPickupRadius());

        if (receiver != null) {

            experienceService.addLevels(receiver, data.levels());
            soundService.play(receiver, config.getBreakSound());
            diagnosticsService.incrementBottlesUsed();
            diagnosticsService.debug("A bottle with " + data.levels()
                    + " levels broke next to " + receiver.getName());

            return 0;

        }

        // Рядом никого: сыпем орбы, чтобы опыт не потерялся
        diagnosticsService.incrementBottlesUsed();
        return ExperienceFormula.expFromLevels(data.levels());

    }

    /**
     * Ближайший игрок в радиусе подбора.
     *
     * @param location точка удара
     * @param radius   радиус из конфига
     * @return игрок или {@code null}
     */
    private @Nullable Player nearestPlayer(@Nullable Location location, double radius) {

        if (location == null || location.getWorld() == null) {
            return null;
        }

        Player nearest = null;
        double best = radius * radius;

        for (Player player : location.getWorld().getPlayers()) {

            double distance = player.getLocation().distanceSquared(location);

            if (distance <= best) {
                best = distance;
                nearest = player;
            }

        }

        return nearest;

    }

    private void notify(@Nullable Player player, @NotNull MessageKey key,
                        @NotNull String reason, @Nullable Player context, @NotNull BottleData data) {

        if (player != null) {
            messageService.send(player, key);
        }

        diagnosticsService.suspicious(reason + (context == null ? "" : ": " + context.getName())
                + " (" + data.levels() + " levels)");

    }

    private void reject(@NotNull Player player, @NotNull BottleData data,
                        @NotNull MessageKey key, @NotNull String reason) {
        messageService.send(player, key);
        diagnosticsService.suspicious(reason + " by " + player.getName() + ": " + data.levels() + " levels");
    }

    private boolean isThrowAction(@NotNull Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }
}