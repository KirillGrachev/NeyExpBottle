package eu.neydev.expbottle.command;

import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /neyexpbottle help} — справка из config.yml.
 *
 * <p>Единственная подкоманда без проверки прав: справка доступна всем,
 * а выполняется по умолчанию, когда подкоманда не указана или неизвестна.</p>
 */
public class HelpSubCommand extends AdminSubCommand {

    public HelpSubCommand(@NotNull CommandContext context) {
        super(context);
    }

    @Override
    public @NotNull String name() {
        return "help";
    }

    @Override
    public void execute(@NotNull CommandSender sender, String @NotNull [] args) {

        context.messages().send(sender, MessageKey.HELP, Placeholders.create()
                .set("version", context.version())
                .set("author", context.author())
                .set("tiers", context.services().getBottleRegistry().size()));

    }
}