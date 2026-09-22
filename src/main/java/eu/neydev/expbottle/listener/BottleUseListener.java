package eu.neydev.expbottle.listener;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.event.BottleThrowHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Слушатель броска и разбивания бутылки.
 *
 * <p>{@code ignoreCancelled} не включён: плагины защиты регионов отменяют
 * взаимодействие с блоком, но бросок предмета в руке — наша зона ответственности.</p>
 */
public class BottleUseListener implements Listener {

    private final BottleThrowHandler bottleThrowHandler;

    public BottleUseListener(@NotNull NeyExpBottle plugin) {
        this.bottleThrowHandler = new BottleThrowHandler(plugin.getServices());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        bottleThrowHandler.onThrowAttempt(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBottleBreak(@NotNull ExpBottleEvent event) {
        bottleThrowHandler.onBottleBreak(event);
    }
}
