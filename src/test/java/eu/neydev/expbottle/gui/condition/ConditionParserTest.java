package eu.neydev.expbottle.gui.condition;

import eu.neydev.expbottle.util.Placeholders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Разбор и вычисление условий меню.
 */
class ConditionParserTest {

    private static Placeholders placeholders() {

        return Placeholders.create()
                .set("player", "Ney")
                .set("player_level", 42)
                .set("player_exp", 1500)
                .set("levels", 5)
                .set("empty", "");

    }

    private static boolean evaluate(String expression) {
        return Condition.parse(expression).evaluate(placeholders());
    }

    @Test
    @DisplayName("Пустое условие всегда истинно")
    void blankIsAlwaysTrue() {

        assertInstanceOf(Condition.AlwaysTrue.class, Condition.parse(null));
        assertInstanceOf(Condition.AlwaysTrue.class, Condition.parse(""));
        assertInstanceOf(Condition.AlwaysTrue.class, Condition.parse("   "));

        assertTrue(evaluate(""));

    }

    @Test
    @DisplayName("Числовые сравнения")
    void numericComparisons() {

        assertTrue(evaluate("{player_level} >= 42"));
        assertTrue(evaluate("{player_level} > 41"));
        assertFalse(evaluate("{player_level} > 42"));
        assertTrue(evaluate("{player_exp} == 1500"));
        assertTrue(evaluate("{player_level} != 7"));
        assertTrue(evaluate("{levels} <= 5"));
        assertFalse(evaluate("{levels} < 5"));

    }

    @Test
    @DisplayName("Строковые сравнения, в том числе в кавычках")
    void stringComparisons() {

        assertTrue(evaluate("{player} == Ney"));
        assertTrue(evaluate("{player} == \"Ney\""));
        assertTrue(evaluate("{player} == 'ney'"), "Равенство строк должно игнорировать регистр");
        assertTrue(evaluate("{player} != Steve"));
        assertFalse(evaluate("{player} == Steve"));

    }

    @Test
    @DisplayName("Логические операторы и скобки")
    void logicalOperators() {

        assertTrue(evaluate("{player_level} >= 10 && {player_exp} > 1000"));
        assertFalse(evaluate("{player_level} >= 10 && {player_exp} > 10000"));
        assertTrue(evaluate("{player_level} >= 1000 || {player_exp} > 1000"));
        assertFalse(evaluate("!({player_level} >= 10)"));
        assertTrue(evaluate("!({player} == Steve)"));
        assertTrue(evaluate("({player_level} > 100 || {levels} == 5) && {player} == Ney"));

    }

    @Test
    @DisplayName("Приоритет: && сильнее ||")
    void andHasHigherPrecedence() {

        // false && false || true даёт true: у && приоритет выше
        assertTrue(evaluate("{player_level} > 100 && {player_exp} > 99999 || {player} == Ney"));
        // true || (false && false) даёт true: вторая ветка ложна
        assertTrue(evaluate("{player} == Ney || {player_level} > 100 && {player_exp} > 99999"));

    }

    @Test
    @DisplayName("Значение без оператора трактуется как «не пусто и не false»")
    void truthiness() {

        assertTrue(evaluate("{player}"));
        assertFalse(evaluate("{empty}"));
        assertFalse(evaluate("false"));
        assertFalse(evaluate("0"));
        assertTrue(evaluate("true"));
        assertTrue(evaluate("!false"));

    }

    @Test
    @DisplayName("Плейсхолдеры подставляются до сравнения")
    void placeholdersAreSubstituted() {

        Placeholders values = Placeholders.create().set("levels", 250);
        Condition condition = Condition.parse("{levels} >= 100");

        assertTrue(condition.evaluate(values));
        assertFalse(condition.evaluate(Placeholders.create().set("levels", 5)));

    }

    @Test
    @DisplayName("Некорректные выражения отклоняются")
    void malformedExpressions() {

        assertThrows(IllegalArgumentException.class, () -> Condition.parse("{player_level} >="));
        assertThrows(IllegalArgumentException.class, () -> Condition.parse("({player_level} > 5"));
        assertThrows(IllegalArgumentException.class, () -> Condition.parse("{player} == Ney extra"));
        assertThrows(IllegalArgumentException.class, () -> Condition.parse("\"незакрытая кавычка"));

    }

    @Test
    @DisplayName("Дерево условия имеет ожидаемую структуру")
    void parsesIntoExpectedTree() {

        Condition condition = Condition.parse("{a} > 1 && !{b}");

        assertInstanceOf(Condition.And.class, condition);

        Condition.And and = (Condition.And) condition;
        assertInstanceOf(Condition.Comparison.class, and.left());
        assertInstanceOf(Condition.Not.class, and.right());

        Condition.Comparison comparison = (Condition.Comparison) and.left();
        assertEquals(ComparisonOperator.GREATER, comparison.operator());
        assertEquals("{a}", comparison.left());
        assertEquals("1", comparison.right());

    }

    @Test
    @DisplayName("Дробные числа сравниваются корректно")
    void decimalComparisons() {

        assertTrue(Condition.parse("{levels} >= 4.5")
                .evaluate(Placeholders.create().set("levels", 4.75)));
        assertFalse(Condition.parse("{levels} > 4.5")
                .evaluate(Placeholders.create().set("levels", 4.25)));

    }
}
