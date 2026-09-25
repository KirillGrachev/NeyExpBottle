package eu.neydev.expbottle.config.section;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Секция {@code settings.menu}: меню по умолчанию и тексты статуса витрины.
 *
 * @param defaultMenu            имя меню, открываемого без аргумента
 * @param availableText          текст статуса «обмен выполним»
 * @param unavailableText        текст статуса «не хватает уровней»
 * @param unavailableBottlesText текст статуса «не хватает пустых пузырьков»
 */
public record MenuSection(@NotNull String defaultMenu, @NotNull String availableText,
                          @NotNull String unavailableText, @NotNull String unavailableBottlesText) {

    private static final String DEFAULT_MENU = "exchange";
    private static final String DEFAULT_AVAILABLE_TEXT = " &a✔ &fAvailable";
    private static final String DEFAULT_UNAVAILABLE_TEXT = " &c✖ &fNot enough levels";
    private static final String DEFAULT_UNAVAILABLE_BOTTLES_TEXT = " &c✖ &fNot enough empty bottles";

    private static final String PATH_DEFAULT = "settings.menu.default";
    private static final String PATH_AVAILABLE = "settings.menu.available_text";
    private static final String PATH_UNAVAILABLE = "settings.menu.unavailable_text";
    private static final String PATH_UNAVAILABLE_BOTTLES = "settings.menu.unavailable_bottles_text";

    public static @NotNull MenuSection read(@NotNull FileConfiguration config) {

        return new MenuSection(
                config.getString(PATH_DEFAULT, DEFAULT_MENU),
                config.getString(PATH_AVAILABLE, DEFAULT_AVAILABLE_TEXT),
                config.getString(PATH_UNAVAILABLE, DEFAULT_UNAVAILABLE_TEXT),
                config.getString(PATH_UNAVAILABLE_BOTTLES, DEFAULT_UNAVAILABLE_BOTTLES_TEXT)
        );

    }
}
