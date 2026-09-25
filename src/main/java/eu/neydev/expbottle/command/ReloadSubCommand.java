package eu.neydev.expbottle.command;

import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /neyexpbottle reload} — перечитывает конфиг и меню.
 *
 * <p>После перезагрузки заново подбирается способ нанесения текстур голов,
 * поэтому состояние {@link eu.neydev.expbottle.service.SkullTextureService} сбрасывается.</p>
 */
public class ReloadSubCommand extends AdminSubCommand {

    public ReloadSubCommand(@NotNull CommandContext context) {
        super(context);
    }

    @Override
    public @NotNull String name() {
        return "reload";
    }

    @Override
    public void execute(@NotNull CommandSender sender, String @NotNull [] args) {

        if (context.denyIfNotAdmin(sender)) {
            return;
        }

        context.messages().send(sender, MessageKey.RELOAD_START);

        try {

            context.services().getSkullTextureService().reset();

            long time = context.services().reload();

            Placeholders placeholders = Placeholders.create()
                    .set("time", time)
                    .set("tiers", context.services().getBottleRegistry().size());

            context.messages().send(sender, MessageKey.RELOAD_SUCCESS, placeholders);
            context.messages().send(sender, MessageKey.RELOAD_TIME, placeholders);

            context.services().getDiagnosticsService().info("Configuration reloaded (" + sender.getName()
                    + "), time: " + time + " ms, exchange tiers: " + context.services().getBottleRegistry().size()
                    + ", skull textures: " + context.services().getSkullTextureService().getWorkingStrategy());

        } catch (Exception exception) {

            context.messages().send(sender, MessageKey.RELOAD_ERROR);
            context.services().getDiagnosticsService().severe("Error while reloading configuration:", exception);

        }

    }
}