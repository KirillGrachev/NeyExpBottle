package eu.neydev.expbottle.command;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.service.MessageService;
import eu.neydev.expbottle.service.PermissionService;
import eu.neydev.expbottle.service.PluginServices;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * База для команд плагина, зарегистрированных в plugin.yml.
 *
 * <p>Держит {@link CommandContext} с доступом к сервисам и помощникам и отдаёт
 * его подкомандам, поэтому логика доступа и tab-complete не дублируется.
 * Напрямую база оставляет только те помощники, которыми пользуются сами
 * команды верхнего уровня.</p>
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final NeyExpBottle plugin;
    protected final CommandContext context;

    private final String commandName;

    protected BaseCommand(@NotNull NeyExpBottle plugin, @NotNull String commandName) {
        this.plugin = plugin;
        this.commandName = commandName;
        this.context = new CommandContext(plugin);
    }

    /**
     * Имя команды в plugin.yml.
     *
     * @return имя без слэша
     */
    public @NotNull String getCommandName() {
        return commandName;
    }

    protected @NotNull PluginServices services() {
        return context.services();
    }

    protected @NotNull MessageService messages() {
        return context.messages();
    }

    protected @NotNull PermissionService permissions() {
        return context.permissions();
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, String @NotNull [] args) {
        return Collections.emptyList();
    }

    /**
     * Отсеивает варианты подсказки по введённому префиксу.
     */
    protected @NotNull List<String> filter(@NotNull List<String> values, @NotNull String prefix) {
        return context.filter(values, prefix);
    }
}