package eu.neydev.expbottle.command;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.service.MessageService;
import eu.neydev.expbottle.service.PermissionService;
import eu.neydev.expbottle.service.PluginServices;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Общий доступ к сервисам и мелким помощникам команд.
 *
 * <p>Один экземпляр используют и команды верхнего уровня ({@link BaseCommand}),
 * и подкоманды ({@link AdminSubCommand}) — так помощники не дублируются,
 * а логика tab-complete и доступа к сервисам живёт в одном месте.</p>
 */
public class CommandContext {

    private final NeyExpBottle plugin;

    public CommandContext(@NotNull NeyExpBottle plugin) {
        this.plugin = plugin;
    }

    public @NotNull PluginServices services() {
        return plugin.getServices();
    }

    public @NotNull MessageService messages() {
        return services().getMessageService();
    }

    public @NotNull PermissionService permissions() {
        return services().getPermissionService();
    }

    public @NotNull String version() {
        return plugin.getDescription().getVersion();
    }

    public @NotNull String author() {

        List<String> authors = plugin.getDescription().getAuthors();
        return authors.isEmpty() ? "Ney" : String.join(", ", authors);

    }

    /**
     * Проверяет право на админ-команды и отправляет отказ, если его нет.
     *
     * @return true, если команду выполнять нельзя
     */
    public boolean denyIfNotAdmin(@NotNull CommandSender sender) {

        if (permissions().canAdmin(sender)) {
            return false;
        }

        messages().send(sender, MessageKey.NO_PERMISSION);
        return true;

    }

    /**
     * Отсеивает варианты подсказки по введённому префиксу.
     */
    public @NotNull List<String> filter(@NotNull List<String> values, @NotNull String prefix) {

        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();

        for (String value : values) {

            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(value);
            }

        }

        return result;

    }

    public @NotNull List<String> playerNames() {

        List<String> names = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }

        return names;

    }
}