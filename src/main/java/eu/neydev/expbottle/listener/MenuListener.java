package eu.neydev.expbottle.listener;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.service.PluginServices;
import eu.neydev.expbottle.event.MenuInteractionHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Слушатель меню обмена.
 *
 * <p>Меню определяется по holder'у инвентаря, поэтому никаких карт
 * «игрок -> GUI» и утечек при перезагрузке плагина здесь нет.</p>
 */
public class MenuListener implements Listener {

    private final MenuInteractionHandler menuInteractionHandler;

    public MenuListener(@NotNull NeyExpBottle plugin) {
        PluginServices services = plugin.getServices();
        this.menuInteractionHandler = new MenuInteractionHandler(
                services.getConfigManager(), services.getActionExecutor(), services.getMessageService(),
                services.getSoundService(), services.getPlaceholderService(), services.getExchangeService(),
                services.getDiagnosticsService(), services.getMenuService(), services.getAmountSelectionService());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(@NotNull InventoryClickEvent event) {
        menuInteractionHandler.onMenuClick(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuDrag(@NotNull InventoryDragEvent event) {
        menuInteractionHandler.onMenuDrag(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMenuClose(@NotNull InventoryCloseEvent event) {
        menuInteractionHandler.onMenuClose(event);
    }
}