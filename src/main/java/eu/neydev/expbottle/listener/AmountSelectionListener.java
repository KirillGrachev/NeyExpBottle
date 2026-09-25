package eu.neydev.expbottle.listener;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.service.AmountSelectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Чистит выбор количества при выходе игрока, чтобы карта выбора не росла
 * вместе со списком когда-то заходивших ников.
 */
public class AmountSelectionListener implements Listener {

    private final AmountSelectionService amountSelectionService;

    public AmountSelectionListener(@NotNull NeyExpBottle plugin) {
        this.amountSelectionService = plugin.getServices().getAmountSelectionService();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        amountSelectionService.clear(event.getPlayer().getUniqueId());
    }
}