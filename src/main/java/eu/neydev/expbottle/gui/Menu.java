package eu.neydev.expbottle.gui;

import eu.neydev.expbottle.gui.item.MenuItem;
import eu.neydev.expbottle.registry.MenuDefinition;
import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.service.PlaceholderService;
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

    private final MenuRenderer renderer;
    private final PlaceholderService placeholderService;
    private final NeyExpBottle plugin;
    private final MenuDefinition definition;
    private final Player player;
    private final MenuHolder holder;
    private final Inventory inventory;
    private final MenuLayout layout;
    private final Menu parent;
    private final Placeholders context;

    private BukkitTask updateTask;

    public Menu(@NotNull MenuRenderer renderer, @NotNull PlaceholderService placeholderService,
                @NotNull NeyExpBottle plugin, @NotNull MenuDefinition definition, @NotNull Player player) {
        this(renderer, placeholderService, plugin, definition, player, null, Placeholders.create());
    }

    /**
     * @param renderer         отрисовщик предметов меню
     * @param placeholderService плейсхолдеры заголовка и динамических строк
     * @param plugin           плагин: на нём живёт задача автообновления
     * @param definition       описание меню
     * @param player           зритель
     * @param parent           меню, из которого открыли это, или {@code null}
     * @param context          плейсхолдеры родителя (уровни кнопки обмена и т.п.)
     */
    public Menu(@NotNull MenuRenderer renderer, @NotNull PlaceholderService placeholderService,
                @NotNull NeyExpBottle plugin, @NotNull MenuDefinition definition,
                @NotNull Player player, @Nullable Menu parent, @NotNull Placeholders context) {

        this.renderer = renderer;
        this.placeholderService = placeholderService;
        this.plugin = plugin;
        this.definition = definition;
        this.player = player;
        this.parent = parent;
        this.context = context;

        this.holder = new MenuHolder();
        this.inventory = Bukkit.createInventory(holder, definition.size(), buildTitle(definition));

        this.holder.initialize(this, inventory);
        this.layout = MenuLayout.of(definition.items());

        renderer.render(inventory, player, definition.name(), layout, context);

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
        renderer.renderDynamic(inventory, player, definition.name(), layout, context);
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

    /**
     * Меню, из которого открыли это окно.
     *
     * @return родительское меню или {@code null}, если окно открыто само по себе
     */
    public @Nullable Menu getParent() {
        return parent;
    }

    /**
     * Плейсхолдеры, принесённые из родительского меню.
     *
     * @return контекст (пустой набор, если меню открыто само по себе)
     */
    public @NotNull Placeholders getContext() {
        return context;
    }

    private @NotNull String buildTitle(@NotNull MenuDefinition definition) {

        Placeholders placeholders = placeholderService.forMenu(player, definition.name());
        placeholders.merge(context);

        return placeholderService.format(player, definition.title(), placeholders);

    }

    /**
     * Периодическая перерисовка динамических предметов по {@code update_interval}.
     * Задача принадлежит конкретному окну и снимается при его закрытии.
     */
    private void startUpdateTask() {

        int interval = definition.updateInterval();

        if (interval <= 0) {
            return;
        }

        stopUpdateTask();
        updateTask = Bukkit.getScheduler().runTaskTimer(
                plugin, this::refresh, interval, interval);

    }

    private void stopUpdateTask() {

        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

    }
}