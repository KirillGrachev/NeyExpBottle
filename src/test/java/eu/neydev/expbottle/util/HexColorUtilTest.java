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
    @DisplayName("Пустые значения не ломают утилиту")
    void handlesNullAndEmpty() {

        assertEquals("", HexColorUtil.color((String) null));
        assertEquals("", HexColorUtil.color(""));
        assertEquals("", HexColorUtil.strip(null));
        assertTrue(HexColorUtil.color((List<String>) null).isEmpty());

    }

    @Test
    @DisplayName("Классические &-коды переводятся в §-коды")
    void translatesLegacyCodes() {
        assertEquals("§aЗелёный", HexColorUtil.color("&aЗелёный"));
    }

    @Test
    @DisplayName("HEX раскрывается в формат §x§r§r§g§g§b§b (цифры в нижнем регистре)")
    void expandsHex() {
        assertEquals("§x§f§f§0§0§0§0Красный", HexColorUtil.color("#FF0000Красный"));
    }

    @Test
    @DisplayName("Форма &#RRGGBB не оставляет лишнего амперсанда")
    void expandsHexWithAmpersand() {

        String colored = HexColorUtil.color("&#00FF00Зелёный");

        assertEquals("§x§0§0§f§f§0§0Зелёный", colored);
        assertFalse(colored.contains("&&"), "Двойной амперсанд говорит о неверном разборе");

    }

    @Test
    @DisplayName("Градиент красит каждый символ")
    void appliesGradient() {

        String colored = HexColorUtil.color("<gradient:#FF0000:#0000FF>ab</gradient>");

        assertEquals("§x§f§f§0§0§0§0a§x§0§0§0§0§f§fb", colored);

    }

    @Test
    @DisplayName("Одиночный символ градиента не делит на ноль")
    void gradientWithSingleCharacter() {
        assertEquals("§x§f§f§0§0§0§0a", HexColorUtil.color("<gradient:#FF0000:#0000FF>a</gradient>"));
    }

    @Test
    @DisplayName("strip убирает и коды, и собственные теги")
    void stripsColors() {

        assertEquals("Привет", HexColorUtil.strip("&aПривет"));
        assertEquals("Привет", HexColorUtil.strip("#FF0000Привет"));
        assertEquals("Привет", HexColorUtil.strip("<gradient:#FF0000:#0000FF>Привет</gradient>"));

    }

    @Test
    @DisplayName("Список строк красится построчно")
    void colorsLists() {

        List<String> colored = HexColorUtil.color(List.of("&aОдин", "#FF0000Два"));

        assertEquals(2, colored.size());
        assertEquals("§aОдин", colored.get(0));
        assertTrue(colored.get(1).startsWith("§x"));

    }
}
