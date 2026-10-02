package eu.neydev.expbottle.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка разбора цветов.
 *
 * <p>Тесты не требуют запущенного сервера: {@link ServerVersion} при отсутствии
 * ядра использует значение по умолчанию (1.16), то есть ветку с HEX-цветами.</p>
 */
class HexColorUtilTest {

    @Test
    @DisplayName("Empty values do not break the utility")
    void handlesNullAndEmpty() {

        assertEquals("", HexColorUtil.color((String) null));
        assertEquals("", HexColorUtil.color(""));
        assertEquals("", HexColorUtil.strip(null));
        assertTrue(HexColorUtil.color((List<String>) null).isEmpty());

    }

    @Test
    @DisplayName("Classic & codes translate to § codes")
    void translatesLegacyCodes() {
        assertEquals("§aGreen", HexColorUtil.color("&aGreen"));
    }

    @Test
    @DisplayName("HEX expands to the §x§r§r§g§g§b§b form (lowercase digits)")
    void expandsHex() {
        assertEquals("§x§f§f§0§0§0§0Red", HexColorUtil.color("#FF0000Red"));
    }

    @Test
    @DisplayName("The &#RRGGBB form leaves no extra ampersand")
    void expandsHexWithAmpersand() {

        String colored = HexColorUtil.color("&#00FF00Green");

        assertEquals("§x§0§0§f§f§0§0Green", colored);
        assertFalse(colored.contains("&&"), "A double ampersand means the parse went wrong");

    }

    @Test
    @DisplayName("The gradient colors every character")
    void appliesGradient() {
        String colored = HexColorUtil.color("<gradient:#FF0000:#0000FF>ab</gradient>");
        assertEquals("§x§f§f§0§0§0§0a§x§0§0§0§0§f§fb", colored);
    }

    @Test
    @DisplayName("A single gradient character does not divide by zero")
    void gradientWithSingleCharacter() {
        assertEquals("§x§f§f§0§0§0§0a", HexColorUtil.color("<gradient:#FF0000:#0000FF>a</gradient>"));
    }

    @Test
    @DisplayName("strip removes both codes and own tags")
    void stripsColors() {

        assertEquals("Hello", HexColorUtil.strip("&aHello"));
        assertEquals("Hello", HexColorUtil.strip("#FF0000Hello"));
        assertEquals("Hello", HexColorUtil.strip("<gradient:#FF0000:#0000FF>Hello</gradient>"));

    }

    @Test
    @DisplayName("A list of lines is colored line by line")
    void colorsLists() {

        List<String> colored = HexColorUtil.color(List.of("&aOne", "#FF0000Two"));

        assertEquals(2, colored.size());
        assertEquals("§aOne", colored.get(0));
        assertTrue(colored.get(1).startsWith("§x"));

    }
}