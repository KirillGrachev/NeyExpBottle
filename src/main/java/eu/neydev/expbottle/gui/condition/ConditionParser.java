package eu.neydev.expbottle.gui.condition;

import eu.neydev.expbottle.util.ValueResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Рекурсивный разборщик условий меню.
 *
 * <p>Грамматика (приоритет от низкого к высокому):</p>
 * <pre>
 * or         := and ( '||' and )*
 * and        := unary ( '&amp;&amp;' unary )*
 * unary      := '!' unary | primary
 * primary    := '(' or ')' | comparison
 * comparison := value [ operator value ]
 * value      := "строка" | 'строка' | токен
 * </pre>
 */
public final class ConditionParser {

    private static final String[] OPERATORS = {"==", "!=", ">=", "<=", ">", "<"};

    private final String source;
    private int position;

    private ConditionParser(@NotNull String source) {
        this.source = source;
    }

    /**
     * Разбирает условие из конфига.
     *
     * @param raw текст условия (пустой — всегда «да»)
     * @return дерево условия
     * @throws IllegalArgumentException если выражение некорректно
     */
    public static @NotNull Condition parse(@Nullable String raw) {

        if (ValueResolver.isBlank(raw)) {
            return Condition.alwaysTrue();
        }

        ConditionParser parser = new ConditionParser(raw.trim());
        Condition condition = parser.parseOr();

        parser.skipWhitespace();

        if (!parser.atEnd()) {
            throw new IllegalArgumentException("unparsed remainder '" + parser.tail() + "'");
        }

        return condition;

    }

    private @NotNull Condition parseOr() {

        Condition left = parseAnd();

        while (match("||")) {
            left = new Condition.Or(left, parseAnd());
        }

        return left;

    }

    private @NotNull Condition parseAnd() {

        Condition left = parseUnary();

        while (match("&&")) {
            left = new Condition.And(left, parseUnary());
        }

        return left;

    }

    private @NotNull Condition parseUnary() {

        skipWhitespace();

        if (match("!")) {
            return new Condition.Not(parseUnary());
        }

        return parsePrimary();

    }

    private @NotNull Condition parsePrimary() {

        skipWhitespace();

        if (match("(")) {

            Condition inner = parseOr();
            expect(")");
            return inner;

        }

        return parseComparison();

    }

    private @NotNull Condition parseComparison() {

        String left = parseValue();

        skipWhitespace();

        ComparisonOperator operator = ComparisonOperator.fromSymbol(peekOperator());

        if (operator == null) {
            return new Condition.Truthy(left);
        }

        consume(operator.getSymbol());
        return new Condition.Comparison(left, operator, parseValue());

    }

    /**
     * Читает значение: строку в кавычках или токен до пробела/оператора.
     */
    private @NotNull String parseValue() {

        skipWhitespace();

        if (atEnd()) {
            throw new IllegalArgumentException("expected a value at the end of the expression");
        }

        char first = source.charAt(position);

        if (first == '"' || first == '\'') {
            return parseQuoted(first);
        }

        int start = position;

        while (!atEnd()) {

            char symbol = source.charAt(position);

            if (Character.isWhitespace(symbol) || isOperatorChar(symbol)) {
                break;
            }

            position++;

        }

        if (start == position) {
            throw new IllegalArgumentException("empty value at position " + position);
        }

        return source.substring(start, position);

    }

    private @NotNull String parseQuoted(char quote) {

        position++;
        int start = position;

        while (!atEnd() && source.charAt(position) != quote) {
            position++;
        }

        if (atEnd()) {
            throw new IllegalArgumentException("unclosed quote " + quote);
        }

        String value = source.substring(start, position);
        position++;

        return value;

    }

    private @Nullable String peekOperator() {

        for (String operator : OPERATORS) {

            if (source.startsWith(operator, position)) {
                return operator;
            }

        }

        return null;

    }

    private boolean isOperatorChar(char symbol) {
        return symbol == '=' || symbol == '!' || symbol == '<' || symbol == '>'
                || symbol == '&' || symbol == '|' || symbol == '(' || symbol == ')';
    }

    private boolean match(@NotNull String token) {

        skipWhitespace();

        if (!source.startsWith(token, position)) {
            return false;
        }

        position += token.length();
        return true;

    }

    private void consume(@NotNull String token) {

        if (!match(token)) {
            throw new IllegalArgumentException("expected '" + token + "' at position " + position);
        }

    }

    private void expect(@NotNull String token) {
        consume(token);
    }

    private void skipWhitespace() {

        while (!atEnd() && Character.isWhitespace(source.charAt(position))) {
            position++;
        }

    }

    private boolean atEnd() {
        return position >= source.length();
    }

    private @NotNull String tail() {
        return source.substring(position);
    }
}