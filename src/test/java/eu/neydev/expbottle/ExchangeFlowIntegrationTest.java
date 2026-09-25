package eu.neydev.expbottle;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.enchantments.EnchantmentMock;
import eu.neydev.expbottle.event.BottleThrowHandler;
import eu.neydev.expbottle.service.AmountSelectionService;
import eu.neydev.expbottle.gui.Menu;
import eu.neydev.expbottle.gui.MenuHolder;
import eu.neydev.expbottle.model.BottleData;
import eu.neydev.expbottle.util.ExperienceFormula;
import eu.neydev.expbottle.util.HexColorUtil;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Обмен уровней на бутылку через меню: успех, отказ по уровням и пузырькам, перерисовка витрины и границы фабрики.
 */
class ExchangeFlowIntegrationTest extends PluginTestHarness {

    @Test
    @DisplayName("The exchange charges the levels and an empty bottle and gives the bottle")
    void exchangeCreatesBottle() {

        PlayerMock player = operator("Ney");
        player.setLevel(30);
        player.setExp(0.0f);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));

        player.performCommand("exp");
        click(player, SLOT_TIER_5);

        assertEquals(25, player.getLevel(), "The levels were charged incorrectly. Messages: " + drainMessages(player));
        assertFalse(player.getInventory().contains(Material.GLASS_BOTTLE), "The empty bottle was not charged");

        ItemStack bottle = findBottle(player);
        assertNotNull(bottle, "The experience bottle was not given");

        BottleData data = plugin.getServices().getBottleTagService().read(bottle);
        assertTrue(data.bottle());
        assertEquals(5, data.levels());
        assertFalse(data.forged(), "The plugin bottle must carry a valid signature");
        assertEquals(1, plugin.getServices().getDiagnosticsService().getBottlesCreated());

    }

    @Test
    @DisplayName("The exchange refreshes the menu: without experience the button goes dark")
    void exchangeRefreshesMenu() {

        PlayerMock player = operator("Ney");
        player.setLevel(5);
        player.setExp(0.0f);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 2));

        player.performCommand("exp");

        click(player, SLOT_TIER_5);
        assertEquals(0, player.getLevel());

        click(player, SLOT_TIER_5);
        assertEquals(0, player.getLevel(), "The second exchange went through without experience");
        assertTrue(drainMessages(player).contains("don't have"));

    }

    @Test
    @DisplayName("Without experience the exchange fails and gives nothing")
    void exchangeWithoutExperienceFails() {

        PlayerMock player = operator("Ney");
        player.setLevel(1);
        player.setExp(0.0f);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));

        player.performCommand("exp");
        click(player, SLOT_TIER_5);

        assertEquals(1, player.getLevel(), "The level must not change");
        assertTrue(player.getInventory().contains(Material.GLASS_BOTTLE), "The empty bottle must not be charged");
        assertNull(findBottle(player), "The bottle must not appear");

        String messages = drainMessages(player);
        assertTrue(messages.contains("don't have"), "The player got no denial: " + messages);

    }

    @Test
    @DisplayName("Without an empty bottle the exchange fails")
    void exchangeWithoutEmptyBottleFails() {

        PlayerMock player = operator("Ney");
        player.setLevel(30);
        player.setExp(0.0f);

        player.performCommand("exp");
        click(player, SLOT_TIER_5);

        assertEquals(30, player.getLevel(), "The experience was charged without the empty bottle");
        assertNull(findBottle(player));

    }

    @Test
    @DisplayName("BottleFactory does not produce a bottle above the config cap")
    void factoryClampsLevels() {

        int maxLevels = plugin.getConfigManager().getMaxBottleLevels();
        ItemStack bottle = plugin.getServices().getBottleFactory().create(maxLevels + 500);

        assertEquals(maxLevels, plugin.getServices().getBottleTagService().read(bottle).levels());

    }
}
