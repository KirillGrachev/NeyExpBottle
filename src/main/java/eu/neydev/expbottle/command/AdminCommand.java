package eu.neydev.expbottle.command;

import eu.neydev.expbottle.NeyExpBottle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Команда {@code /neyexpbottle} — диспетчер подкоманд.
 *
 * <p>Весь текст (включая справку) берётся из config.yml: в старой версии
 * помощь была зашита в Java-код и не переводилась. Логика каждой подкоманды
 * живёт в своём {@link AdminSubCommand}; этот класс лишь выбирает нужную
 * по первому аргументу и передаёт ей выполнение и подсказки.</p>
 */
public class AdminCommand extends BaseCommand {

    private static final List<String> SUBCOMMANDS = List.of("reload", "give", "info", "menu", "help");
    private static final String DEFAULT_SUBCOMMAND = "help";

    private final Map<String, AdminSubCommand> handlers = new LinkedHashMap<>();
    private final AdminSubCommand fallback;

    public AdminCommand(@NotNull NeyExpBottle plugin) {

        super(plugin, "neyexpbottle");

        register(new ReloadSubCommand(context));
        register(new GiveSubCommand(context));
        register(new InfoSubCommand(context));
        register(new MenuSubCommand(context));
        register(new HelpSubCommand(context));

        this.fallback = handlers.get(DEFAULT_SUBCOMMAND);

    }

    private void register(@NotNull AdminSubCommand subCommand) {
        handlers.put(subCommand.name(), subCommand);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {

        AdminSubCommand handler = resolve(args);
        handler.execute(sender, args);

        return true;

    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, String @NotNull [] args) {

        if (!permissions().canAdmin(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        AdminSubCommand handler = handlers.get(args[0].toLowerCase(Locale.ROOT));
        return handler == null ? Collections.emptyList() : handler.suggest(sender, args);

    }

    /**
     * Подкоманда по первому аргументу; при пустом или неизвестном — справка.
     */
    private @NotNull AdminSubCommand resolve(String @NotNull [] args) {

        if (args.length == 0) {
            return fallback;
        }

        return handlers.getOrDefault(args[0].toLowerCase(Locale.ROOT), fallback);

    }
}