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
 * Команды и перезагрузка: старт плагина, чат-обмен, выдача, reload и права ОП при op_bypass: false.
 */
class CommandAndReloadIntegrationTest extends PluginTestHarness {

    @Test
    @DisplayName("The plugin starts, the menu and the exchange buttons are read from menus/")
    void pluginStartsUp() {

        assertNotNull(plugin.getServices());
        assertTrue(plugin.getConfigManager().isEnabled());

        assertEquals(1, plugin.getServices().getMenuRegistry().size(), "The default menu did not load");
        assertTrue(plugin.getServices().getMenuRegistry().byName("exchange").isPresent());
        assertEquals(5, plugin.getBottleRegistry().size());
        assertEquals(5, plugin.getBottleRegistry().byId("tier_5").orElseThrow().levels());

        assertTrue(new File(plugin.getDataFolder(), "menus/exchange.yml").exists(),
                "The default menu file was not created");

    }

    @Test
    @DisplayName("/exp exchange works from chat like the menu button")
    void exchangeCommandWorks() {

        PlayerMock player = operator("Ney");
        player.setLevel(20);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));

        player.performCommand("exp exchange 5");

        assertEquals(15, player.getLevel(), "The chat exchange did not charge the levels");
        assertNotNull(findBottle(player), "The chat exchange did not give the bottle");

        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
        player.performCommand("exp exchange tier_10");
        assertEquals(5, player.getLevel(), "The exchange by the button id did not work");

    }

    @Test
    @DisplayName("The give command creates a bottle for the player")
    void giveCommandCreatesBottle() {

        PlayerMock player = server.addPlayer("Ney");
        PlayerMock admin = operator("Admin");

        admin.performCommand("neyexpbottle give Ney tier_10");

        ItemStack bottle = findBottle(player);
        assertNotNull(bottle, "The bottle was not given");
        assertEquals(10, plugin.getServices().getBottleTagService().read(bottle).levels());

    }

    @Test
    @DisplayName("The reload picks up new menus and keeps everything working")
    void reloadKeepsPluginWorking() throws IOException {

        PlayerMock admin = operator("Admin");
        admin.performCommand("neyexpbottle reload");

        String messages = drainMessages(admin);
        assertTrue(messages.contains("reloaded"), "No reload confirmation: " + messages);
        assertEquals(5, plugin.getBottleRegistry().size());

        writeMenu("second", """
                menu:
                  title: "Second menu"
                  size: 27
                items:
                  button:
                    type: TIER
                    slot: 13
                    levels: 7
                    material: EXPERIENCE_BOTTLE
                """);

        plugin.getServices().reload();

        assertEquals(2, plugin.getServices().getMenuRegistry().size(), "The new menu was not picked up");
        assertEquals(6, plugin.getBottleRegistry().size(), "The new exchange button was not registered");
        assertTrue(plugin.getBottleRegistry().byId("button").isPresent());

        admin.performCommand("exp second");
        assertEquals("second", findMenu(admin).getName());

    }

    @Test
    @DisplayName("op_bypass: false denies an OP player without the permission")
    void opWithoutPermissionIsDeniedWhenOpBypassFalse() {

        assertFalse(plugin.getConfigManager().isOpBypassEnabled(),
                "op_bypass must default to false");

        PlayerMock op = server.addPlayer("Admin");
        op.setOp(true);

        op.performCommand("exp");

        assertNull(findMenu(op), "An OP without expbottle.use must be denied with op_bypass: false");
        assertTrue(drainMessages(op).contains("permission"));

        op.addAttachment(plugin, "expbottle.use", true);
        op.performCommand("exp");
        assertNotNull(findMenu(op), "With an explicit grant the menu must open");

    }
}
