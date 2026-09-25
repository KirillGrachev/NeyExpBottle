package eu.neydev.expbottle.command;

import eu.neydev.expbottle.NeyExpBottle;
import org.bukkit.command.PluginCommand;
import org.jetbrains.annotations.NotNull;

/**
 * Регистрация команд в одном месте — по образцу {@code EventDispatcher}.
 */
public class CommandDispatcher {

    private final NeyExpBottle plugin;

    public CommandDispatcher(@NotNull NeyExpBottle plugin) {
        this.plugin = plugin;
    }

    public void registerCommands(@NotNull BaseCommand @NotNull ... commands) {

        for (BaseCommand command : commands) {

            PluginCommand pluginCommand = plugin.getCommand(command.getCommandName());

            // Недостижимо в тестах: команды объявлены в plugin.yml
            if (pluginCommand == null) {

                plugin.getLogger().warning("Command /" + command.getCommandName()
                        + " is not declared in plugin.yml - skipping");
                continue;

            }

            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);

        }

    }
}