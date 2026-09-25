package eu.neydev.expbottle.command;

import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.util.Placeholders;
import eu.neydev.expbottle.util.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@code /neyexpbottle info [игрок]} — диагностика: версия, ядро, ступени,
 * статистика опыта игрока и состояние нанесения текстур голов.
 *
 * <p>Из консоли дополнительно печатается способ нанесения текстур и счётчики —
 * это первый ответ на вопрос «почему головы пустые».</p>
 */
public class InfoSubCommand extends AdminSubCommand {

    public InfoSubCommand(@NotNull CommandContext context) {
        super(context);
    }

    @Override
    public @NotNull String name() {
        return "info";
    }

    @Override
    public void execute(@NotNull CommandSender sender, String @NotNull [] args) {

        if (context.denyIfNotAdmin(sender)) {
            return;
        }

        Player target = null;

        if (args.length >= 2) {

            target = Bukkit.getPlayerExact(args[1]);

            if (target == null) {
                context.messages().send(sender, MessageKey.PLAYER_NOT_FOUND,
                        Placeholders.create().set("player", args[1]));
                return;
            }

        } else if (sender instanceof Player player) {
            target = player;
        }

        Placeholders placeholders = context.services().getPlaceholderService().forStatistics()
                .set("version", context.version())
                .set("author", context.author())
                .set("server", ServerVersion.getRaw())
                .set("tiers", context.services().getBottleRegistry().size());

        if (target == null) {

            placeholders.set("player", "—")
                    .set("player_level", "—")
                    .set("player_exp", "—")
                    .set("player_progress", "—")
                    .set("player_levels", "—");

        } else {
            placeholders.merge(context.services().getPlaceholderService().forPlayer(target));
        }

        context.messages().send(sender, MessageKey.INFO, placeholders);

        if (!(sender instanceof Player)) {

            context.services().getDiagnosticsService().info("Skull textures: method "
                    + context.services().getSkullTextureService().getWorkingStrategy()
                    + ", applied " + context.services().getSkullTextureService().getAppliedCount()
                    + ", failed " + context.services().getSkullTextureService().getFailedCount());

        }

    }

    @Override
    public @NotNull List<String> suggest(@NotNull CommandSender sender, String @NotNull [] args) {

        if (args.length == 2) {
            return context.filter(context.playerNames(), args[1]);
        }

        return List.of();

    }
}