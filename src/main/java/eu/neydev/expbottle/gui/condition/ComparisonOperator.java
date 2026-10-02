package eu.neydev.expbottle.gui.condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Операторы сравнения в условиях меню.
 *
 * <p>Если оба значения разбираются как числа — сравнение числовое, иначе строковое
 * (без учёта регистра для равенства).</p>
 */
public enum ComparisonOperator {

    EQUALS("=="),
    NOT_EQUALS("!="),
    GREATER(">"),
    GREATER_OR_EQUAL(">="),
    LESS("<"),
    LESS_OR_EQUAL("<=");

    private final String symbol;

    ComparisonOperator(@NotNull String symbol) {
        this.symbol = symbol;
    }

    public @NotNull String getSymbol() {
        return symbol;
    }

    /**
     * Ищет оператор по символу. Двухсимвольные проверяются первыми,
     * чтобы {@code >=} не распалось на {@code >}.
     *
     * @param symbol текст из условия
     * @return оператор или {@code null}
     */
    public static @Nullable ComparisonOperator fromSymbol(@Nullable String symbol) {

        if (symbol == null) {
            return null;
        }

        for (ComparisonOperator operator : values()) {

            if (operator.symbol.equals(symbol)) {
                return operator;
            }

        }

        return null;

    }

    /**
     * Сравнивает два уже подставленных значения.
     *
     * @param left  левое значение
     * @param right правое значение
     * @return результат сравнения
     */
    public boolean matches(@NotNull String left, @NotNull String right) {

        Double leftNumber = asNumber(left);
        Double rightNumber = asNumber(right);

        if (leftNumber != null && rightNumber != null) {
            return matchesNumeric(Double.compare(leftNumber, rightNumber));
        }

        return matchesText(left.trim(), right.trim());

    }

    private boolean matchesNumeric(int comparison) {

        return switch (this) {
            case EQUALS -> comparison == 0;
            case NOT_EQUALS -> comparison != 0;
            case GREATER -> comparison > 0;
            case GREATER_OR_EQUAL -> comparison >= 0;
            case LESS -> comparison < 0;
            case LESS_OR_EQUAL -> comparison <= 0;
        };

    }

    private boolean matchesText(@NotNull String left, @NotNull String right) {

        int comparison = left.compareToIgnoreCase(right);

        return switch (this) {
            case EQUALS -> comparison == 0;
            case NOT_EQUALS -> comparison != 0;
            case GREATER -> comparison > 0;
            case GREATER_OR_EQUAL -> comparison >= 0;
            case LESS -> comparison < 0;
            case LESS_OR_EQUAL -> comparison <= 0;
        };

    }

    private @Nullable Double asNumber(@NotNull String value) {
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}