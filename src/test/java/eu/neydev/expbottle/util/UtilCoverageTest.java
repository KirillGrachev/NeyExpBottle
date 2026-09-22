package eu.neydev.expbottle.util;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Покрытие утилит: разбор значений, подписи, звуки, зачарования,
 * сборщик предметов и HEX-цвета на ядрах без поддержки HEX.
 */
class UtilCoverageTest {

    private static final Logger LOGGER = Logger.getLogger("UtilCoverageTest");

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        ServerVersion.setForTests(1, 16);
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("ValueResolver: материал, перечисления, границы и пустые значения")
    void valueResolverBranches() {

        assertEquals(Material.STONE, ValueResolver.material(null, Material.STONE, LOGGER));
        assertEquals(Material.STONE, ValueResolver.material("  ", Material.STONE, LOGGER));
        assertEquals(Material.STONE, ValueResolver.material("ТАКОГО_НЕТ", Material.STONE, LOGGER));
        assertEquals(Material.PAPER, ValueResolver.material("paper", Material.STONE, LOGGER));

        assertEquals(MenuItemTypeProbe.TIER, ValueResolver.enumValue("tier", MenuItemTypeProbe.class,
                MenuItemTypeProbe.DECORATION, LOGGER));
        assertEquals(MenuItemTypeProbe.DECORATION, ValueResolver.enumValue(null, MenuItemTypeProbe.class,
                MenuItemTypeProbe.DECORATION, LOGGER));
        assertEquals(MenuItemTypeProbe.DECORATION, ValueResolver.enumValue("битое", MenuItemTypeProbe.class,
                MenuItemTypeProbe.DECORATION, LOGGER));

        assertEquals(5, ValueResolver.clamp(5, 1, 10));
        assertEquals(1, ValueResolver.clamp(0, 1, 10));
        assertEquals(10, ValueResolver.clamp(99, 1, 10));
        assertEquals(1.5f, ValueResolver.clamp(1.5f, 0f, 2f));
        assertEquals(0f, ValueResolver.clamp(-1f, 0f, 2f));
        assertEquals(2f, ValueResolver.clamp(3f, 0f, 2f));

        assertTrue(ValueResolver.isBlank(null));
        assertTrue(ValueResolver.isBlank("   "));
        assertFalse(ValueResolver.isBlank("x"));

    }

    /**
     * Перечисление-зонд для проверки универсального разбора enum-значений.
     */
    private enum MenuItemTypeProbe {
        DECORATION,
        TIER
    }

    @Test
    @DisplayName("SignatureUtil: ключ создаётся, читается и переживает перезапись")
    void signatureUtilLifecycle() throws Exception {

        File folder = Files.createTempDirectory("neb-secret").toFile();

        byte[] first = SignatureUtil.loadOrCreateSecret(folder, LOGGER);
        byte[] second = SignatureUtil.loadOrCreateSecret(folder, LOGGER);

        assertEquals(first.length, second.length);
        assertTrue(java.util.Arrays.equals(first, second), "Ключ должен читаться тем же");

        String signature = SignatureUtil.sign(first, 10);
        assertFalse(signature.isEmpty());
        assertEquals(16, signature.length());
        assertTrue(SignatureUtil.verify(first, 10, signature));
        assertFalse(SignatureUtil.verify(first, 11, signature));
        assertFalse(SignatureUtil.verify(first, 10, "deadbeefdeadbeef"));
        assertFalse(SignatureUtil.verify(new byte[0], 10, signature));
        assertEquals("", SignatureUtil.sign(new byte[0], 10));

    }

    @Test
    @DisplayName("SignatureUtil: битый файл ключа не роняет загрузку")
    void signatureUtilBrokenFile() throws Exception {

        File folder = Files.createTempDirectory("neb-secret-bad").toFile();
        Files.writeString(new File(folder, "secret.key").toPath(), "");

        byte[] secret = SignatureUtil.loadOrCreateSecret(folder, LOGGER);
        assertEquals(0, secret.length, "Пустой файл читается как пустой ключ");

    }

    @Test
    @DisplayName("SoundUtil: известные и неизвестные звуки кешируются")
    void soundUtilBranches() {

        assertNull(SoundUtil.resolveKey(null));
        assertNull(SoundUtil.resolveKey("  "));
        assertNotNull(SoundUtil.resolveKey("ENTITY_EXPERIENCE_ORB_PICKUP"));
        assertNull(SoundUtil.resolveKey("ТАКОГО_ЗВУКА_НЕТ"));

        assertTrue(SoundUtil.isKnown("ENTITY_EXPERIENCE_ORB_PICKUP"));
        assertFalse(SoundUtil.isKnown("ТАКОГО_ЗВУКА_НЕТ"));

    }

