package eu.neydev.expbottle.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Регистрация слушателей в одном месте — по образцу {@code EventDispatcher} из NeyAntiSkull.
 */
public class EventDispatcher {

    private final JavaPlugin plugin;

    public EventDispatcher(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerEvents(Listener @NotNull ... listeners) {

        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }

    }
}
