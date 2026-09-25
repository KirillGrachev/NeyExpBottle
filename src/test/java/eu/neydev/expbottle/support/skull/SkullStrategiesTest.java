package eu.neydev.expbottle.support.skull;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import net.minecraft.util.com.mojang.authlib.GameProfile;
import net.minecraft.world.item.component.ResolvableProfile;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Проверка способов нанесения текстуры: какие поля меты ищет каждый способ,
 * что происходит на ядре без нужных методов и как работает последний рубеж
 * через тег NMS.
 */
class SkullStrategiesTest {

    private static final String PAYLOAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly9jLnBuZyJ9fX0=";

    private ServerMock server;

    @BeforeEach
    void setUp() {

        server = MockBukkit.mock();
        primePropertyStub();

    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Every strategy keeps its short id for the log")
    void ids() {

        assertEquals("paper_profile", new PaperProfileStrategy().id());
        assertEquals("craft_profile_properties", new CraftProfilePropertiesStrategy().id());
        assertEquals("game_profile_field", new GameProfileFieldStrategy().id());
        assertEquals("reserialized_profile", new ReserializedProfileStrategy().id());
        assertEquals("nms_tag", new NmsTagStrategy().id());

    }

    @Test
    @DisplayName("The field strategy skips a meta without a GameProfile field")
    void fieldStrategySkipsPlainMeta() throws Exception {

        ItemStack result = new GameProfileFieldStrategy().apply(newSkullMeta(), head(), PAYLOAD, "Ney");

        assertNull(result);

    }

    @Test
    @DisplayName("The field strategy writes the profile into the meta field")
    @SuppressWarnings("deprecation")
    void fieldStrategyWritesProfile() throws Exception {

        ProfileSkullMeta meta = new ProfileSkullMeta();
        ItemStack item = head();

        assertSame(item, new GameProfileFieldStrategy().apply(meta, item, PAYLOAD, "Ney"));

        GameProfile profile = (GameProfile) Reflect.findFieldValue(meta, "GameProfile");

        assertNotNull(profile);
        assertNotNull(profile.getProperties().entries().get(GameProfileFactory.TEXTURE_PROPERTY));
        assertEquals("Ney", meta.getOwner());

    }

    @Test
    @DisplayName("The reserialized strategy skips a meta without a ResolvableProfile field")
    void reserializedStrategySkipsPlainMeta() throws Exception {

        ItemStack result = new ReserializedProfileStrategy().apply(newSkullMeta(), head(), PAYLOAD, "Ney");

        assertNull(result);

    }

    @Test
    @DisplayName("The reserialized strategy writes the resolvable profile into the field")
    void reserializedStrategyWritesProfile() throws Exception {

        // Перебор конструкторов ResolvableProfile сбрасывается и прогревается:
        // первый отказ уже потрачен, способ получит рабочую сигнатуру
        ResolvableProfile.resetStub();
        GameProfileFactory.newResolvableProfile(PAYLOAD, "Ney");

        ResolvableSkullMeta meta = new ResolvableSkullMeta();
        ItemStack item = head();

        assertSame(item, new ReserializedProfileStrategy().apply(meta, item, PAYLOAD, "Ney"));

        ResolvableProfile profile = (ResolvableProfile) Reflect.findFieldValue(meta, "ResolvableProfile");

        assertNotNull(profile);
        assertNotNull(profile.profile());

    }

    @Test
    @DisplayName("The paper strategy dies on a core without Bukkit.createProfile")
    void paperStrategyDies() {
        assertThrows(NoSuchMethodException.class,
                () -> new PaperProfileStrategy().apply(newSkullMeta(), head(), PAYLOAD, "Ney"));
    }

    @Test
    @DisplayName("The craft strategy dies on a core without Bukkit.createPlayerProfile")
    void craftStrategyDies() {
        assertThrows(NoSuchMethodException.class,
                () -> new CraftProfilePropertiesStrategy().apply(newSkullMeta(), head(), PAYLOAD, "Ney"));
    }

    @Test
    @DisplayName("The nms strategy writes the tag and mirrors the item back")
    void nmsStrategyApplies() throws Exception {

        NmsTagStrategy strategy = new NmsTagStrategy();
        ItemStack item = head();

        // Первый запрос копии: тега ещё нет, он создаётся и записывается
        ItemStack first = strategy.apply(newSkullMeta(), item, PAYLOAD, "Ney");
        assertNotNull(first);

        // Повторный запрос того же материала: тег уже на месте
        ItemStack second = strategy.apply(newSkullMeta(), item, PAYLOAD, null);
        assertNotNull(second);

    }

    @Test
    @DisplayName("The nms strategy dies on an item the core refuses to copy")
    void nmsStrategyDiesOnUncopyableItem() {

        NmsTagStrategy strategy = new NmsTagStrategy();

        // Отказ ядра приходит через отражение и завёрнут в InvocationTargetException
        Exception exception = assertThrows(Exception.class,
                () -> strategy.apply(newSkullMeta(), new ItemStack(Material.STONE), PAYLOAD, "Ney"));

        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());

    }

    /**
     * Прогрев stub-свойства: способы, собирающие профиль, видят рабочий authlib.
     */
    private static void primePropertyStub() {

        net.minecraft.util.com.mojang.authlib.properties.Property.resetStub();
        GameProfileFactory.newAuthlibProperty(PAYLOAD);
        GameProfileFactory.newAuthlibProperty(PAYLOAD);

    }

    private SkullMeta newSkullMeta() {
        return (SkullMeta) head().getItemMeta();
    }

    private ItemStack head() {
        return new ItemStack(Material.PLAYER_HEAD);
    }

    /**
     * Мета с полем типа GameProfile: так скалл-мета выглядела до 1.20.5.
     */
    static class ProfileSkullMeta extends be.seeseemelk.mockbukkit.inventory.meta.SkullMetaMock {

        private GameProfile profile;

    }

    /**
     * Мета с полем типа ResolvableProfile: так скалл-мета выглядит с 1.20.5.
     */
    static class ResolvableSkullMeta extends be.seeseemelk.mockbukkit.inventory.meta.SkullMetaMock {

        private ResolvableProfile resolvable;

    }
}
