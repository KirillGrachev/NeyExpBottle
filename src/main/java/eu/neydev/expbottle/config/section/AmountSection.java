package eu.neydev.expbottle.config.section;

import eu.neydev.expbottle.config.type.AmountSettings;
import eu.neydev.expbottle.config.type.SoundSettings;
import eu.neydev.expbottle.service.ExchangeService;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Секция {@code settings.amount}: варианты количества и вид переключателя.
 */
public final class AmountSection {

    private static final String TOKEN_ALL = "all";
    private static final List<String> DEFAULT_AMOUNT_OPTIONS = List.of("1", "16", "64", "all");

    private AmountSection() {
    }

    private static final String PATH_AMOUNTS = "settings.amount.amounts";
    private static final String PATH_ALL_LABEL = "settings.amount.all_label";
    private static final String PATH_SWITCHER_ACTIVE = "settings.amount.switcher.active";
    private static final String PATH_SWITCHER_INACTIVE = "settings.amount.switcher.inactive";
    private static final String PATH_HINT = "settings.amount.hint";
    private static final String PATH_CYCLE_SOUND = "settings.amount.cycle_sound";
    private static final String PATH_CYCLE_ON_RIGHT_CLICK = "settings.amount.cycle_on_right_click";

    public static @NotNull AmountSettings read(@NotNull FileConfiguration config, @NotNull Logger logger) {

        return new AmountSettings(
                readOptions(config, logger),
                config.getString(PATH_ALL_LABEL, "ALL"),
                config.getString(PATH_SWITCHER_ACTIVE, " &a» &f{label} &7bottle(s) &8(&e{cost} &7lvl&8)"),
                config.getString(PATH_SWITCHER_INACTIVE, " &7» {label} &7bottle(s) &8(&e{cost} &7lvl&8)"),
                config.getString(PATH_HINT, "&eRMB: &7switch the amount &8(&f{amount_label}&8)"),
                Sounds.read(config, PATH_CYCLE_SOUND, SoundSettings.of("UI_BUTTON_CLICK", 0.5f, 1.4f)),
                config.getBoolean(PATH_CYCLE_ON_RIGHT_CLICK, true)
        );

    }

    /**
     * Варианты количества: оставляем только {@code all} и числа 1..MAX_AMOUNT,
     * остальные отбрасываем с предупреждением, чтобы переключатель не показывал
     * кнопки, которые обмен не примет.
     */
    private static @NotNull List<String> readOptions(@NotNull FileConfiguration config, @NotNull Logger logger) {

        List<String> raw = config.getStringList(PATH_AMOUNTS);
        List<String> amounts = new ArrayList<>();

        for (String token : raw) {

            String normalized = token.trim().toLowerCase(Locale.ROOT);

            if (normalized.equals(TOKEN_ALL)) {

                amounts.add(TOKEN_ALL);
                continue;

            }

            try {

                int value = Integer.parseInt(normalized);

                if (value < 1 || value > ExchangeService.MAX_AMOUNT) {
                    logger.warning("settings.amount.amounts: " + token + " is out of 1-"
                            + ExchangeService.MAX_AMOUNT + " - option skipped");
                    continue;
                }

                amounts.add(String.valueOf(value));

            } catch (NumberFormatException exception) {
                logger.warning("settings.amount.amounts: '" + token + "' is not a number or 'all' - option skipped");
            }

        }

        return amounts.isEmpty() ? new ArrayList<>(DEFAULT_AMOUNT_OPTIONS) : amounts;

    }
}
