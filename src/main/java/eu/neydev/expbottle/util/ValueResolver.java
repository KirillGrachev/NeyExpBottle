package eu.neydev.expbottle.util;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Безопасный разбор значений конфигурации.
 *
 * <p>Любая ошибка администратора в конфиге превращается в предупреждение
 * и значение по умолчанию, а не в исключение при включении плагина.</p>
 */
public final class ValueResolver {

    private ValueResolver() {
    }

    /**
     * Разбирает название материала.
     *
     * @param raw      значение из конфига
     * @param fallback материал по умолчанию
     * @param logger   логгер для предупреждений
     * @return найденный материал или {@code fallback}
     */
    public static @NotNull Material material(@Nullable String raw, @NotNull Material fallback,
                                             @NotNull Logger logger) {

        if (isBlank(raw)) {
            return fallback;
        }

        Material material = Material.matchMaterial(raw.trim());

        if (material == null) {
            logger.warning("Unknown material '" + raw + "' - using " + fallback.name());
            return fallback;
        }

        return material;

    }

    /**
     * Разбирает перечисление без {@code IllegalArgumentException} наружу.
     *
     * @param raw      значение из конфига
     * @param type     тип перечисления
     * @param fallback значение по умолчанию
     * @param logger   логгер для предупреждений
     * @return элемент перечисления или {@code fallback}
     */
    public static <T extends Enum<T>> @NotNull T enumValue(@Nullable String raw, @NotNull Class<T> type,
                                                           @NotNull T fallback, @NotNull Logger logger) {

        if (isBlank(raw)) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            logger.warning("Invalid value '" + raw + "' for " + type.getSimpleName()
                    + " - using " + fallback.name());
            return fallback;
        }

    }

    /**
     * Приводит число к допустимому диапазону.
     *
     * @param value исходное значение
     * @param min   нижняя граница
     * @param max   верхняя граница
     * @return значение в границах
     */
    public static int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }

    /**
     * Приводит дробное число к допустимому диапазону.
     *
     * @param value исходное значение
     * @param min   нижняя граница
     * @param max   верхняя граница
     * @return значение в границах
     */
    public static float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(min, value));
    }

    public static boolean isBlank(@Nullable String text) {
        return text == null || text.trim().isEmpty();
    }
}