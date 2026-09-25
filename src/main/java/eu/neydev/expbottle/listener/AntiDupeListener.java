package eu.neydev.expbottle.listener;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.service.BottleTagService;
import eu.neydev.expbottle.service.DiagnosticsService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Защита от копирования бутылок через креатив.
 *
 * <p>Действия креативного инвентаря клонируют предмет вместе с NBT, поэтому
 * копия бит-в-бит неотличима от оригинала. Единственный надёжный способ —
 * запретить сами креативные действия над нашими бутылками: клонирование
 * средней кнопкой, подмену слота и перенос из креативного меню.</p>
 */
public class AntiDupeListener implements Listener {

    private final BottleTagService tagService;
    private final DiagnosticsService diagnosticsService;

    public AntiDupeListener(@NotNull NeyExpBottle plugin) {

        this.tagService = plugin.getServices().getBottleTagService();
        this.diagnosticsService = plugin.getServices().getDiagnosticsService();

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreativeClone(@NotNull InventoryCreativeEvent event) {

        if (!tagService.isBottle(event.getCurrentItem()) && !tagService.isBottle(event.getCursor())) {
            return;
        }

        // Молчаливая отмена: без сообщений в консоль и без сообщений игроку.
        // Клонирование наших бутылок в креативе запрещено всегда.
        event.setCancelled(true);
        diagnosticsService.countSuspicious();

    }
}