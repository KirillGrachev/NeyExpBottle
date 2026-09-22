package eu.neydev.expbottle.command;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.registry.BottleTier;
import eu.neydev.expbottle.util.Placeholders;
import eu.neydev.expbottle.util.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Команда {@code /neyexpbottle} — перезагрузка, выдача бутылок и диагностика.
 *
 * <p>Весь текст (включая справку) берётся из config.yml: в старой версии
 * помощь была зашита в Java-код и не переводилась.</p>
 */
public class AdminCommand extends BaseCommand {

    private static final List<String> SUBCOMMANDS = List.of("reload", "give", "info", "menu", "help");
    private static final List<String> AMOUNTS = List.of("1", "16", "32", "64");

    private static final int MAX_GIVE_AMOUNT = 64;

    public AdminCommand(@NotNull NeyExpBottle plugin) {
        super(plugin, "neyexpbottle");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {

        String subcommand = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);

        switch (subcommand) {

            case "reload" -> handleReload(sender);
            case "give" -> handleGive(sender, args);
            case "info" -> handleInfo(sender, args);
            case "menu" -> handleMenu(sender, args);
            default -> handleHelp(sender);

        }

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

        if (args.length == 2 && isSubcommand(args[0], "menu")) {
            return filter(services().getMenuService().getMenuNames(), args[1]);
        }

        if (args.length == 2 && isSubcommand(args[0], "give", "info")) {
            return filter(playerNames(), args[1]);
        }

        if (args.length == 3 && isSubcommand(args[0], "give")) {
            return filter(services().getBottleRegistry().getIds(), args[2]);
        }

        if (args.length == 3 && isSubcommand(args[0], "menu")) {
            return filter(playerNames(), args[2]);
        }

        if (args.length == 4 && isSubcommand(args[0], "give")) {
            return filter(AMOUNTS, args[3]);
        }

        return Collections.emptyList();

    }

    private void handleReload(@NotNull CommandSender sender) {

        if (!permissions().canAdmin(sender)) {
            messages().send(sender, MessageKey.NO_PERMISSION);
            return;
        }

        messages().send(sender, MessageKey.RELOAD_START);

        try {

            long time = services().reload();

            Placeholders placeholders = Placeholders.create()
                    .set("time", time)
                    .set("tiers", services().getBottleRegistry().size());

            messages().send(sender, MessageKey.RELOAD_SUCCESS, placeholders);
            messages().send(sender, MessageKey.RELOAD_TIME, placeholders);

            services().getDiagnosticsService().info("Configuration reloaded (" + sender.getName()
                    + "), time: " + time + " ms, exchange tiers: " + services().getBottleRegistry().size());

        } catch (Exception exception) {

            messages().send(sender, MessageKey.RELOAD_ERROR);
            services().getDiagnosticsService().severe("Error while reloading configuration:", exception);

        }

    }

    private void handleGive(@NotNull CommandSender sender, String @NotNull [] args) {

        if (!permissions().canAdmin(sender)) {
            messages().send(sender, MessageKey.NO_PERMISSION);
            return;
        }

        if (args.length < 3) {
            messages().send(sender, MessageKey.GIVE_USAGE);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            messages().send(sender, MessageKey.PLAYER_NOT_FOUND,
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

        ItemStack bottle = services().getBottleFactory().create(levels);

        for (int i = 0; i < amount; i++) {
            services().getInventoryService().giveOrDrop(target, bottle.clone());
        }

        Placeholders placeholders = services().getPlaceholderService().forLevels(levels)
                .set("player", target.getName())
                .set("amount", amount);

        messages().send(sender, MessageKey.GIVE_SUCCESS, placeholders);
        messages().send(target, MessageKey.GIVE_RECEIVED, placeholders);

        services().getDiagnosticsService().info(sender.getName() + " gave " + amount + "x "
                + levels + " LVL to " + target.getName());

    }

    private void handleInfo(@NotNull CommandSender sender, String @NotNull [] args) {

        if (!permissions().canAdmin(sender)) {
            messages().send(sender, MessageKey.NO_PERMISSION);
            return;
        }

        Player target = null;

        if (args.length >= 2) {

            target = Bukkit.getPlayerExact(args[1]);

            if (target == null) {
                messages().send(sender, MessageKey.PLAYER_NOT_FOUND,
                        Placeholders.create().set("player", args[1]));
                return;
            }

        } else if (sender instanceof Player player) {
            target = player;
        }

        Placeholders placeholders = services().getPlaceholderService().forStatistics()
                .set("version", version())
                .set("author", author())
                .set("server", ServerVersion.getRaw())
                .set("tiers", services().getBottleRegistry().size());

        if (target == null) {

            placeholders.set("player", "—")
                    .set("player_level", "—")
                    .set("player_exp", "—")
                    .set("player_progress", "—")
                    .set("player_levels", "—");

        } else {
            placeholders.merge(services().getPlaceholderService().forPlayer(target));
        }

        messages().send(sender, MessageKey.INFO, placeholders);

    }

    /**
     * Открывает указанное меню игроку — удобно проверять разметку без перезахода.
     */
    private void handleMenu(@NotNull CommandSender sender, String @NotNull [] args) {

        if (!permissions().canAdmin(sender)) {
            messages().send(sender, MessageKey.NO_PERMISSION);
            return;
        }

        if (args.length < 2) {
            messages().send(sender, MessageKey.MENU_USAGE);
            return;
        }

        Player target = null;

        if (args.length >= 3) {

            target = Bukkit.getPlayerExact(args[2]);

            if (target == null) {
                messages().send(sender, MessageKey.PLAYER_NOT_FOUND,
                        Placeholders.create().set("player", args[2]));
                return;
            }

        } else if (sender instanceof Player player) {
            target = player;
        }

        if (target == null) {
            messages().send(sender, MessageKey.ONLY_PLAYERS);
            return;
        }

        services().getMenuService().open(target, args[1]);

    }

    private void handleHelp(@NotNull CommandSender sender) {

        messages().send(sender, MessageKey.HELP, Placeholders.create()
                .set("version", version())
                .set("author", author())
                .set("tiers", services().getBottleRegistry().size()));

    }

    /**
     * Аргумент может быть либо идентификатором уровня из menu.yml, либо числом.
     *
     * @return количество уровней или -1 при ошибке (сообщение уже отправлено)
     */
    private int resolveLevels(@NotNull CommandSender sender, @NotNull String raw) {

        Optional<BottleTier> tier = services().getBottleRegistry().byId(raw);

        if (tier.isPresent()) {
            return tier.get().levels();
        }

        int maxLevels = services().getConfigManager().getMaxBottleLevels();

        try {

            int levels = Integer.parseInt(raw.trim());

            if (levels < 1 || levels > maxLevels) {
                messages().send(sender, MessageKey.INVALID_AMOUNT, Placeholders.create()
                        .set("levels", raw)
                        .set("max_levels", maxLevels));
                return -1;
            }

            return levels;

        } catch (NumberFormatException exception) {

            messages().send(sender, MessageKey.INVALID_TIER,
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
                messages().send(sender, MessageKey.INVALID_AMOUNT, Placeholders.create()
                        .set("levels", amount)
                        .set("max_levels", MAX_GIVE_AMOUNT));
                return -1;
            }

            return amount;

        } catch (NumberFormatException exception) {

            messages().send(sender, MessageKey.INVALID_AMOUNT, Placeholders.create()
                    .set("levels", args[3])
                    .set("max_levels", MAX_GIVE_AMOUNT));
            return -1;

        }

    }
}
