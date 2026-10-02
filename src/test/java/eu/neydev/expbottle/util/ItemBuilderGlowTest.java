package eu.neydev.expbottle.util;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Блеск предмета: на ядре с реестром зачарований запасной путь ставит
 * невидимое зачарование и прячет его флагом.
 */
class ItemBuilderGlowTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        registerMockEnchantment("glow_probe", "UNBREAKING");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("The glow applies a hidden enchantment when the registry exists")
    @SuppressWarnings("deprecation")
    void glowAppliesHiddenEnchantment() {

        ItemStack item = new ItemBuilder(Material.PAPER)
                .setGlow(true)
                .build();

        assertFalse(item.getEnchantments().isEmpty(), "The fake enchantment gives the glint");
        assertTrue(item.getItemMeta().hasItemFlag(ItemFlag.HIDE_ENCHANTS), "The enchantment list stays hidden");

    }

    @Test
    @DisplayName("The hide flag applies without an enchantment")
    void hideEnchantmentsFlag() {

        ItemStack item = new ItemBuilder(Material.PAPER)
                .setHideEnchantments(true)
                .build();

        assertTrue(item.getItemMeta().hasItemFlag(ItemFlag.HIDE_ENCHANTS));

    }

    /**
     * Мок-сервер не регистрирует ванильные зачарования: тест добавляет своё,
     * как это делает интеграционный тест меню.
     */
    @SuppressWarnings("deprecation")
    private void registerMockEnchantment(String key, String legacyName) {

        // Мок-сервер держит ванильные зачарования только в реестре ключей,
        // а getByName ищет по legacy-имени: своё зачарование регистрируем
        // под отдельным ключом, чтобы не столкнуться с ванильным
        NamespacedKey namespacedKey = new NamespacedKey("minecraft", key);

        if (Enchantment.getByKey(namespacedKey) != null) {
            return;
        }

        try {
            Enchantment.registerEnchantment(new LegacyNameEnchantment(namespacedKey, legacyName));
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // Уже зарегистрировано в этой JVM
        }

    }

    /**
     * Зачарование с legacy-именем: {@code Enchantment.getByName} ищет по getName,
     * а мок из MockBukkit отдаёт ключ вместо имени.
     */
    static class LegacyNameEnchantment extends Enchantment {

        private final String legacyName;

        LegacyNameEnchantment(NamespacedKey key, String legacyName) {

            super(key);
            this.legacyName = legacyName;

        }

        @Override
        public @NotNull String getName() {
            return legacyName;
        }

        @Override
        public int getMaxLevel() {
            return 3;
        }

        @Override
        public int getStartLevel() {
            return 1;
        }

        @Override
        public @NotNull EnchantmentTarget getItemTarget() {
            return EnchantmentTarget.ALL;
        }

        @Override
        public boolean isTreasure() {
            return false;
        }

        @Override
        public boolean isCursed() {
            return false;
        }

        @Override
        public boolean conflictsWith(@NotNull Enchantment other) {
            return false;
        }

        @Override
        public boolean canEnchantItem(@NotNull ItemStack item) {
            return true;
        }
    }
}
