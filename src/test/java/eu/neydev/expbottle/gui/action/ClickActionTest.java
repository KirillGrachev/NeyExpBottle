package eu.neydev.expbottle.gui.action;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Разбор действий меню из конфига.
 */
class ClickActionTest {

    private static final Logger LOGGER = Logger.getLogger("ClickActionTest");

    @Test
    @DisplayName("Действие с аргументом")
    void parsesActionWithArgument() {

        ClickAction action = ClickAction.parse("[message] &cНедостаточно опыта");

        assertEquals(ActionType.MESSAGE, action.type());
        assertEquals("&cНедостаточно опыта", action.argument());

    }

    @Test
    @DisplayName("Действие без аргумента")
    void parsesActionWithoutArgument() {

        ClickAction action = ClickAction.parse("[close]");

        assertEquals(ActionType.CLOSE, action.type());
        assertEquals("", action.argument());

    }

    @Test
    @DisplayName("Регистр тега и пробелы не важны")
    void tagIsCaseInsensitive() {

        assertEquals(ActionType.CONSOLE, ClickAction.parse("[ Console ]give Ney diamond 1").type());
        assertEquals(ActionType.REFRESH, ClickAction.parse("[REFRESH]").type());

    }

    @Test
    @DisplayName("Аргумент может содержать скобки и несколько пробелов")
    void argumentKeepsInnerBrackets() {

        ClickAction action = ClickAction.parse("[player] minecraft:give @p diamond[Unbreakable:1] 1");

        assertEquals(ActionType.PLAYER, action.type());
        assertEquals("minecraft:give @p diamond[Unbreakable:1] 1", action.argument());

    }

    @Test
    @DisplayName("Все типы действий распознаются")
    void allActionTypesAreRecognized() {

        for (ActionType type : ActionType.values()) {
            assertEquals(type, ClickAction.parse("[" + type.getTag() + "] аргумент").type());
        }

    }

    @Test
    @DisplayName("Некорректные строки отклоняются")
    void malformedActions() {

        assertThrows(IllegalArgumentException.class, () -> ClickAction.parse(""));
        assertThrows(IllegalArgumentException.class, () -> ClickAction.parse(null));
        assertThrows(IllegalArgumentException.class, () -> ClickAction.parse("message без скобок"));
        assertThrows(IllegalArgumentException.class, () -> ClickAction.parse("[unknown] текст"));

    }

    @Test
    @DisplayName("Список действий пропускает битые строки, а не падает")
    void parseListSkipsBrokenLines() {

        List<ClickAction> actions = ClickAction.parseList(
                List.of("[message] первый", "битая строка", "[close]", ""), LOGGER, "test");

        assertEquals(2, actions.size());
        assertEquals(ActionType.MESSAGE, actions.get(0).type());
        assertEquals(ActionType.CLOSE, actions.get(1).type());

    }

    @Test
    @DisplayName("Пустой список действий допустим")
    void parseListHandlesEmpty() {

        assertTrue(ClickAction.parseList(null, LOGGER, "test").isEmpty());
        assertTrue(ClickAction.parseList(List.of(), LOGGER, "test").isEmpty());

    }
}
