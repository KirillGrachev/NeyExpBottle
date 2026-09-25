package eu.neydev.expbottle.gui.condition;

import eu.neydev.expbottle.util.Placeholders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Условие меню — дерево выражения, которое вычисляется уже с подставленными плейсхолдерами.
 *
 * <p>Поддерживается: сравнение ({@code ==}, {@code !=}, {@code >}, {@code >=},
 * {@code <}, {@code <=}), отрицание {@code !}, {@code &&}, {@code ||} и скобки.
 * Строки можно брать в кавычки: {@code {player} == "Ney"}.</p>
 *
 * <p>Примеры:</p>
 * <pre>
 * {player_level} &gt;= 10
 * {player_exp} &gt; 1000 &amp;&amp; {levels} &lt;= 50
 * !({player} == "Ney")
 * </pre>
 */
public interface Condition {

    /**
     * Вычисляет условие.
     *
     * @param placeholders значения плейсхолдеров для подстановки в шаблоны
     * @return результат
     */
    boolean evaluate(@NotNull Placeholders placeholders);

    /**
     * Условие, которое всегда истинно — используется, когда в конфиге пусто.
     *
     * @return константа
     */
    static @NotNull Condition alwaysTrue() {
        return AlwaysTrue.INSTANCE;
    }

    /**
     * Разбирает строку из конфига.
     *
     * @param raw текст условия
     * @return условие
     * @throws IllegalArgumentException если выражение некорректно
     */
    static @NotNull Condition parse(@Nullable String raw) {
        return ConditionParser.parse(raw);
    }

    /**
     * Пустое условие: всегда «да».
     */
    final class AlwaysTrue implements Condition {

        private static final AlwaysTrue INSTANCE = new AlwaysTrue();

        private AlwaysTrue() {
        }

        @Override
        public boolean evaluate(@NotNull Placeholders placeholders) {
            return true;
        }

        @Override
        public @NotNull String toString() {
            return "true";
        }
    }

    /**
     * Логическое «И».
     */
    record And(@NotNull Condition left, @NotNull Condition right) implements Condition {

        @Override
        public boolean evaluate(@NotNull Placeholders placeholders) {
            return left.evaluate(placeholders) && right.evaluate(placeholders);
        }

    }

    /**
     * Логическое «ИЛИ».
     */
    record Or(@NotNull Condition left, @NotNull Condition right) implements Condition {

        @Override
        public boolean evaluate(@NotNull Placeholders placeholders) {
            return left.evaluate(placeholders) || right.evaluate(placeholders);
        }

    }

    /**
     * Отрицание.
     */
    record Not(@NotNull Condition inner) implements Condition {

        @Override
        public boolean evaluate(@NotNull Placeholders placeholders) {
            return !inner.evaluate(placeholders);
        }

    }

    /**
     * Сравнение двух шаблонов.
     */
    record Comparison(@NotNull String left, @NotNull ComparisonOperator operator,
                      @NotNull String right) implements Condition {

        @Override
        public boolean evaluate(@NotNull Placeholders placeholders) {
            return operator.matches(placeholders.apply(left), placeholders.apply(right));
        }

    }

    /**
     * Значение без оператора: истина, если текст не пустой и не «false»/«0».
     */
    record Truthy(@NotNull String template) implements Condition {

        @Override
        public boolean evaluate(@NotNull Placeholders placeholders) {

            String value = placeholders.apply(template).trim();
            return !value.isEmpty() && !value.equalsIgnoreCase("false") && !value.equals("0");

        }

    }
}