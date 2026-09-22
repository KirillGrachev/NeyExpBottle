package eu.neydev.expbottle.service;

import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.gui.Menu;
import eu.neydev.expbottle.gui.MenuHolder;
import eu.neydev.expbottle.registry.MenuDefinition;
import eu.neydev.expbottle.registry.MenuRegistry;
import eu.neydev.expbottle.util.Placeholders;
import eu.neydev.expbottle.util.ValueResolver;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Открытие и обслуживание меню.
 *
 * <p>Открытое меню ищется через holder инвентаря, поэтому плагин не хранит
 * собственных карт игроков и не может «забыть» их очистить.</p>
 */
public class MenuService {

    private final PluginServices services;
    private final PluginConfig config;
    private final MenuRegistry menuRegistry;
    private final MessageService messageService;
    private final PermissionService permissionService;
    private final PlaceholderService placeholderService;
    private final ActionExecutor actionExecutor;
    private final DiagnosticsService diagnosticsService;

    public MenuService(@NotNull PluginServices services) {

        this.services = services;
        this.config = services.getConfigManager();
        this.menuRegistry = services.getMenuRegistry();
        this.messageService = services.getMessageService();
        this.permissionService = services.getPermissionService();
        this.placeholderService = services.getPlaceholderService();
        this.actionExecutor = services.getActionExecutor();
        this.diagnosticsService = services.getDiagnosticsService();

    }

    /**
     * Открывает меню по умолчанию из config.yml.
     *
     * @param player игрок
     * @return true если меню открыто
     */
    public boolean open(@NotNull Player player) {

        // Если меню по умолчанию переименовали или удалили, открываем первое доступное
        MenuDefinition definition = menuRegistry.getDefault(config.getDefaultMenu());

        if (definition == null) {

            messageService.send(player, MessageKey.MENU_NOT_FOUND,
                    Placeholders.create().set("menu", config.getDefaultMenu()));
            return false;

        }

        return open(player, definition.name());

    }

    /**
     * Открывает меню по имени.
     *
     * @param player   игрок
     * @param menuName имя меню (файл в папке menus/)
     * @return true если меню открыто
     */
    public boolean open(@NotNull Player player, @Nullable String menuName) {

        if (!config.isEnabled()) {
            messageService.send(player, MessageKey.PLUGIN_DISABLED);
            return false;
        }

        if (ValueResolver.isBlank(menuName)) {
            messageService.send(player, MessageKey.MENU_NOT_FOUND, Placeholders.create().set("menu", ""));
            return false;
        }

        MenuDefinition definition = menuRegistry.byName(menuName).orElse(null);

        if (definition == null) {

            messageService.send(player, MessageKey.MENU_NOT_FOUND,
                    Placeholders.create().set("menu", menuName));
            diagnosticsService.debug("Menu '" + menuName + "' not found (request " + player.getName() + ")");
            return false;

        }

        if (!permissionService.has(player, definition.permission())) {
            messageService.send(player, MessageKey.NO_PERMISSION);
            return false;
        }

        Placeholders placeholders = placeholderService.forPlayer(player);

        if (!definition.openRequirement().evaluate(placeholders)) {

            if (definition.denialMessage().isEmpty()) {
                messageService.send(player, MessageKey.MENU_DENIED, placeholders);
            } else {
                messageService.sendText(player, placeholderService.format(player, definition.denialMessage(), placeholders));
            }

            return false;

        }

        new Menu(services, definition, player).open();
        diagnosticsService.incrementMenusOpened();

        actionExecutor.execute(player, definition.openActions(), placeholders);

        return true;

    }

    /**
     * Ищет открытое у игрока меню.
     *
     * @param player игрок
     * @return меню или {@code null}
     */
    public @Nullable Menu findMenu(@NotNull Player player) {

        Inventory topInventory = player.getOpenInventory().getTopInventory();

        if (topInventory != null && topInventory.getHolder() instanceof MenuHolder holder) {
            return holder.getMenu();
        }

        return null;

    }

    public boolean isMenuOpen(@NotNull Player player) {
        return findMenu(player) != null;
    }

    public void refresh(@NotNull Player player) {

        Menu menu = findMenu(player);

        if (menu != null) {
            menu.refresh();
        }

    }

    public void close(@NotNull Player player) {

        if (isMenuOpen(player)) {
            player.closeInventory();
        }

    }

    /**
     * Имена всех загруженных меню — для tab-complete и команды {@code menu}.
     *
     * @return отсортированный список имён
     */
    public @NotNull List<String> getMenuNames() {
        return menuRegistry.getNames();
    }
}
