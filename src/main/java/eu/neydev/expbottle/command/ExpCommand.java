package eu.neydev.expbottle.command;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.model.ExchangeResult;
import eu.neydev.expbottle.registry.BottleTier;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Команда {@code /exp}: меню и обмен без GUI.
 *
 * <ul>
 *     <li>{@code /exp} — меню по умолчанию;</li>
 *     <li>{@code /exp open <меню>} — конкретное меню;</li>
 *     <li>{@code /exp exchange <уровни|id>} — обмен из чата, как кнопка меню.</li>
 * </ul>
 *
 * <p>Слова {@code exchange} и {@code open} зарезервированы, остальные аргументы
 * трактуются как имена меню.</p>
 */
public class ExpCommand extends BaseCommand {

    private static final String SUB_EXCHANGE = "exchange";
    private static final String SUB_OPEN = "open";

    public ExpCommand(@NotNull NeyExpBottle plugin) {
        super(plugin, "exp");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {

        if (!(sender instanceof Player player)) {
            messages().send(sender, MessageKey.ONLY_PLAYERS);
            return true;
        }

        if (!permissions().canUse(player)) {
            messages().send(player, MessageKey.NO_PERMISSION);
            return true;
        }

        String subcommand = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

        switch (subcommand) {

            case SUB_EXCHANGE -> handleExchange(player, args);

            case SUB_OPEN -> services().getMenuService()
                    .open(player, args.length >= 2 ? args[1] : null);

            default -> {

                if (subcommand.isEmpty()) {
                    services().getMenuService().open(player);
                } else {
                    services().getMenuService().open(player, subcommand);
                }

            }

        }

        return true;

    }

    /**
     * Обмен уровней на бутылку из чата: те же проверки и те же сообщения,
     * что у кнопки меню.
     */
    private void handleExchange(@NotNull Player player, String @NotNull [] args) {

        if (!permissions().canExchange(player)) {
            messages().send(player, MessageKey.NO_PERMISSION);
            return;
        }

        if (args.length < 2) {
            messages().send(player, MessageKey.EXCHANGE_USAGE);
            return;
        }

        int levels = resolveLevels(player, args[1]);

        if (levels <= 0) {
            return;
        }

        ExchangeResult result = services().getExchangeService().exchange(player, levels);
        services().getActionExecutor().handleResult(player, result);

    }

    private int resolveLevels(@NotNull Player player, @NotNull String raw) {

        Optional<BottleTier> tier = services().getBottleRegistry().byId(raw);

        if (tier.isPresent()) {
            return tier.get().levels();
        }

        int maxLevels = services().getConfigManager().getMaxBottleLevels();

        try {

            int levels = Integer.parseInt(raw.trim());

            if (levels < 1 || levels > maxLevels) {

                messages().send(player, MessageKey.INVALID_AMOUNT, Placeholders.create()
                        .set("levels", raw)
                        .set("max_levels", maxLevels));
                return -1;

            }

            return levels;

        } catch (NumberFormatException exception) {

            messages().send(player, MessageKey.INVALID_TIER,
                    Placeholders.create().set("tier", raw));
            return -1;

        }

    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, String @NotNull [] args) {

        if (!permissions().canUse(sender)) {
            return List.of();
        }

        if (args.length == 1) {

            List<String> suggestions = new ArrayList<>(List.of(SUB_EXCHANGE, SUB_OPEN));
            suggestions.addAll(services().getMenuService().getMenuNames());
            return filter(suggestions, args[0]);

        }

        if (args.length == 2 && args[0].equalsIgnoreCase(SUB_EXCHANGE)) {
            return filter(services().getBottleRegistry().getIds(), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase(SUB_OPEN)) {
            return filter(services().getMenuService().getMenuNames(), args[1]);
        }

        return List.of();

    }
}
