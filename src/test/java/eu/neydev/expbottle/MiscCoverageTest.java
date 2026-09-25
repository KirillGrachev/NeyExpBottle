package eu.neydev.expbottle;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import eu.neydev.expbottle.event.BottleThrowHandler;
import eu.neydev.expbottle.event.BottleUseEvent;
import eu.neydev.expbottle.gui.Menu;
import eu.neydev.expbottle.gui.MenuHolder;
import eu.neydev.expbottle.gui.MenuLayout;
import eu.neydev.expbottle.gui.item.MenuItem;
import eu.neydev.expbottle.gui.item.MenuItemType;
import eu.neydev.expbottle.listener.AntiDupeListener;
import eu.neydev.expbottle.listener.MenuListener;
import eu.neydev.expbottle.service.PluginServices;
import eu.neydev.expbottle.support.ExpBottleExpansion;
import eu.neydev.expbottle.support.PlaceholderBridge;
import eu.neydev.expbottle.support.PlaceholderSupport;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Покрытие событий, GUI, support-слоя, реестров и конфигурации:
 * ветки броска, кликов меню, декораций, ошибок разметки и дефолтов конфига.
 */
class MiscCoverageTest {

    private ServerMock server;
    private NeyExpBottle plugin;
    private PluginServices services;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(NeyExpBottle.class);
        services = plugin.getServices();
    }

    @AfterEach
    void tearDown() {
        server.getScheduler().cancelTasks(plugin);
        MockBukkit.unmock();
    }

    private PlayerMock granted(String name) {

        PlayerMock player = server.addPlayer(name);

        for (String permission : List.of("expbottle.use", "expbottle.exchange", "expbottle.admin")) {
            player.addAttachment(plugin, permission, true);
        }

        return player;

    }

    private void setConfig(String from, String to) throws IOException {

        File file = new File(plugin.getDataFolder(), "config.yml");
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        assertTrue(content.contains(from), "config.yml is missing '" + from + "'");
        Files.writeString(file.toPath(), content.replace(from, to), StandardCharsets.UTF_8);
        services.reload();

    }

    private void writeMenu(String name, String yaml) throws IOException {

        File folder = new File(plugin.getDataFolder(), "menus");
        assertTrue(folder.exists() || folder.mkdirs());

        Files.writeString(new File(folder, name + ".yml").toPath(), yaml, StandardCharsets.UTF_8);

    }

    private String drain(PlayerMock player) {

        StringBuilder builder = new StringBuilder();
        String message;

        while ((message = player.nextMessage()) != null) {
            builder.append(message.toLowerCase(Locale.ROOT)).append(' ');
        }

        return builder.toString();

    }

    private PlayerInteractEvent interact(PlayerMock player, ItemStack item, Action action,
                                         EquipmentSlot hand) {

        PlayerInteractEvent event = new PlayerInteractEvent(player, action, item, null, BlockFace.SELF, hand);
        server.getPluginManager().callEvent(event);
        return event;

    }

    private ItemStack forgedBottle(int levels) {

        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(
                services.getBottleTagService().getLevelsKey(), PersistentDataType.INTEGER, levels);
        item.setItemMeta(meta);
        return item;

    }

    @Test
    @DisplayName("Throw: foreign items, the left button and the off hand are ignored")
    void throwAttemptIgnoredCases() {

        PlayerMock player = granted("Ney");
        BottleThrowHandler handler = new BottleThrowHandler(services);

        handler.onThrowAttempt(interact(player, new ItemStack(Material.STONE), Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));
        handler.onThrowAttempt(interact(player, null, Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));
        handler.onThrowAttempt(interact(player, services.getBottleFactory().create(5), Action.LEFT_CLICK_AIR, EquipmentSlot.HAND));
        handler.onThrowAttempt(interact(player, services.getBottleFactory().create(5), Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND));

        assertEquals(0, drain(player).length(), "Nothing must happen");

    }

    @Test
    @DisplayName("Throw: a forged and a damaged bottle are rejected, a valid one flies")
    void throwAttemptVerdicts() {

        PlayerMock player = granted("Ney");
        BottleThrowHandler handler = new BottleThrowHandler(services);

        PlayerInteractEvent forged = interact(player, forgedBottle(500), Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(forged.isCancelled(), "A forgery is not thrown");
        assertEquals(org.bukkit.event.Event.Result.DENY, forged.useItemInHand(), "The item in the hand is swallowed");
        assertTrue(drain(player).contains("not created by the server"));

        int max = services.getConfigManager().getMaxBottleLevels();
        ItemStack signed = services.getBottleTagService().tag(new ItemStack(Material.EXPERIENCE_BOTTLE), max + 1);
        PlayerInteractEvent broken = interact(player, signed, Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(broken.isCancelled(), "A damaged bottle is not thrown");
        assertEquals(org.bukkit.event.Event.Result.DENY, broken.useItemInHand(), "The item in the hand is swallowed");
        assertTrue(drain(player).contains("damaged"));

        PlayerInteractEvent valid = interact(player, services.getBottleFactory().create(5),
                Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertNotEquals(org.bukkit.event.Event.Result.DENY, valid.useItemInHand(),
                "A valid bottle is thrown");

    }

    @Test
    @DisplayName("Throw: a non-projectile material swallows the click, an external cancel is respected")
    void throwAttemptWrongMaterialAndExternalCancel() throws IOException {

        // Ванильный снаряд существует только у EXPERIENCE_BOTTLE: с другим
        // материалом предмета из конфига клик в обычном режиме гасится кодом
        setConfig("material: EXPERIENCE_BOTTLE", "material: GLASS_BOTTLE");

        PlayerMock player = granted("Ney");

        PlayerInteractEvent wrongMaterial = interact(player, services.getBottleFactory().create(5),
                Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(wrongMaterial.isCancelled(), "A non-projectile material cannot be thrown");
        assertEquals(org.bukkit.event.Event.Result.DENY, wrongMaterial.useItemInHand(), "The item in the hand is swallowed");

        setConfig("material: GLASS_BOTTLE", "material: EXPERIENCE_BOTTLE");

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onUse(BottleUseEvent event) {
                event.setCancelled(true);
            }
        }, plugin);

        PlayerInteractEvent cancelled = interact(player, services.getBottleFactory().create(5),
                Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(cancelled.isCancelled(), "An external BottleUseEvent cancel swallows the throw");
        assertEquals(org.bukkit.event.Event.Result.DENY, cancelled.useItemInHand(), "The item in the hand is swallowed");

    }

    @Test
    @DisplayName("Safe mode: the throw uses the bottle at once")
    void safeModeUsesBottleOnThrow() throws IOException {

        setConfig("safe_mode: false", "safe_mode: true");

        PlayerMock player = granted("Ney");
        player.setLevel(30);

        ItemStack bottle = services.getBottleFactory().create(5);
        player.getInventory().setItemInMainHand(bottle);

        PlayerInteractEvent single = interact(player, bottle, Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);

        assertTrue(single.isCancelled(), "In safe mode the vanilla throw is replaced by our own");
        assertEquals(35, player.getLevel(), "The thrower got the levels right away");

        ItemStack left = player.getInventory().getItemInMainHand();
        assertTrue(left == null || left.getType() == Material.AIR, "The bottle was consumed");

        ItemStack stack = services.getBottleFactory().create(5);
        stack.setAmount(3);
        player.getInventory().setItemInMainHand(stack);

        interact(player, stack, Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);

        assertEquals(40, player.getLevel(), "The second throw used the bottle again");
        assertEquals(2, player.getInventory().getItemInMainHand().getAmount(), "One bottle left the stack");

    }

    @Test
    @DisplayName("Safe mode: an external cancel keeps the bottle in the hand")
    void safeModeRespectsExternalCancel() throws IOException {

        setConfig("safe_mode: false", "safe_mode: true");

        PlayerMock player = granted("Ney");
        player.setLevel(30);

        ItemStack bottle = services.getBottleFactory().create(5);
        player.getInventory().setItemInMainHand(bottle);

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onUse(BottleUseEvent event) {
                event.setCancelled(true);
            }
        }, plugin);

        PlayerInteractEvent event = interact(player, bottle, Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);

        assertTrue(event.isCancelled(), "The cancelled use swallows the throw");
        assertEquals(30, player.getLevel(), "Nothing was granted");
        assertEquals(Material.EXPERIENCE_BOTTLE, player.getInventory().getItemInMainHand().getType(),
                "The bottle stayed in the hand");

    }

    @Test
    @DisplayName("The spent projectile gives nothing, a vanilla bottle keeps its own reward")
    void breakExperienceVerdicts() {

        PlayerMock player = granted("Ney");
        player.setLevel(30);
        BottleThrowHandler handler = new BottleThrowHandler(services);

        ItemStack spent = services.getBottleTagService().markSpent(new ItemStack(Material.EXPERIENCE_BOTTLE));
        assertEquals(0, handler.breakExperience(spent, player.getLocation(), player, 7),
                "The spent safe-mode projectile gives no reward on break");

        ItemStack vanilla = new ItemStack(Material.EXPERIENCE_BOTTLE);
        assertEquals(7, handler.breakExperience(vanilla, player.getLocation(), player, 7),
                "A vanilla bottle keeps its own reward");

        ItemStack bottle = services.getBottleFactory().create(5);
        assertEquals(0, handler.breakExperience(bottle, player.getLocation(), player, 7),
                "Our bottle replaces the reward with the stored levels");
        assertEquals(35, player.getLevel(), "The receiver next to the break got the levels");

    }

    @Test
    @DisplayName("Menu clicks: decoration and shift are ignored, a denial without a message uses the default")
    void menuClickBranches() throws IOException {

        writeMenu("clicks", """
                menu:
                  title: "Clicks"
                  size: 27
                items:
                  deco:
                    type: DECORATION
                    slot: 10
                    material: STONE
                    name: " "
                  blocked:
                    type: CUSTOM
                    slot: 11
                    material: PAPER
                    name: "Blocked"
                    click_requirement: "{player_level} >= 100"
                    click:
                      - "[message] &amust not appear"
                """);

        services.reload();

        PlayerMock player = granted("Ney");
        player.performCommand("exp clicks");

        InventoryView view = player.getOpenInventory();

        server.getPluginManager().callEvent(new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 10, ClickType.LEFT, InventoryAction.PICKUP_ALL));
        assertEquals(0, drain(player).length(), "A decoration stays silent");

        server.getPluginManager().callEvent(new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 11, ClickType.SHIFT_LEFT, InventoryAction.PICKUP_ALL));
        assertEquals(0, drain(player).length(), "A shift click is ignored");

        server.getPluginManager().callEvent(new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 11, ClickType.LEFT, InventoryAction.PICKUP_ALL));
        assertTrue(drain(player).contains("permission"), "An empty denial_message falls back to the default denial");

    }

    @Test
    @DisplayName("Drag and menu close are handled by the listener")
    void menuDragAndClose() throws IOException {

        writeMenu("drag", """
                menu:
                  title: "Drag"
                  size: 9
                  close_actions:
                    - "[message] &7closed"
                items:
                  deco:
                    type: DECORATION
                    slot: 4
                    material: STONE
                    name: " "
                """);

        services.reload();

        PlayerMock player = granted("Ney");
        player.performCommand("exp drag");

        InventoryView view = player.getOpenInventory();

        InventoryDragEvent drag = new InventoryDragEvent(
                view, new ItemStack(Material.STONE), new ItemStack(Material.AIR), false,
                Map.of(0, new ItemStack(Material.STONE)));
        server.getPluginManager().callEvent(drag);
        assertTrue(drag.isCancelled(), "A drag in the menu is cancelled");

        server.getPluginManager().callEvent(new InventoryCloseEvent(view));
        assertTrue(drain(player).contains("closed"), "The close actions ran in the listener");

        // Закрытие без меню ничего не делает
        server.getPluginManager().callEvent(new InventoryCloseEvent(player.getOpenInventory()));

    }

    @Test
    @DisplayName("Anti dupe: foreign items and the disabled setting are not cancelled")
    void antiDupeBranches() {

        AntiDupeListener listener = new AntiDupeListener(plugin);
        PlayerMock player = granted("Ney");
        player.performCommand("exp");

        InventoryView view = player.getOpenInventory();

        InventoryCreativeEvent foreign = new InventoryCreativeEvent(
                view, InventoryType.SlotType.CONTAINER, 10, new ItemStack(Material.STONE));
        listener.onCreativeClone(foreign);
        assertFalse(foreign.isCancelled(), "A foreign item is left alone");

        InventoryCreativeEvent vanilla = new InventoryCreativeEvent(
                view, InventoryType.SlotType.CONTAINER, 11, new ItemStack(Material.EXPERIENCE_BOTTLE));
        listener.onCreativeClone(vanilla);
        assertFalse(vanilla.isCancelled(), "A vanilla experience bottle clones as usual");

    }

    @Test
    @DisplayName("A vanilla bottle: the throw is not swallowed, the reward is not replaced")
    void vanillaBottleIsUntouched() {

        PlayerMock player = granted("Ney");
        BottleThrowHandler handler = new BottleThrowHandler(services);

        ItemStack vanilla = new ItemStack(Material.EXPERIENCE_BOTTLE);

        // У клика в воздух useInteractedBlock равен DENY всегда, поэтому
        // проверяем именно предмет в руке: его гасим только мы.
        PlayerInteractEvent event = interact(player, vanilla, Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertNotEquals(org.bukkit.event.Event.Result.DENY, event.useItemInHand(), "A plain bottle is not swallowed");
        assertEquals(0, drain(player).length(), "The player is told nothing");

        int before = player.getLevel();
        assertEquals(0, handler.handleBreak(vanilla, player.getLocation(), player),
                "The signature and the reward do not apply to a foreign item");
        assertEquals(before, player.getLevel(), "The player levels do not change");
        assertEquals(0, drain(player).length(), "There are no security warnings");

    }

    @Test
    @DisplayName("GUI: the holder before init, menu getters and the layout with priorities")
    void guiInternals() {

        MenuHolder fresh = new MenuHolder();
        assertNull(fresh.getInventory(), "There is no inventory before init");

        PlayerMock player = granted("Ney");
        player.performCommand("exp");

        Menu menu = services.getMenuService().findMenu(player);
        assertNotNull(menu);
        assertEquals(player, menu.getPlayer());
        assertNotNull(menu.getHolder());
        assertNotNull(menu.getInventory());
        assertEquals("exchange", menu.getName());

        menu.refresh();
        menu.onClose();

        MenuItem low = MenuItem.builder("low").type(MenuItemType.DECORATION)
                .material(Material.STONE).priority(0).slots(List.of(5)).build();
        MenuItem high = MenuItem.builder("high").type(MenuItemType.DECORATION)
                .material(Material.PAPER).priority(5).slots(List.of(5)).build();

        MenuLayout layout = MenuLayout.of(List.of(low, high));
        assertEquals("high", layout.getItem(5).getId(), "A high priority overrides a low one");
        assertNull(layout.getItem(6));

    }

    @Test
    @DisplayName("PlaceholderAPI without the plugin installed does not break startup")
    void placeholderSupportWithoutApi() {

        PlaceholderSupport.registerIfPresent(plugin);

        PlaceholderBridge bridge = new PlaceholderBridge();
        assertEquals("without percents", bridge.process(granted("Ney"), "without percents"));

        ExpBottleExpansion expansion = new ExpBottleExpansion(plugin);

        assertEquals("expbottle", expansion.getIdentifier());
        assertNotNull(expansion.getAuthor());
        assertNotNull(expansion.getVersion());
        assertTrue(expansion.persist());

        PlayerMock player = granted("Ney");
        player.setLevel(9);

        assertEquals("9", expansion.onRequest(player, "level"));
        assertNotNull(expansion.onRequest(player, "exp"));
        assertNotNull(expansion.onRequest(player, "progress"));
        assertNotNull(expansion.onRequest(player, "levels"));
        assertNotNull(expansion.onRequest(player, "created"));
        assertNotNull(expansion.onRequest(player, "used"));
        assertNotNull(expansion.onRequest(player, "denied"));
        assertNotNull(expansion.onRequest(player, "tiers"));
        assertNotNull(expansion.onRequest(player, "version"));
        assertNull(expansion.onRequest(player, "unknown_key"));
        assertNotNull(expansion.onRequest(null, "level"));

    }

    @Test
    @DisplayName("The menu registry survives broken files and duplicates")
    void menuRegistryBadFiles() throws IOException {

        writeMenu("broken", """
                items:
                  deco:
                    type: DECORATION
                    slot: 4
                    material: STONE
                """);

        writeMenu("empty_menu", """
                menu:
                  title: "Empty"
                  size: 7
                """);

        writeMenu("mess", """
                menu:
                  title: "Mess"
                  size: 27
                items:
                  not_a_section: "line"
                  zero:
                    type: TIER
                    slot: 10
                    levels: 0
                    material: EXPERIENCE_BOTTLE
                  duplicate:
                    type: TIER
                    slot: 11
                    levels: 5
                    material: EXPERIENCE_BOTTLE
                """);

        writeMenu("duplicate_id", """
                menu:
                  title: "Dup"
                  size: 27
                items:
                  duplicate:
                    type: TIER
                    slot: 12
                    levels: 7
                    material: EXPERIENCE_BOTTLE
                """);

        services.reload();

        assertTrue(services.getMenuRegistry().byName("broken").isEmpty(), "A file without the menu section does not load");
        assertTrue(services.getMenuRegistry().byName("mess").isPresent(), "A partially broken menu loads");
        assertTrue(services.getBottleRegistry().byId("zero").isEmpty(), "A button with levels 0 is dropped");
        assertTrue(services.getBottleRegistry().byId("duplicate").isPresent());
        assertEquals(7, services.getBottleRegistry().byId("duplicate").orElseThrow().levels(),
                "A duplicate id from another menu does not overwrite the first one (menus go alphabetically)");

        assertNotNull(services.getMenuRegistry().getNames());
        assertTrue(services.getMenuRegistry().size() >= 1);

    }

    @Test
    @DisplayName("Config: missing keys fall back to defaults, broken values do not break the load")
    void configDefaultsAndGarbage() throws IOException {

        File file = new File(plugin.getDataFolder(), "config.yml");

        Files.writeString(file.toPath(), """
                settings:
                  bottle:
                    material: NO_SUCH
                  menu:
                    default: ""
                messages:
                  only_players:
                    text:
                      - "text without enabled"
                """, StandardCharsets.UTF_8);

        services.reload();

        assertEquals(Material.EXPERIENCE_BOTTLE, services.getConfigManager().getBottleMaterial());
        assertTrue(services.getConfigManager().isEnabled(), "A missing key equals the default");
        assertTrue(services.getConfigManager().getMessage(eu.neydev.expbottle.config.type.MessageKey.ONLY_PLAYERS).size() == 1);
        assertFalse(services.getConfigManager().isMessageEnabled(eu.neydev.expbottle.config.type.MessageKey.NO_PERMISSION),
                "A missing message is disabled");

        Files.writeString(file.toPath(), "", StandardCharsets.UTF_8);
        services.reload();

        assertTrue(services.getConfigManager().isEnabled(), "An empty config equals the defaults");
        assertNotNull(services.getConfigManager().getDefaultMenu());
        assertFalse(services.getConfigManager().isSafeMode(), "Safe mode is off by default");

    }

    @Test
    @DisplayName("The listeners register and work through the dispatcher")
    void listenersWired() {

        MenuListener listener = new MenuListener(plugin);
        assertNotNull(listener, "The menu listener is created");

        PlayerMock player = granted("Ney");
        player.performCommand("exp");

        InventoryView view = player.getOpenInventory();

        InventoryClickEvent event = new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 4, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        listener.onMenuClick(event);
        assertTrue(event.isCancelled(), "The menu click was cancelled by the listener");

    }
}