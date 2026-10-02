package eu.neydev.expbottle.support.skull;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка записи владельца и тега SkullOwner: штатный метод меты, дерево
 * тегов с текстурой и обратное зеркало NMS-копии в предмет Bukkit.
 */
class SkullOwnerWriterTest {

    private static final String PAYLOAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly9iLnBuZyJ9fX0=";

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
    @DisplayName("applyOwner without an owner leaves the meta alone")
    void applyOwnerWithoutOwner() {

        SkullMeta meta = newSkullMeta();

        SkullOwnerWriter.applyOwner(meta, null);
        SkullOwnerWriter.applyOwner(meta, "");

        assertFalse(meta.hasOwner());

    }

    @Test
    @DisplayName("applyOwner sets the owner by name")
    @SuppressWarnings("deprecation")
    void applyOwnerSetsName() {

        SkullMeta meta = newSkullMeta();

        SkullOwnerWriter.applyOwner(meta, "Ney");
        assertEquals("Ney", meta.getOwner());

    }

    @Test
    @DisplayName("applyOwner survives a meta that refuses the owner")
    void applyOwnerSurvivesRefusal() {
        SkullOwnerWriter.applyOwner(new RefusingSkullMeta(), "Ney");
    }

    @Test
    @DisplayName("setOwnerProfile is missing on cores without the profile API")
    void setOwnerProfileMissing() {
        assertThrows(Exception.class, () -> SkullOwnerWriter.setOwnerProfile(newSkullMeta(), new Object()));
    }

    @Test
    @DisplayName("writeSkullOwner builds the SkullOwner tag tree with the texture")
    void writeSkullOwnerBuildsTree() {

        CompoundTag tag = new CompoundTag();

        assertTrue(SkullOwnerWriter.writeSkullOwner(tag, CompoundTag.class, PAYLOAD, "Ney"));

        CompoundTag owner = (CompoundTag) tag.values().get("SkullOwner");
        assertNotNull(owner);
        assertEquals(GameProfileFactory.stableUuid(PAYLOAD).toString(), owner.values().get("Id"));
        assertEquals("Ney", owner.values().get("Name"));

        CompoundTag properties = (CompoundTag) owner.values().get("Properties");
        CompoundTag textures = (CompoundTag) properties.values().get("textures");
        CompoundTag texture = (CompoundTag) textures.values().get("textures");

        assertEquals(PAYLOAD, texture.values().get("Value"));

    }

    @Test
    @DisplayName("writeSkullOwner gives up on a tag without string methods")
    void writeSkullOwnerWithoutMethods() {
        EmptyTag tag = new EmptyTag();
        assertFalse(SkullOwnerWriter.writeSkullOwner(tag, EmptyTag.class, PAYLOAD, "Ney"));
    }

    @Test
    @DisplayName("writeSkullOwner gives up on a tag class without a default constructor")
    void writeSkullOwnerWithoutConstructor() {
        NoDefaultConstructor tag = new NoDefaultConstructor("stub");
        assertFalse(SkullOwnerWriter.writeSkullOwner(tag, NoDefaultConstructor.class, PAYLOAD, "Ney"));
    }

    @Test
    @DisplayName("mirrorBack returns the bukkit copy of the nms stack")
    void mirrorBackSuccess() {

        Object nmsStack = be.seeseemelk.mockbukkit.CraftItemStack.asNMSCopy(new ItemStack(Material.PLAYER_HEAD));

        ItemStack mirrored = SkullOwnerWriter.mirrorBack(be.seeseemelk.mockbukkit.CraftItemStack.class, nmsStack);

        assertNotNull(mirrored);
        assertEquals(Material.PLAYER_HEAD, mirrored.getType());

    }

    @Test
    @DisplayName("mirrorBack returns null when the class has no mirror method")
    void mirrorBackWithoutMethod() {
        assertNull(SkullOwnerWriter.mirrorBack(NoMirror.class, new Object()));
    }

    @Test
    @DisplayName("mirrorBack swallows a mirror method that throws")
    void mirrorBackThrowing() {
        Object stub = new be.seeseemelk.mockbukkit.CraftItemStack.NmsStackStub();
        assertNull(SkullOwnerWriter.mirrorBack(ThrowingMirror.class, stub));
    }

    @Test
    @DisplayName("mirrorBack returns null when the mirror is not an item")
    void mirrorBackWrongType() {
        Object stub = new be.seeseemelk.mockbukkit.CraftItemStack.NmsStackStub();
        assertNull(SkullOwnerWriter.mirrorBack(WrongMirror.class, stub));
    }

    @SuppressWarnings("deprecation")
    private SkullMeta newSkullMeta() {
        return (SkullMeta) new ItemStack(Material.PLAYER_HEAD).getItemMeta();
    }

    /**
     * Мета, отказывающаяся ставить владельца: ветка «владелец не критичен».
     */
    static class RefusingSkullMeta extends be.seeseemelk.mockbukkit.inventory.meta.SkullMetaMock {

        @Override
        public boolean setOwningPlayer(OfflinePlayer owner) {
            throw new UnsupportedOperationException("stub: the meta refuses owners");
        }
    }

    /**
     * Тег без единого метода записи: цепочка строк не соберётся.
     */
    public static class EmptyTag {
    }

    /**
     * Тег без публичного конструктора по умолчанию: getConstructor падает.
     */
    public static class NoDefaultConstructor {

        public NoDefaultConstructor(String stub) {
        }
    }

    /**
     * Класс без методов зеркала: оба имени кандидата отсутствуют.
     */
    public static class NoMirror {
    }

    /**
     * Зеркало, бросающее исключение: отказ ядра глотается.
     */
    public static class ThrowingMirror {

        public static ItemStack asBukkitCopy(be.seeseemelk.mockbukkit.CraftItemStack.NmsStackStub stack) {
            throw new IllegalStateException("stub: the mirror is broken");
        }
    }

    /**
     * Зеркало, возвращающее не предмет: результат отбрасывается.
     */
    public static class WrongMirror {

        public static String asBukkitCopy(be.seeseemelk.mockbukkit.CraftItemStack.NmsStackStub stack) {
            return "not an item";
        }
    }
}
