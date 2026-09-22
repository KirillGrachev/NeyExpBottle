package eu.neydev.expbottle.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Набор плейсхолдеров вида {@code {ключ}} и их подстановка в текст.
 *
 * <p>Единый формат для сообщений, названий и лора: в старой версии плагина
 * часть строк использовала {@code {exp}}, а часть — {@code %lvl}.</p>
 */
public final class Placeholders {

    private final Map<String, String> values = new LinkedHashMap<>();

    public static @NotNull Placeholders create() {
        return new Placeholders();
    }

    /**
     * Добавляет значение плейсхолдера.
     *
     * @param key   имя без фигурных скобок
     * @param value значение, {@code null} превращается в пустую строку
     * @return этот же объект (fluent-стиль)
     */
    public @NotNull Placeholders set(@NotNull String key, @Nullable Object value) {
        values.put(key, value == null ? "" : String.valueOf(value));
        return this;
    }

    /**
     * Переносит значения другого набора в текущий.
     *
     * @param other источник значений
     * @return этот же объект (fluent-стиль)
     */
    public @NotNull Placeholders merge(@NotNull Placeholders other) {
        values.putAll(other.values);
        return this;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Подставляет значения в строку.
     *
     * @param text исходный текст
     * @return текст с подставленными значениями
     */
    public @NotNull String apply(@Nullable String text) {

        if (text == null || text.isEmpty()) {
            return "";
        }

        if (values.isEmpty()) {
            return text;
        }

        String result = text;

        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return result;

    }

    /**
     * Подставляет значения в список строк.
     *
     * @param lines исходные строки
     * @return новый список
     */
    public @NotNull List<String> apply(@Nullable List<String> lines) {

        List<String> result = new ArrayList<>();

        if (lines == null || lines.isEmpty()) {
            return result;
        }

        for (String line : lines) {
            result.add(apply(line));
        }

        return result;

    }
}
