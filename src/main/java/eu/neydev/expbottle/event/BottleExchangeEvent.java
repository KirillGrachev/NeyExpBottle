package eu.neydev.expbottle.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Вызывается перед обменом опыта на бутылку.
 *
 * <p>Событие летит после всех внутренних проверок (права, кулдаун, опыт,
 * пустые пузырьки), поэтому отмена означает «внешний плагин против сделки».</p>
 */
public class BottleExchangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int levels;
    private final int requiredBottles;

    private boolean cancelled;

    public BottleExchangeEvent(@NotNull Player player, int levels, int requiredBottles) {
        this.player = player;
        this.levels = levels;
        this.requiredBottles = requiredBottles;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    /**
     * Сколько уровней списывается с игрока.
     *
     * @return количество уровней
     */
    public int getLevels() {
        return levels;
    }

    /**
     * Сколько пустых пузырьков требуется (0, если требование выключено).
     *
     * @return количество пузырьков
     */
    public int getRequiredBottles() {
        return requiredBottles;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}