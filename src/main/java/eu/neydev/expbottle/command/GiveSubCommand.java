package eu.neydev.expbottle.command;

import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.registry.BottleTier;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * {@code /neyexpbottle give <игрок> <уровни|tier> [количество]} — выдача бутылок.
 *
 * <p>Аргумент уровней принимает либо число, либо идентификатор ступени из
 * menu.yml; количество ограничено сверху, чтобы команда не стала лаг-машиной.</p>
 */
public class GiveSubCommand extends AdminSubCommand {

    private static final List<String> AMOUNTS = List.of("1", "16", "32", "64");
    private static final int MAX_GIVE_AMOUNT = 64;

    public GiveSubCommand(@NotNull CommandContext context) {
        super(context);
    }

    @Override
    public @NotNull String name() {
        return "give";
    }

    @Override
    public void execute(@NotNull CommandSender sender, String @NotNull [] args) {

        if (context.denyIfNotAdmin(sender)) {
            return;
        }

        if (args.length < 3) {
            context.messages().send(sender, MessageKey.GIVE_USAGE);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            context.messages().send(sender, MessageKey.PLAYER_NOT_FOUND,
                    Placeholders.create().set("player", args[1]));
            return;
        }

        int levels = resolveLevels(sender, args[2]);

        if (levels <= 0) {
            return;
        }

        int amount = resolveAmount(sender, args);

        if (amount <= 0) {
            return;
        }

        ItemStack bottle = context.services().getBottleFactory().create(levels);

        for (int i = 0; i < amount; i++) {
            context.services().getInventoryService().giveOrDrop(target, bottle.clone());
        }

        Placeholders placeholders = context.services().getPlaceholderService().forLevels(levels)
                .set("player", target.getName())
                .set("amount", amount);

        context.messages().send(sender, MessageKey.GIVE_SUCCESS, placeholders);
        context.messages().send(target, MessageKey.GIVE_RECEIVED, placeholders);

        context.services().getDiagnosticsService().info(sender.getName() + " gave " + amount + "x "
                + levels + " LVL to " + target.getName());

    }

    @Override
    public @NotNull List<String> suggest(@NotNull CommandSender sender, String @NotNull [] args) {

        if (args.length == 2) {
            return context.filter(context.playerNames(), args[1]);
        }

        if (args.length == 3) {
            return context.filter(context.services().getBottleRegistry().getIds(), args[2]);
        }

        if (args.length == 4) {
            return context.filter(AMOUNTS, args[3]);
        }

        return List.of();

    }

    /**
     * Аргумент может быть либо идентификатором уровня из menu.yml, либо числом.
     *
     * @return количество уровней или -1 при ошибке (сообщение уже отправлено)
     */
    private int resolveLevels(@NotNull CommandSender sender, @NotNull String raw) {

        Optional<BottleTier> tier = context.services().getBottleRegistry().byId(raw);

        if (tier.isPresent()) {
            return tier.get().levels();
        }

        int maxLevels = context.services().getConfigManager().getMaxBottleLevels();

        try {

            int levels = Integer.parseInt(raw.trim());

            if (levels < 1 || levels > maxLevels) {
                context.messages().send(sender, MessageKey.INVALID_AMOUNT, Placeholders.create()
                        .set("levels", raw)
                        .set("max_levels", maxLevels));
                return -1;
            }

            return levels;

        } catch (NumberFormatException exception) {
            context.messages().send(sender, MessageKey.INVALID_TIER,
                    Placeholders.create().set("tier", raw));
            return -1;
        }

    }

    /**
     * @return количество предметов или -1 при ошибке (сообщение уже отправлено)
     */
    private int resolveAmount(@NotNull CommandSender sender, String @NotNull [] args) {

        if (args.length < 4) {
            return 1;
        }

        try {

            int amount = Integer.parseInt(args[3].trim());

            if (amount < 1 || amount > MAX_GIVE_AMOUNT) {
                context.messages().send(sender, MessageKey.INVALID_AMOUNT, Placeholders.create()
                        .set("levels", amount)
                        .set("max_levels", MAX_GIVE_AMOUNT));
                return -1;
            }

            return amount;

        } catch (NumberFormatException exception) {

            context.messages().send(sender, MessageKey.INVALID_AMOUNT, Placeholders.create()
                    .set("levels", args[3])
                    .set("max_levels", MAX_GIVE_AMOUNT));
            return -1;

        }

    }
}