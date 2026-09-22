package eu.neydev.expbottle.command;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.service.MessageService;
import eu.neydev.expbottle.service.PermissionService;
import eu.neydev.expbottle.service.PluginServices;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * База для команд плагина: доступ к сервисам и мелкие helpers для tab-complete.
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final NeyExpBottle plugin;

    private final String commandName;

    protected BaseCommand(@NotNull NeyExpBottle plugin, @NotNull String commandName) {
        this.plugin = plugin;
        this.commandName = commandName;
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
        return plugin.getServices();
    }

    protected @NotNull MessageService messages() {
        return services().getMessageService();
    }

    protected @NotNull PermissionService permissions() {
        return services().getPermissionService();
    }

    protected @NotNull String version() {
        return plugin.getDescription().getVersion();
    }

    protected @NotNull String author() {

        List<String> authors = plugin.getDescription().getAuthors();
        return authors.isEmpty() ? "Ney" : String.join(", ", authors);

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

        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();

        for (String value : values) {

            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(value);
            }

        }

        return result;

    }

    protected @NotNull List<String> playerNames() {

        List<String> names = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }

        return names;

    }

    protected boolean isSubcommand(@NotNull String raw, @NotNull String... expected) {

        for (String value : expected) {

            if (value.equalsIgnoreCase(raw)) {
                return true;
            }

        }

        return false;

    }
}
