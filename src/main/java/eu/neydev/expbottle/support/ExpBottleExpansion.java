package eu.neydev.expbottle.support;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.service.PluginServices;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Плейсхолдеры PlaceholderAPI с идентификатором {@code %expbottle_*%}.
 *
 * <p>Класс загружается только если PlaceholderAPI установлен —
 * см. {@link PlaceholderSupport}.</p>
 */
public class ExpBottleExpansion extends PlaceholderExpansion {

    private static final String IDENTIFIER = "expbottle";
    private static final String DEFAULT_AUTHOR = "Ney";

    private final NeyExpBottle plugin;
    private final PluginServices services;

    public ExpBottleExpansion(@NotNull NeyExpBottle plugin) {
        this.plugin = plugin;
        this.services = plugin.getServices();
    }

    @Override
    public @NotNull String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public @NotNull String getAuthor() {

        List<String> authors = plugin.getDescription().getAuthors();
        return authors.isEmpty() ? DEFAULT_AUTHOR : String.join(", ", authors);

    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {

        // Если вызывающая сторона уже передала Player, не делаем лишний lookup:
        // на некоторых ядрах и в тестах OfflinePlayer#getPlayer может вернуть null
        Player online = player instanceof Player onlinePlayer
                ? onlinePlayer
                : (player == null ? null : player.getPlayer());

        return switch (params.toLowerCase(Locale.ROOT)) {

            case "level" -> online == null ? "0" : String.valueOf(online.getLevel());
            case "exp" -> online == null ? "0"
                    : String.valueOf(services.getExperienceService().getTotalExperience(online));
            case "progress" -> online == null ? "0"
                    : String.valueOf(services.getExperienceService().getProgressPercent(online));
            case "levels" -> online == null ? "0"
                    : String.valueOf(services.getExperienceService().getExactLevels(online));

            case "created" -> String.valueOf(services.getDiagnosticsService().getBottlesCreated());
            case "used" -> String.valueOf(services.getDiagnosticsService().getBottlesUsed());
            case "denied" -> String.valueOf(services.getDiagnosticsService().getExchangesDenied());
            case "tiers" -> String.valueOf(services.getBottleRegistry().size());
            case "version" -> plugin.getDescription().getVersion();

            default -> null;

        };

    }
}