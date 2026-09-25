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
 * Навигация и условия меню: возврат между меню, действия закрытия, кастомные действия, требования клика и открытия, права меню.
 */
class MenuNavigationIntegrationTest extends PluginTestHarness {

    @Test
    @DisplayName("[back] returns to the parent menu and closes the window without one")
    void backNavigationBetweenMenus() throws IOException {

        PlayerMock player = operator("Ney");
        writeChildMenu();
        plugin.getServices().reload();

        player.performCommand("exp");
        Menu exchange = findMenu(player);
        assertNotNull(exchange, "The exchange menu did not open");

        assertTrue(plugin.getServices().getMenuService()
                        .openChild(player, "child", exchange, Placeholders.create()),
                "The child menu did not open");

        Menu child = findMenu(player);
        assertNotNull(child);
        assertEquals("child", child.getName());

        // {menu} и {cooldown} раскрыты в текстах предмета, а не остались токенами
        String childLore = loreOf(player, 4);
        assertTrue(childLore.contains("Return to child"),
                "The {menu} placeholder was not resolved in the lore: " + childLore);
        assertTrue(childLore.contains("cd:0"),
                "The {cooldown} placeholder was not resolved in the lore: " + childLore);
        assertFalse(childLore.contains("{menu}") || childLore.contains("{cooldown}"),
                "Tokens are left in the lore: " + childLore);

        click(player, 4, ClickType.LEFT);
        assertEquals("exchange", findMenu(player).getName(), "Returned to the parent menu");

        plugin.getServices().getMenuService().openParent(player);
        assertNull(findMenu(player), "There is no parent - the window closes");

    }

    @Test
    @DisplayName("The close actions run when the menu closes")
    void closeActionsExecute() throws IOException {

        writeMenu("bye", """
                menu:
                  title: "Bye"
                  size: 9
                  close_actions:
                    - "[message] &7menu closed"
                items:
                  deco:
                    type: DECORATION
                    slot: 4
                    material: STONE
                    name: " "
                """);

        plugin.getServices().reload();

        PlayerMock player = operator("Ney");
        player.performCommand("exp bye");

        Menu menu = findMenu(player);
        assertNotNull(menu);

        server.getPluginManager().callEvent(
                new InventoryCloseEvent(player.getOpenInventory()));

        assertTrue(drainMessages(player).contains("menu closed"), "The close actions did not run");

    }

    @Test
    @DisplayName("Click actions run: message, exchange, close, open")
    void customActionsExecute() throws IOException {

        writeMenu("actions", """
                menu:
                  title: "Actions"
                  size: 27
                items:
                  say:
                    type: CUSTOM
                    slot: 10
                    material: PAPER
                    name: "Say"
                    click:
                      - "[message] &aworks {player}"
                  pay:
                    type: TIER
                    slot: 11
                    levels: 3
                    material: EXPERIENCE_BOTTLE
                    name: "Pay"
                  exit:
                    type: CLOSE
                    slot: 12
                    material: BARRIER
                    name: "Exit"
                  jump:
                    type: CUSTOM
                    slot: 13
                    material: ENDER_PEARL
                    name: "Jump"
                    click:
                      - "[open] exchange"
                """);

        plugin.getServices().reload();

        PlayerMock player = operator("Ney");
        player.setLevel(20);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));

        player.performCommand("exp actions");
        assertNotNull(findMenu(player));

        click(player, 10);
        // {player} подставляется как есть (ник латиницей), drainMessages приводит к нижнему регистру
        assertTrue(drainMessages(player).contains("works ney"), "The [message] action did not run");
        assertNotNull(findMenu(player), "The menu closed after [message]");

        click(player, 11);
        assertEquals(17, player.getLevel(), "The [exchange] action did not charge the levels");
        assertNotNull(findBottle(player), "The [exchange] action did not give the bottle");

        click(player, 13);
        assertEquals("exchange", findMenu(player).getName(), "The [open] action did not switch the menu");

        click(player, 40);
        assertNull(findMenu(player), "The [close] action did not close the menu");

    }

    @Test
    @DisplayName("view_requirement hides the item, click_requirement blocks the click")
    void requirementsAreApplied() throws IOException {

        writeMenu("reqs", """
                menu:
                  title: "Requirements"
                  size: 27
                items:
                  hidden:
                    type: CUSTOM
                    slot: 10
                    material: DIAMOND
                    name: "Hidden"
                    view_requirement: "{player_level} >= 1000"
                  shown:
                    type: CUSTOM
                    slot: 11
                    material: GOLD_INGOT
                    name: "Shown"
                    view_requirement: "{player_level} >= 1"
                  blocked:
                    type: CUSTOM
                    slot: 12
                    material: PAPER
                    name: "Blocked"
                    click_requirement: "{player_level} >= 100"
                    denial_message: "&cYou need 100 levels"
                    click:
                      - "[message] &amust not appear"
                """);

        plugin.getServices().reload();

        PlayerMock player = operator("Ney");
        player.setLevel(10);
        player.performCommand("exp reqs");

        Menu menu = findMenu(player);
        assertNotNull(menu);

        Inventory top = menu.getInventory();
        assertTrue(isEmpty(top.getItem(10)), "An item with a failed view_requirement must be absent");
        assertEquals(Material.GOLD_INGOT, top.getItem(11).getType(), "An item with a satisfied condition is not shown");

        click(player, 12);
        String messages = drainMessages(player);
        assertTrue(messages.contains("need 100 levels"), "No denial message: " + messages);
        assertFalse(messages.contains("must not appear"), "The actions ran despite the condition");

    }

    @Test
    @DisplayName("The menu permission and open_requirement are checked before the open")
    void menuPermissionAndOpenRequirement() throws IOException {

        writeMenu("vip", """
                menu:
                  title: "VIP"
                  size: 9
                  permission: "test.vip"
                  open_requirement: "{player_level} >= 10"
                  denial_message: "&cYou need level 10"
                items:
                  deco:
                    type: DECORATION
                    slot: 4
                    material: STONE
                    name: " "
                """);

        plugin.getServices().reload();

        PlayerMock guest = server.addPlayer("Guest");
        guest.setLevel(5);

        // Мок-сервер не подхватывает default: true из plugin.yml — выдаём явно
        guest.addAttachment(plugin, "expbottle.use", true);

        guest.performCommand("exp vip");
        assertNull(findMenu(guest), "The menu opened without the test.vip permission");

        guest.addAttachment(plugin, "test.vip", true);
        guest.performCommand("exp vip");
        assertNull(findMenu(guest), "The menu opened with a failed open_requirement");
        assertTrue(drainMessages(guest).contains("need level 10"));

        guest.setLevel(15);
        guest.performCommand("exp vip");
        assertNotNull(findMenu(guest), "The menu did not open although every condition holds");

    }
}
