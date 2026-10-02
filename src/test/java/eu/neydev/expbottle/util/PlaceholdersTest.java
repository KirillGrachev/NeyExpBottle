package eu.neydev.expbottle.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Подстановка плейсхолдеров, включая значения, которые сами содержат плейсхолдеры.
 */
class PlaceholdersTest {

    @Test
    @DisplayName("Nested placeholders resolve regardless of the declaration order")
    void nestedValuesAreResolved() {

        Placeholders placeholders = Placeholders.create()
                .set("amount_label", "16")
                .set("amount_hint", "RMB ({amount_label})")
                .set("outer", "hint: {amount_hint}");

        assertEquals("hint: RMB (16)", placeholders.apply("{outer}"));

    }

    @Test
    @DisplayName("A self-referencing value does not hang the substitution")
    void selfReferenceStops() {
        Placeholders placeholders = Placeholders.create().set("loop", "{loop}");
        assertEquals("{loop}", placeholders.apply("{loop}"));
    }

    @Test
    @DisplayName("Empty and null text, an empty set, a list of lines")
    void emptyInputs() {

        Placeholders empty = Placeholders.create();

        assertEquals("", empty.apply((String) null));
        assertEquals("", empty.apply(""));
        assertEquals("as is", empty.apply("as is"));
        assertTrue(empty.isEmpty());
        assertEquals(List.of(), empty.apply((List<String>) null));

        Placeholders placeholders = Placeholders.create().set("a", 1);

        assertEquals(List.of("1", ""), placeholders.apply(Arrays.asList("{a}", null)));

    }

    @Test
    @DisplayName("merge carries values over, null becomes an empty string")
    void mergeAndNulls() {

        Placeholders base = Placeholders.create().set("a", "1");
        base.merge(Placeholders.create().set("b", null));

        assertFalse(base.isEmpty());
        assertEquals("1-", base.apply("{a}-{b}"));

    }
}