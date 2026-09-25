package eu.neydev.expbottle.command;

import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@code /neyexpbottle menu <имя> [игрок]} — открывает меню адресно.
 *
 * <p>Удобно проверять разметку без перезахода: консоль может открыть меню
 * любому игроку, а игрок без аргумента — себе.</p>
 */
public class MenuSubCommand extends AdminSubCommand {

    public MenuSubCommand(@NotNull CommandContext context) {
        super(context);
    }

    @Override
    public @NotNull String name() {
        return "menu";
    }

    @Override
    public void execute(@NotNull CommandSender sender, String @NotNull [] args) {

        if (context.denyIfNotAdmin(sender)) {
            return;
        }

        if (args.length < 2) {
            context.messages().send(sender, MessageKey.MENU_USAGE);
            return;
        }

        Player target = null;

        if (args.length >= 3) {

            target = Bukkit.getPlayerExact(args[2]);

            if (target == null) {
                context.messages().send(sender, MessageKey.PLAYER_NOT_FOUND,
                        Placeholders.create().set("player", args[2]));
                return;
            }

        } else if (sender instanceof Player player) {
            target = player;
        }

        if (target == null) {
            context.messages().send(sender, MessageKey.ONLY_PLAYERS);
            return;
        }

        context.services().getMenuService().open(target, args[1]);

    }

    @Override
    public @NotNull List<String> suggest(@NotNull CommandSender sender, String @NotNull [] args) {

        if (args.length == 2) {
            return context.filter(context.services().getMenuService().getMenuNames(), args[1]);
        }

        if (args.length == 3) {
            return context.filter(context.playerNames(), args[2]);
        }

        return List.of();

    }
}