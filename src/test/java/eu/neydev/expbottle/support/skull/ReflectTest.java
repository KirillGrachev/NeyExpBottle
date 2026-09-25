package eu.neydev.expbottle.support.skull;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.com.mojang.authlib.GameProfile;
import net.minecraft.util.com.mojang.authlib.properties.PropertyMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка отражательных приёмчиков: поиск классов (включая craft-пакет
 * сервера), полей по имени типа и безопасные вызовы методов с перебором имён.
 */
class ReflectTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("firstClass returns the first loadable name and null when none loads")
    void firstClass() {

        assertEquals(String.class, Reflect.firstClass("no.such.Klass", "java.lang.String"));
        assertNull(Reflect.firstClass("no.such.Klass", "no.such.Klass.too"));

    }

    @Test
    @DisplayName("resolveCraftClass finds the class in the server package")
    void resolveCraftClassFromServerPackage() {

        Class<?> resolved = Reflect.resolveCraftClass("CraftItemStack");

        assertEquals(be.seeseemelk.mockbukkit.CraftItemStack.class, resolved);

    }

    @Test
    @DisplayName("resolveCraftClass returns null after trying every versioned package")
    void resolveCraftClassMissing() {
        assertNull(Reflect.resolveCraftClass("NoSuchCraftClass"));
    }

    @Test
    @DisplayName("findFieldOfType walks superclasses and skips static fields")
    void findFieldOfType() {

        ChildBox box = new ChildBox();

        assertInstanceOf(PropertyMap.class, Reflect.findFieldValue(box, "PropertyMap"));
        assertNull(Reflect.findFieldValue(box, "GameProfile"), "static fields are skipped");
        assertNull(Reflect.findFieldValue(box, "NoSuchType"));

        assertNotNull(Reflect.findFieldOfType(ChildBox.class, "PropertyMap"), "the field lives in the superclass");

    }

    @Test
    @DisplayName("invokeAny calls the first matching no-argument method")
    void invokeAnyNoArguments() {

        GameProfile profile = new GameProfile(UUID.randomUUID(), "Ney");

        assertInstanceOf(PropertyMap.class, Reflect.invokeAny(profile, "nope", "getProperties"));
        assertNull(Reflect.invokeAny(profile, "nope", "nothing"));

    }

    @Test
    @DisplayName("requireMethod finds the method or throws NoSuchMethodException")
    void requireMethod() throws NoSuchMethodException {

        assertNotNull(Reflect.requireMethod(String.class, "trim"));
        assertThrows(NoSuchMethodException.class, () -> Reflect.requireMethod(String.class, "nope"));

    }

    @Test
    @DisplayName("invokeSetter tries the names until one exists and stays silent otherwise")
    void invokeSetter() {

        SetterBox box = new SetterBox();
        CompoundTag tag = new CompoundTag();

        Reflect.invokeSetter(box, CompoundTag.class, new String[] {"nope", "accept"}, tag);
        assertSame(tag, box.value);

        Reflect.invokeSetter(box, CompoundTag.class, new String[] {"nope", "never"}, new CompoundTag());
        assertSame(tag, box.value, "no candidate matched - the box keeps the first tag");

    }

    @Test
    @DisplayName("invokeAny with a key calls the first matching two-argument method")
    void invokeAnyKeyValue() {

        CompoundTag tag = new CompoundTag();
        CompoundTag child = new CompoundTag();

        assertTrue(Reflect.invokeAny(tag, CompoundTag.class, new String[] {"nope", "set"}, "k", child));
        assertSame(child, tag.values().get("k"));

        assertFalse(Reflect.invokeAny(tag, CompoundTag.class, new String[] {"nope", "missing"}, "k", child));

    }

    @Test
    @DisplayName("invokeAny with a key requires the (String, argument) signature, not (argument, argument)")
    void invokeAnySignatureContract() {

        // Контракт против регресса: NMS-теги имеют put(String, Tag), и поиск
        // обязан игнорировать методы с двумя одинаковыми параметрами
        SameTypeOnly sameType = new SameTypeOnly();
        assertFalse(Reflect.invokeAny(sameType, CompoundTag.class, new String[] {"put"}, "k", new CompoundTag()),
                "a (Tag, Tag) method must not match a (String, Tag) call");

        KeyedTarget keyed = new KeyedTarget();
        assertTrue(Reflect.invokeAny(keyed, CompoundTag.class, new String[] {"nope", "put"}, "k", new CompoundTag()));
        assertNotNull(keyed.values.get("k"));

    }

    /**
     * Тег с методом «два одинаковых параметра»: до фикса invokeAny находил его
     * и падал IllegalArgumentException на строковом ключе.
     */
    static class SameTypeOnly {

        public void put(CompoundTag key, CompoundTag value) {
            throw new AssertionError("must not be called with a string key");
        }
    }

    /**
     * Тег с контрактной сигнатурой (String, Tag).
     */
    static class KeyedTarget {

        private final java.util.Map<String, Object> values = new java.util.HashMap<>();

        public void put(String key, CompoundTag value) {
            values.put(key, value);
        }
    }

    /**
     * Родитель с полем-картой свойств: поиск обязан дойти до суперкласса.
     */
    static class ParentBox {

        private final PropertyMap map = new PropertyMap();

    }

    /**
     * Наследник со статичным полем типа GameProfile: статика пропускается.
     */
    static class ChildBox extends ParentBox {

        private static GameProfile profile;

    }

    /**
     * Приёмник тега: сеттер находится со второго имени-кандидата.
     */
    static class SetterBox {

        private CompoundTag value;

        public void accept(CompoundTag tag) {
            this.value = tag;
        }
    }
}
