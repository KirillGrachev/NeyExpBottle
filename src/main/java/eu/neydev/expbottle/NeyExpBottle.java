package eu.neydev.expbottle;

import eu.neydev.expbottle.command.AdminCommand;
import eu.neydev.expbottle.command.CommandDispatcher;
import eu.neydev.expbottle.command.ExpCommand;
import eu.neydev.expbottle.config.ConfigManager;
import eu.neydev.expbottle.event.EventDispatcher;
import eu.neydev.expbottle.listener.AmountSelectionListener;
import eu.neydev.expbottle.listener.AntiDupeListener;
import eu.neydev.expbottle.listener.BottleUseListener;
import eu.neydev.expbottle.listener.MenuListener;
import eu.neydev.expbottle.registry.BottleRegistry;
import eu.neydev.expbottle.registry.MenuRegistry;
import eu.neydev.expbottle.service.PluginServices;
import eu.neydev.expbottle.support.PlaceholderSupport;
import eu.neydev.expbottle.util.ServerVersion;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class NeyExpBottle extends JavaPlugin {

    private PluginServices services;

    /**
     * Конструктор без аргументов использует сервер.
     * Второй конструктор ниже нужен только тестам (MockBukkit).
     */
    public NeyExpBottle() {
        super();
    }

    /**
     * Конструктор для MockBukkit: мок-сервер создаёт плагин сам и передаёт
     * описание, папку данных и файл jar'а. В рантайме не используется.
     */
    @SuppressWarnings("deprecation")
    protected NeyExpBottle(@NotNull JavaPluginLoader loader, @NotNull PluginDescriptionFile description,
                           @NotNull File dataFolder, @NotNull File file) {
        super(loader, description, dataFolder, file);
    }

    @Override
    public void onEnable() {

        this.services = new PluginServices(this);

        // Регистрация команд
        new CommandDispatcher(this).registerCommands(
                new ExpCommand(this),
                new AdminCommand(this)
        );

        // Регистрация слушателей
        new EventDispatcher(this).registerEvents(
                new MenuListener(this),
                new BottleUseListener(this),
                new AntiDupeListener(this),
                new AmountSelectionListener(this)
        );

        // Интеграция с PlaceholderAPI (если установлен)
        PlaceholderSupport.registerIfPresent(this);

        getLogger().info("Core: " + ServerVersion.getRaw()
                + ", HEX colors: " + (ServerVersion.isHexSupported() ? "yes" : "no (nearest color fallback)"));
        getLogger().info("Menus loaded: " + services.getMenuRegistry().size()
                + ", exchange buttons: " + services.getBottleRegistry().size());
        getLogger().info("NeyExpBottle started successfully!");

    }

    @Override
    public void onDisable() {

        if (services != null) {
            services.shutdown();
        }

        getLogger().info("NeyExpBottle stopped!");

    }

    public @NotNull PluginServices getServices() {
        return services;
    }

    public @NotNull ConfigManager getConfigManager() {
        return services.getConfigManager();
    }

    public @NotNull MenuRegistry getMenuRegistry() {
        return services.getMenuRegistry();
    }

    public @NotNull BottleRegistry getBottleRegistry() {
        return services.getBottleRegistry();
    }
}