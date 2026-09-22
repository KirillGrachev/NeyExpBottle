package eu.neydev.expbottle.gui.action;

import eu.neydev.expbottle.util.ValueResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Одно действие меню: {@code [тег] аргумент}.
 *
 * @param type     тип действия
 * @param argument аргумент (сырой шаблон, плейсхолдеры подставляются при выполнении)
 */
public record ClickAction(@NotNull ActionType type, @NotNull String argument) {

    /** [message] текст — тег в квадратных скобках, дальше аргумент до конца строки. */
    private static final Pattern PATTERN = Pattern.compile("^\\[\\s*([a-zA-Z_]+)\\s*]\\s*(.*)$", Pattern.DOTALL);

    /**
     * Разбирает строку действия.
     *
     * @param raw строка из конфига
     * @return действие
     * @throws IllegalArgumentException если формат или тег неверны
     */
    public static @NotNull ClickAction parse(@Nullable String raw) {

        if (ValueResolver.isBlank(raw)) {
            throw new IllegalArgumentException("empty action");
        }

        Matcher matcher = PATTERN.matcher(raw.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("expected format '[tag] argument'");
        }

        ActionType type = ActionType.fromTag(matcher.group(1));

        if (type == null) {
            throw new IllegalArgumentException("unknown tag '" + matcher.group(1) + "'");
        }

        return new ClickAction(type, matcher.group(2).trim());

    }

    /**
     * Разбирает список действий, пропуская некорректные строки с предупреждением в лог.
     *
     * @param raw     строки из конфига
     * @param logger  логгер плагина
     * @param context описание места в конфиге (для понятного предупреждения)
     * @return список пригодных действий
     */
    public static @NotNull List<ClickAction> parseList(@Nullable List<String> raw, @NotNull Logger logger,
                                                       @NotNull String context) {

        List<ClickAction> actions = new ArrayList<>();

        if (raw == null || raw.isEmpty()) {
            return actions;
        }

        for (String line : raw) {

            if (ValueResolver.isBlank(line)) {
                continue;
            }

            try {
                actions.add(parse(line));
            } catch (IllegalArgumentException exception) {
                logger.warning(context + ": " + exception.getMessage() + " — '" + line + "'");
            }

        }

        return actions;

    }
}
