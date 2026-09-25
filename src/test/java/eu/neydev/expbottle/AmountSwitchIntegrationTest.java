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
 * Переключатель количества: циклирование правым кликом, тумблеры, валидность выбора и статус доступности по выбранному количеству.
 */
class AmountSwitchIntegrationTest extends PluginTestHarness {

    @Test
    @DisplayName("RMB in CYCLE mode switches the amount right on the button")
    void rightClickCyclesAmount() {

        PlayerMock player = operator("Ney");
        player.setLevel(20);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 5));

        player.performCommand("exp");

        AmountSelectionService selection = plugin.getServices().getAmountSelectionService();
        assertEquals(1, selection.getSelected(player), "The first option is selected by default");

        click(player, SLOT_TIER_5, ClickType.RIGHT);
        assertEquals(16, selection.getSelected(player), "RMB switched to the second option");
        assertEquals("exchange", findMenu(player).getName(), "In CYCLE mode the window does not change");

        String lore = loreOf(player, SLOT_TIER_5);
        assertTrue(lore.contains("\u00a7a\u00bb \u00a7f16"), "The switcher highlighted the active option: " + lore);
        assertTrue(lore.contains("\u00a77\u00bb 1 "), "The other options are shown as inactive: " + lore);

        // Полный круг: 16 -> 64 -> all -> 1
        click(player, SLOT_TIER_5, ClickType.RIGHT);
        click(player, SLOT_TIER_5, ClickType.RIGHT);
        click(player, SLOT_TIER_5, ClickType.RIGHT);
        assertEquals(1, selection.getSelected(player), "The switch goes in a circle");

        click(player, SLOT_TIER_5, ClickType.LEFT);
        assertEquals(15, player.getLevel(), "The exchange honored the selected amount");

    }

    @Test
    @DisplayName("The right click amount switch obeys the global and per-menu toggles")
    void amountCycleIsConfigurable() throws IOException {

        writeMenu("cycleoff", """
                menu:
                  title: "Cycle off"
                  size: 9
                  cycle_amount: false
                items:
                  tier:
                    type: TIER
                    slot: 4
                    levels: 5
                    material: EXPERIENCE_BOTTLE
                """);

        writeMenu("cycleon", """
                menu:
                  title: "Cycle on"
                  size: 9
                  cycle_amount: true
                items:
                  tier:
                    type: TIER
                    slot: 4
                    levels: 5
                    material: EXPERIENCE_BOTTLE
                """);

        plugin.getServices().reload();

        PlayerMock player = operator("Ney");
        player.setLevel(200);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 64));

        AmountSelectionService amounts = plugin.getServices().getAmountSelectionService();

        // По умолчанию правый клик переключает количество: tier_1 встроенного меню живёт в слоте 20
        player.performCommand("exp open exchange");
        click(player, 20, ClickType.RIGHT);
        assertEquals(16, amounts.getSelected(player), "The default right click cycles the amount");

        // cycle_amount: false выключает переключатель: правый клик становится обычным и обменивает
        player.performCommand("exp open cycleoff");
        click(player, 4, ClickType.RIGHT);
        assertEquals(16, amounts.getSelected(player), "cycle_amount: false must not cycle");
        String cycleoffMessages = drainMessages(player).toLowerCase(Locale.ROOT);
        assertTrue(cycleoffMessages.contains("you exchanged"),
                "The right click must run the exchange action when cycling is off: " + cycleoffMessages);

        // Глобальный тумблер выключен: встроенное меню тоже не переключает
        setConfigValue("cycle_on_right_click: true", "cycle_on_right_click: false");
        plugin.getServices().reload();
        assertFalse(plugin.getServices().getConfigManager().getAmount().cycleOnRightClick());

        player.performCommand("exp open exchange");
        click(player, 20, ClickType.RIGHT);
        assertEquals(16, amounts.getSelected(player), "The global switch off stops the bundled menu");

        // cycle_amount: true переопределяет глобальный тумблер
        player.performCommand("exp open cycleon");
        click(player, 4, ClickType.RIGHT);
        assertEquals(64, amounts.getSelected(player), "cycle_amount: true overrides the global switch");

    }

    @Test
    @DisplayName("A disabled switch hides the switcher list and the right click hint from the lore")
    void disabledCycleHidesSwitcherLore() throws IOException {

        writeMenu("lorecycle", """
                menu:
                  title: "Lore cycle"
                  size: 9
                items:
                  tier:
                    type: TIER
                    slot: 4
                    levels: 5
                    material: EXPERIENCE_BOTTLE
                    lore:
                      - "price line stays"
                      - "{amount_options}"
                      - "{amount_hint}"
                """);

        plugin.getServices().reload();
        PlayerMock player = operator("Ney");

        player.performCommand("exp open lorecycle");
        String enabledLore = loreOf(player, 4);
        assertTrue(enabledLore.contains("bottle(s)"), "The switcher list is drawn while cycling is on");
        assertTrue(enabledLore.contains("switch the amount"), "The hint is drawn while cycling is on");

        // Выключаем переключатель только в этом меню: список и подсказка исчезают
        File folder = new File(plugin.getDataFolder(), "menus");
        Files.writeString(new File(folder, "lorecycle.yml").toPath(), """
                menu:
                  title: "Lore cycle"
                  size: 9
                  cycle_amount: false
                items:
                  tier:
                    type: TIER
                    slot: 4
                    levels: 5
                    material: EXPERIENCE_BOTTLE
                    lore:
                      - "price line stays"
                      - "{amount_options}"
                      - "{amount_hint}"
                """, StandardCharsets.UTF_8);
        plugin.getServices().reload();

        player.performCommand("exp open lorecycle");
        String disabledLore = loreOf(player, 4);
        assertFalse(disabledLore.contains("bottle(s)"), "The switcher list must disappear with the switch");
        assertFalse(disabledLore.contains("switch the amount"), "The hint must disappear with the switch");
        assertTrue(disabledLore.contains("price line stays"), "The rest of the lore survives");

    }

    @Test
    @DisplayName("The amount selection ignores unknown options and resets on close")
    void amountSelectionValidationAndCleanup() {

        PlayerMock player = operator("Ney");
        AmountSelectionService selection = plugin.getServices().getAmountSelectionService();

        selection.select(player, "64");
        assertEquals(64, selection.getSelected(player));

        selection.select(player, "32");
        assertEquals(64, selection.getSelected(player), "The option is missing from the config - the selection did not change");

        server.getPluginManager().callEvent(new PlayerQuitEvent(player, null));
        assertEquals(1, selection.getSelected(player), "Closing resets the selection");

    }

    @Test
    @DisplayName("The availability status follows the selected amount, not a single bottle")
    void availabilityFollowsSelectedAmount() {

        PlayerMock player = operator("Ney");
        player.setLevel(30);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 64));
        player.performCommand("exp");

        String first = loreOf(player, SLOT_TIER_5);
        assertTrue(first.contains("✔"), "One 5-level bottle is affordable for a player with 30 levels: " + first);

        click(player, SLOT_TIER_5, ClickType.RIGHT);
        assertEquals(16, plugin.getServices().getAmountSelectionService().getSelected(player),
                "RMB set the second amount option");

        String lore = loreOf(player, SLOT_TIER_5);
        assertTrue(lore.contains("✖"),
                "16 bottles of 5 levels (80) are not affordable for a player with 30 levels, the status must change: " + lore);

        click(player, SLOT_TIER_5, ClickType.RIGHT);
        click(player, SLOT_TIER_5, ClickType.RIGHT);
        click(player, SLOT_TIER_5, ClickType.RIGHT);
        assertEquals(1, plugin.getServices().getAmountSelectionService().getSelected(player),
                "A full circle of the switch returned the first option");

        String back = loreOf(player, SLOT_TIER_5);
        assertTrue(back.contains("✔"), "With the amount back to 1 the status is available again: " + back);

    }
}
