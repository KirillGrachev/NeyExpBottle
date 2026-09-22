package eu.neydev.expbottle.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Вызывается перед тем, как игрок получит опыт из бутылки.
 *
 * <p>Отмена события сохраняет бутылку в инвентаре — так другие плагины могут
 * запретить использование в своих регионах или режимах.</p>
 */
public class BottleUseEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack item;
    private final int levels;

    private boolean cancelled;

    public BottleUseEvent(@NotNull Player player, @NotNull ItemStack item, int levels) {
        this.player = player;
        this.item = item;
        this.levels = levels;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    /**
     * Предмет, который игрок держит в руке.
     *
     * @return бутылка опыта
     */
    public @NotNull ItemStack getItem() {
        return item;
    }

    /**
     * Сколько уровней получит игрок (в режиме STACK — за весь стак).
     *
     * @return количество уровней
     */
    public int getLevels() {
        return levels;
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