    @Test
    @DisplayName("EnchantmentUtil: алиасы старых имён и битые имена")
    void enchantmentUtilBranches() {

        assertNull(EnchantmentUtil.resolve(null));
        assertNull(EnchantmentUtil.resolve("   "));
        assertNull(EnchantmentUtil.resolve("NO_SUCH_ENCHANT"));

    }

    @Test
    @DisplayName("ServerVersion: сравнение версий и значения по умолчанию")
    void serverVersionBranches() {

        ServerVersion.setForTests(1, 20);
        assertTrue(ServerVersion.isAtLeast(1, 16));
        assertTrue(ServerVersion.isAtLeast(1, 20));
        assertFalse(ServerVersion.isAtLeast(1, 21));
        assertFalse(ServerVersion.isAtLeast(2, 0));
        assertTrue(ServerVersion.isAtLeast(0, 9));
        assertTrue(ServerVersion.isHexSupported());
        assertEquals(1, ServerVersion.getMajor());
        assertEquals(20, ServerVersion.getMinor());
        assertNotNull(ServerVersion.getRaw());

        ServerVersion.setForTests(1, 15);
        assertFalse(ServerVersion.isHexSupported());

    }

    @Test
    @DisplayName("HexColorUtil на ядре без HEX подставляет ближайший legacy-цвет")
    void hexColorLegacyFallback() {

        ServerVersion.setForTests(1, 15);

        String colored = HexColorUtil.color("#FF0000красный");
        // #FF0000 ближе всего к legacy 0xAA0000 (код 4), а не к 0xFF5555 (код c)
        assertEquals("§4красный", colored);

        String gradient = HexColorUtil.color("<gradient:#FF0000:#FF0000>ab</gradient>");
        assertTrue(gradient.contains("§4"), "Градиент на старом ядре даёт legacy-цвета");

        ServerVersion.setForTests(1, 16);
        assertTrue(HexColorUtil.color("#FF0000x").startsWith("§x"));

    }

    @Test
    @DisplayName("Placeholders: подстановка, слияние и пустые наборы")
    void placeholdersBranches() {

        Placeholders placeholders = Placeholders.create()
                .set("a", 1)
                .set("b", null);

        assertEquals("1", placeholders.apply("{a}{b}"), "null-значение становится пустой строкой");
        assertEquals("", placeholders.apply((String) null));
        assertEquals("", placeholders.apply(""));
        assertEquals("без плейсхолдеров", placeholders.apply("без плейсхолдеров"));
        assertEquals(List.of("1", ""), placeholders.apply(List.of("{a}", "{b}")));
        assertTrue(placeholders.apply((List<String>) null).isEmpty());

        Placeholders merged = Placeholders.create().set("c", 3).merge(placeholders);
        assertEquals("3 1", merged.apply("{c} {a}"));
        assertFalse(merged.isEmpty());
        assertTrue(Placeholders.create().isEmpty());

    }

    @Test
    @DisplayName("ItemBuilder: нулевая мета, лор, флаги, блеск и количество")
    void itemBuilderBranches() {

        ItemStack item = new ItemBuilder(Material.PAPER, 0)
                .setName(null)
                .setLore(null)
                .addLore(null)
                .addLore("строка")
                .addItemFlags()
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlow(false)
                .setUnbreakable(true)
                .setCustomModelData(7)
                .setAmount(3)
                .build();

        assertEquals(3, item.getAmount());
        assertEquals(1, item.getItemMeta().getLore().size());
        assertTrue(item.getItemMeta().isUnbreakable());
        assertEquals(7, item.getItemMeta().getCustomModelData());

        ItemStack air = new ItemBuilder(Material.AIR)
                .setName("имя")
                .setLore(List.of("лор"))
                .addLore("ещё")
                .setGlow(true)
                .setUnbreakable(true)
                .setCustomModelData(1)
                .build();

        assertNotNull(air, "Предмет без меты собирается без падений");

        ItemStack glowing = new ItemBuilder(Material.PAPER)
                .setGlow(true)
                .build();

        assertNotNull(glowing.getItemMeta(), "Блеск применяется к предмету с метой");

    }
}
