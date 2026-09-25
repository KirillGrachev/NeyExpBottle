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

    /**
     * Максимум проходов подстановки: значение одного плейсхолдера может само
     * содержать другие (например, {@code {amount_hint}} раскрывается в шаблон
     * с {@code {amount_label}}), поэтому одного прохода мало.
     */
    private static final int MAX_PASSES = 4;

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
     * Сырое значение плейсхолдера.
     *
     * @param key имя без фигурных скобок
     * @return значение или {@code null}, если ключ не задан
     */
    public @Nullable String get(@NotNull String key) {
        return values.get(key);
    }

    /**
     * Подставляет значения в строку.
     *
     * <p>Проходы повторяются, пока текст меняется: так раскрываются вложенные
     * плейсхолдеры, пришедшие из значений конфига. Ограничение проходов
     * защищает от бесконечного цикла, если значение ссылается само на себя.</p>
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

        for (int pass = 0; pass < MAX_PASSES; pass++) {

            String next = result;

            for (Map.Entry<String, String> entry : values.entrySet()) {
                next = next.replace("{" + entry.getKey() + "}", entry.getValue());
            }

            if (next.equals(result)) {
                break;
            }

            result = next;

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