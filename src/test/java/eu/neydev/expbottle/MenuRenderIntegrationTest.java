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
 * Открытие и отрисовка меню: команды, слоты, плейсхолдеры, выравнивание переключателя, автообновление, чары и головы.
 */
class MenuRenderIntegrationTest extends PluginTestHarness {

    @Test
    @DisplayName("/exp opens the menu with our holder")
    void commandOpensMenu() {

        PlayerMock player = operator("Ney");
        player.performCommand("exp");

        Menu menu = findMenu(player);

        assertNotNull(menu, "The menu did not open");
        assertEquals("exchange", menu.getName());
        assertEquals(45, menu.getInventory().getSize(), "Five rows: header, padding, buttons, padding, footer");
        assertNotNull(menu.getInventory().getItem(SLOT_TIER_5), "The exchange button slot has no item");

    }

    @Test
    @DisplayName("Menu items cannot be pulled out with a click")
    void menuItemsCannotBeTaken() {

        PlayerMock player = operator("Ney");
        player.performCommand("exp");

        InventoryClickEvent event = click(player, SLOT_INFO);

        assertTrue(event.isCancelled(), "The menu click was not cancelled");
        assertFalse(player.getInventory().contains(Material.EXPERIENCE_BOTTLE));

    }

    @Test
    @DisplayName("Menu placeholders are fully substituted, including the nested ones from the config")
    void menuPlaceholdersAreResolved() {

        PlayerMock player = operator("Ney");
        player.setLevel(30);
        player.performCommand("exp");

        Inventory top = player.getOpenInventory().getTopInventory();

        for (ItemStack item : top.getContents()) {

            if (item == null || item.getItemMeta() == null) {
                continue;
            }

            ItemMeta meta = item.getItemMeta();
            List<String> lines = new ArrayList<>();

            if (meta.hasDisplayName()) {
                lines.add(meta.getDisplayName());
            }

            if (meta.hasLore()) {
                lines.addAll(meta.getLore());
            }

            for (String line : lines) {
                for (String key : MENU_PLACEHOLDERS) {
                    assertFalse(line.contains("{" + key + "}"),
                            "Placeholder {" + key + "} not substituted: " + line);
                }
            }

        }

        // Подсказка под переключателем приходит из конфига и сама содержит {amount_label}
        String lore = loreOf(player, SLOT_TIER_5);
        assertTrue(lore.contains("\u00a77switch the amount \u00a78(\u00a7f1\u00a78)"),
                "The nested hint placeholder was not resolved: " + lore);

        click(player, SLOT_TIER_5, ClickType.RIGHT);
        lore = loreOf(player, SLOT_TIER_5);
        assertTrue(lore.contains("\u00a77switch the amount \u00a78(\u00a7f16\u00a78)"),
                "The hint did not follow the amount switch: " + lore);

    }

    @Test
    @DisplayName("The availability and switcher lines align with Costs: by a leading space, the RMB hint stays flush left")
    void amountLinesAreAlignedWithCosts() {

        PlayerMock player = operator("Ney");
        player.setLevel(30);
        player.performCommand("exp");

        List<String> lore = loreLines(player, SLOT_TIER_5);
        assertFalse(lore.isEmpty(), "The exchange button has no lore");

        for (String line : lore) {

            boolean aligned = line.contains("Costs:") || line.contains("»")
                    || line.contains("✔") || line.contains("✖");

            if (aligned) {
                assertTrue(line.startsWith(" "),
                        "The line must start with a space to align with Costs: " + line);
            }

            if (line.contains("switch the amount")) {
                assertFalse(line.startsWith(" "),
                        "The RMB hint stays flush left: " + line);
            }

        }

    }

    @Test
    @DisplayName("The RMB hint is separated from the amount switcher by a blank line")
    void hintIsSeparatedFromSwitcherByBlankLine() {

        PlayerMock player = operator("Ney");
        player.performCommand("exp");

        List<String> lore = loreLines(player, SLOT_TIER_5);

        int hint = -1;

        for (int index = 0; index < lore.size(); index++) {

            if (lore.get(index).contains("switch the amount")) {
                hint = index;
            }

        }

        assertTrue(hint > 1, "The button lore has no RMB hint: " + lore);
        assertTrue(lore.get(hint - 1).isEmpty(),
                "A blank line is required between the switcher and the RMB hint: " + lore);
        assertTrue(lore.get(hint - 2).contains("»"),
                "The last switcher line must sit above the blank line: " + lore);

    }

    @Test
    @DisplayName("update_interval redraws dynamic items on its own")
    void updateIntervalRefreshesMenu() throws IOException {

        writeMenu("live", """
                menu:
                  title: "Live menu"
                  size: 27
                  update_interval: 1
                items:
                  info:
                    type: INFO
                    slot: 4
                    material: BOOK
                    name: "Level: {player_level}"
                """);

        plugin.getServices().reload();

        PlayerMock player = operator("Ney");
        player.setLevel(3);
        player.performCommand("exp live");

        Menu menu = findMenu(player);
        assertNotNull(menu);
        assertTrue(loreAndName(menu.getInventory().getItem(4)).contains("Level: 3"));

        player.setLevel(42);
        server.getScheduler().performTicks(3);

        assertTrue(loreAndName(menu.getInventory().getItem(4)).contains("Level: 42"),
                "The menu did not refresh by update_interval");

    }

    @Test
    @DisplayName("Menu items support hidden enchants and player heads")
    void menuItemsSupportEnchantmentsAndHeads() throws IOException {

        writeMenu("gear", """
                menu:
                  title: "Gear"
                  size: 27
                items:
                  sword:
                    type: CUSTOM
                    slot: 10
                    material: DIAMOND_SWORD
                    name: "Enchanted sword"
                    hide_enchantments: true
                    enchantments:
                      - "DURABILITY:3"
                  head:
                    type: DECORATION
                    slot: 12
                    material: PLAYER_HEAD
                    name: "Owner head"
                    skull_owner: Ney
                """);

        plugin.getServices().reload();

        // Имена зачарований разрешаются во время разбора файла меню,
        // поэтому мок-реестр должен существовать до перезагрузки
        registerMockEnchantments();
        plugin.getServices().reload();

        PlayerMock player = operator("Ney");
        player.performCommand("exp gear");

        Menu menu = findMenu(player);
        assertNotNull(menu);

        ItemStack sword = menu.getInventory().getItem(10);
        assertNotNull(sword);
        assertFalse(sword.getEnchantments().isEmpty(), "The enchantment did not apply");
        assertTrue(sword.getItemMeta().hasItemFlag(ItemFlag.HIDE_ENCHANTS),
                "The enchantment info must be hidden");

        ItemStack head = menu.getInventory().getItem(12);
        assertNotNull(head);
        assertEquals(Material.PLAYER_HEAD, head.getType());

    }

    @Test
    @DisplayName("An unknown menu reports the name and opens nothing")
    void unknownMenuIsReported() {

        PlayerMock player = operator("Ney");
        player.performCommand("exp nosuchmenu");

        assertNull(findMenu(player));
        assertTrue(drainMessages(player).contains("not found"));

    }

    @Test
    @DisplayName("Without expbottle.use the menu does not open")
    void playerWithoutPermissionCannotOpenMenu() {

        PlayerMock player = server.addPlayer("Guest");
        player.addAttachment(plugin, "expbottle.use", false);

        player.performCommand("exp");

        assertNull(findMenu(player), "The menu opened without the permission");
        assertTrue(drainMessages(player).contains("permission"), "The player got no permission message");

    }
}
