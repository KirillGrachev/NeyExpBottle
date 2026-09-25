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
    @DisplayName("An action with an argument")
    void parsesActionWithArgument() {

        ClickAction action = ClickAction.parse("[message] &cNot enough experience");

        assertEquals(ActionType.MESSAGE, action.type());
        assertEquals("&cNot enough experience", action.argument());

    }

    @Test
    @DisplayName("An action without an argument")
    void parsesActionWithoutArgument() {

        ClickAction action = ClickAction.parse("[close]");

        assertEquals(ActionType.CLOSE, action.type());
        assertEquals("", action.argument());

    }

    @Test
    @DisplayName("The tag case and spaces do not matter")
    void tagIsCaseInsensitive() {

        assertEquals(ActionType.CONSOLE, ClickAction.parse("[ Console ]give Ney diamond 1").type());
        assertEquals(ActionType.REFRESH, ClickAction.parse("[REFRESH]").type());

    }

    @Test
    @DisplayName("The argument may contain brackets and multiple spaces")
    void argumentKeepsInnerBrackets() {

        ClickAction action = ClickAction.parse("[player] minecraft:give @p diamond[Unbreakable:1] 1");

        assertEquals(ActionType.PLAYER, action.type());
        assertEquals("minecraft:give @p diamond[Unbreakable:1] 1", action.argument());

    }

    @Test
    @DisplayName("All action types are recognized")
    void allActionTypesAreRecognized() {

        for (ActionType type : ActionType.values()) {
            assertEquals(type, ClickAction.parse("[" + type.getTag() + "] argument").type());
        }

    }

    @Test
    @DisplayName("Invalid lines are rejected")
    void malformedActions() {

        assertThrows(IllegalArgumentException.class, () -> ClickAction.parse(""));
        assertThrows(IllegalArgumentException.class, () -> ClickAction.parse(null));
        assertThrows(IllegalArgumentException.class, () -> ClickAction.parse("message without brackets"));
        assertThrows(IllegalArgumentException.class, () -> ClickAction.parse("[unknown] text"));

    }

    @Test
    @DisplayName("The action list skips broken lines instead of crashing")
    void parseListSkipsBrokenLines() {

        List<ClickAction> actions = ClickAction.parseList(
                List.of("[message] first", "broken line", "[close]", ""), LOGGER, "test");

        assertEquals(2, actions.size());
        assertEquals(ActionType.MESSAGE, actions.get(0).type());
        assertEquals(ActionType.CLOSE, actions.get(1).type());

    }

    @Test
    @DisplayName("An empty action list is allowed")
    void parseListHandlesEmpty() {

        assertTrue(ClickAction.parseList(null, LOGGER, "test").isEmpty());
        assertTrue(ClickAction.parseList(List.of(), LOGGER, "test").isEmpty());

    }
}