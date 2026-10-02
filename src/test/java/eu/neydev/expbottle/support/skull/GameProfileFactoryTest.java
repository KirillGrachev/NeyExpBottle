package eu.neydev.expbottle.support.skull;

import net.minecraft.util.com.mojang.authlib.GameProfile;
import net.minecraft.util.com.mojang.authlib.properties.Property;
import net.minecraft.util.com.mojang.authlib.properties.PropertyMap;
import net.minecraft.world.item.component.ResolvableProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка сборки authlib-профиля: владелец, производный UUID, свойство
 * текстуры с перебором сигнатур конструктора и ResolvableProfile с перебором
 * конструкторов между версиями ядра.
 */
class GameProfileFactoryTest {

    private static final String PAYLOAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly9hLnBuZyJ9fX0=";

    @BeforeEach
    void setUp() {
        primePropertyStub();
    }

    @Test
    @DisplayName("ownerName falls back to the default owner")
    void ownerName() {

        assertEquals(GameProfileFactory.DEFAULT_OWNER_NAME, GameProfileFactory.ownerName(null));
        assertEquals(GameProfileFactory.DEFAULT_OWNER_NAME, GameProfileFactory.ownerName(""));
        assertEquals("Ney", GameProfileFactory.ownerName("Ney"));

    }

    @Test
    @DisplayName("stableUuid is derived from the payload and stays put")
    void stableUuid() {
        assertEquals(GameProfileFactory.stableUuid(PAYLOAD), GameProfileFactory.stableUuid(PAYLOAD));
        assertNotEquals(GameProfileFactory.stableUuid(PAYLOAD), GameProfileFactory.stableUuid(PAYLOAD + "x"));
    }

    @Test
    @DisplayName("newGameProfile carries the owner, the stable uuid and the texture")
    void newGameProfile() throws Exception {

        GameProfile profile = (GameProfile) GameProfileFactory.newGameProfile(PAYLOAD, "Ney");

        assertEquals("Ney", profile.getName());
        assertEquals(GameProfileFactory.stableUuid(PAYLOAD), profile.getId());

        Property property = (Property) profile.getProperties().entries().get(GameProfileFactory.TEXTURE_PROPERTY);

        assertEquals(GameProfileFactory.TEXTURE_PROPERTY, property.getName());
        assertEquals(PAYLOAD, property.getValue());
        assertNull(property.getSignature(), "the three-argument signature passes a null signature");

    }

    @Test
    @DisplayName("newGameProfile uses the default owner when none is given")
    void newGameProfileDefaultOwner() throws Exception {
        GameProfile profile = (GameProfile) GameProfileFactory.newGameProfile(PAYLOAD, null);
        assertEquals(GameProfileFactory.DEFAULT_OWNER_NAME, profile.getName());
    }

    @Test
    @DisplayName("putTextureProperty rejects a profile without a property map")
    void putTexturePropertyWithoutMap() {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "Ney", null);
        assertThrows(IllegalStateException.class, () -> GameProfileFactory.putTextureProperty(profile, PAYLOAD));
    }

    @Test
    @DisplayName("newAuthlibProperty walks both signatures in three attempts")
    void newAuthlibProperty() {

        // Счётчики stub-конструкторов сбрасываются: первый прогон перебора
        // ловит отказ обеих сигнатур, следующие два — каждую успешную
        Property.resetStub();

        assertNull(GameProfileFactory.newAuthlibProperty(PAYLOAD), "both signatures refused - null");

        Property first = (Property) GameProfileFactory.newAuthlibProperty(PAYLOAD);
        Property second = (Property) GameProfileFactory.newAuthlibProperty(PAYLOAD + "x");

        assertEquals(PAYLOAD, first.getValue());
        assertEquals(PAYLOAD + "x", second.getValue());

    }

    @Test
    @DisplayName("newResolvableProfile returns null once every constructor refuses")
    void newResolvableProfileGivesUp() throws Exception {
        ResolvableProfile.resetStub();
        assertNull(GameProfileFactory.newResolvableProfile(PAYLOAD, "Ney"));
    }

    @Test
    @DisplayName("newResolvableProfile walks the constructors until one fits")
    void newResolvableProfile() throws Exception {

        ResolvableProfile.resetStub();
        assertNull(GameProfileFactory.newResolvableProfile(PAYLOAD, "Ney"), "the first full-signature attempt refuses");

        ResolvableProfile profile = (ResolvableProfile) GameProfileFactory.newResolvableProfile(PAYLOAD, "Ney");

        assertInstanceOf(GameProfile.class, profile.profile());

    }

    @Test
    @DisplayName("fillArguments maps the profile, futures and primitives")
    void fillArguments() {

        GameProfile profile = new GameProfile(UUID.randomUUID(), "Ney");

        Object[] byProfile = new Object[1];
        assertTrue(GameProfileFactory.fillArguments(new Class<?>[] {GameProfile.class}, byProfile, profile));
        assertSame(profile, byProfile[0]);

        Object[] byFuture = new Object[1];
        assertTrue(GameProfileFactory.fillArguments(new Class<?>[] {CompletableFuture.class}, byFuture, profile));
        assertInstanceOf(CompletableFuture.class, byFuture[0]);

        Object[] mixed = new Object[2];
        assertTrue(GameProfileFactory.fillArguments(new Class<?>[] {GameProfile.class, boolean.class}, mixed, profile));
        assertEquals(Boolean.TRUE, mixed[1]);

        Object[] withInt = new Object[2];
        assertTrue(GameProfileFactory.fillArguments(new Class<?>[] {GameProfile.class, int.class}, withInt, profile));
        assertEquals(0, withInt[1]);

        Object[] primitives = new Object[2];
        assertFalse(GameProfileFactory.fillArguments(new Class<?>[] {boolean.class, int.class}, primitives, profile),
                "without the profile the signature is useless");

        Object[] unsupported = new Object[1];
        assertFalse(GameProfileFactory.fillArguments(new Class<?>[] {String.class}, unsupported, profile));

    }

    /**
     * Прогрев stub-свойства: после двух прогонов перебора оба конструктора
     * отвечают успехом, и остальные тесты видят предсказуемый authlib.
     */
    private static void primePropertyStub() {

        Property.resetStub();
        
        GameProfileFactory.newAuthlibProperty(PAYLOAD);
        GameProfileFactory.newAuthlibProperty(PAYLOAD);

    }

    @Test
    @DisplayName("A profile map keeps the texture under the textures key")
    void propertyMapKey() {

        PropertyMap map = new PropertyMap();
        map.put(GameProfileFactory.TEXTURE_PROPERTY, "value");

        assertEquals("value", map.entries().get("textures"));

    }
}
