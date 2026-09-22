package eu.neydev.expbottle.support;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Раскрытие плейсхолдеров PlaceholderAPI в текстах меню.
 *
 * <p>Класс загружается только если PlaceholderAPI установлен —
 * см. {@link PlaceholderSupport}.</p>
 */
public class PlaceholderBridge implements TextProcessor {

    @Override
    public @NotNull String process(@NotNull Player player, @NotNull String text) {

        if (text.isEmpty() || text.indexOf('%') < 0) {
            return text;
        }

        return PlaceholderAPI.setPlaceholders(player, text);

    }
}
