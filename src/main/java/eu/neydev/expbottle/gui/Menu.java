package eu.neydev.expbottle.gui;

import eu.neydev.expbottle.gui.item.MenuItem;
import eu.neydev.expbottle.registry.MenuDefinition;
import eu.neydev.expbottle.service.PluginServices;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Открытое меню обмена.
 *
 * <p>Содержимое фиксируется в снимке при создании: если администратор сделает
 * reload при открытом окне, игрок не получит другие кнопки под курсором.</p>
 */
public class Menu {

    private final PluginServices services;
    private final MenuDefinition definition;
    private final Player player;
    private final MenuHolder holder;
    private final Inventory inventory;
    private final MenuRenderer renderer;
    private final MenuLayout layout;

    private BukkitTask updateTask;

    public Menu(@NotNull PluginServices services, @NotNull MenuDefinition definition,
                @NotNull Player player) {

        this.services = services;
        this.definition = definition;
        this.player = player;
        this.renderer = new MenuRenderer(services);

        this.holder = new MenuHolder();
        this.inventory = Bukkit.createInventory(holder, definition.size(), buildTitle(definition));

        this.holder.initialize(this, inventory);
        this.layout = MenuLayout.of(definition.items());

        renderer.render(inventory, player, layout);

    }

    /**
     * Открывает меню игроку и запускает автообновление, если оно настроено.
     */
    public void open() {

        player.openInventory(inventory);
        startUpdateTask();

    }

    /**
     * Перерисовывает предметы с {@code refresh: true}.
     */
    public void refresh() {
        renderer.renderDynamic(inventory, player, layout);
    }

    /**
     * Вызывается при закрытии окна: останавливает задачу обновления.
     */
    public void onClose() {
        stopUpdateTask();
    }

    /**
     * Предмет в указанном слоте.
     *
     * @param slot «сырой» слот окна
     * @return предмет или {@code null}, если слот пуст
     */
    public @Nullable MenuItem getItem(int slot) {
        return layout.getItem(slot);
    }

    public @NotNull MenuDefinition getDefinition() {
        return definition;
    }

    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull MenuHolder getHolder() {
        return holder;
    }

    public @NotNull String getName() {
        return definition.name();
    }

    private @NotNull String buildTitle(@NotNull MenuDefinition definition) {

        Placeholders placeholders = services.getPlaceholderService().forPlayer(player);
        return services.getPlaceholderService().format(player, definition.title(), placeholders);

    }

    /**
     * Периодическая перерисовка — аналог {@code update_interval} из DeluxeMenus.
     * Задача принадлежит конкретному окну и снимается при его закрытии.
     */
    private void startUpdateTask() {

        int interval = definition.updateInterval();

        if (interval <= 0) {
            return;
        }

        stopUpdateTask();
        updateTask = Bukkit.getScheduler().runTaskTimer(
                services.getPlugin(), this::refresh, interval, interval);

    }

    private void stopUpdateTask() {

        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

    }
}
