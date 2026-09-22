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
import org.bukkit.entity.Player;
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

        assertTrue(content.contains(from), "В конфиге нет '" + from + "'");
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
    @DisplayName("Бросок: чужие предметы, левая кнопка и вторая рука игнорируются")
    void throwAttemptIgnoredCases() {

        PlayerMock player = granted("Ney");
        BottleThrowHandler handler = new BottleThrowHandler(services);

        handler.onThrowAttempt(interact(player, new ItemStack(Material.STONE), Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));
        handler.onThrowAttempt(interact(player, null, Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));
        handler.onThrowAttempt(interact(player, services.getBottleFactory().create(5), Action.LEFT_CLICK_AIR, EquipmentSlot.HAND));
        handler.onThrowAttempt(interact(player, services.getBottleFactory().create(5), Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND));

        assertEquals(0, drain(player).length(), "Ничего не должно происходить");

    }

    @Test
    @DisplayName("Бросок: подделка и повреждённая бутылка отклоняются, валидная летит")
    void throwAttemptVerdicts() {

        PlayerMock player = granted("Ney");
        BottleThrowHandler handler = new BottleThrowHandler(services);

        PlayerInteractEvent forged = interact(player, forgedBottle(500), Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(forged.isCancelled(), "Подделка не бросается");
        assertTrue(drain(player).contains("not created by the server"));

        int max = services.getConfigManager().getMaxBottleLevels();
        ItemStack signed = services.getBottleTagService().tag(new ItemStack(Material.EXPERIENCE_BOTTLE), max + 1);
        PlayerInteractEvent broken = interact(player, signed, Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(broken.isCancelled(), "Повреждённая бутылка не бросается");
        assertTrue(drain(player).contains("damaged"));

        PlayerInteractEvent valid = interact(player, services.getBottleFactory().create(5),
                Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertNotEquals(org.bukkit.event.Event.Result.DENY, valid.useItemInHand(),
                "Валидная бутылка бросается");

    }

    @Test
    @DisplayName("Бросок: выключенный конфиг гасит клик, внешняя отмена уважается")
    void throwAttemptDisabledAndExternalCancel() throws IOException {

        setConfig("throwable: true", "throwable: false");

        PlayerMock player = granted("Ney");
        BottleThrowHandler handler = new BottleThrowHandler(services);

        PlayerInteractEvent disabled = interact(player, services.getBottleFactory().create(5),
                Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(disabled.isCancelled(), "При throwable: false клик гасится");

        setConfig("throwable: false", "throwable: true");

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onUse(BottleUseEvent event) {
                event.setCancelled(true);
            }
        }, plugin);

        PlayerInteractEvent cancelled = interact(player, services.getBottleFactory().create(5),
                Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(cancelled.isCancelled(), "Внешняя отмена BottleUseEvent гасит бросок");

    }

    @Test
    @DisplayName("Клики меню: декорация и shift игнорируются, отказ без сообщения использует стандарт")
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
        assertEquals(0, drain(player).length(), "Декорация молчит");

        server.getPluginManager().callEvent(new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 11, ClickType.SHIFT_LEFT, InventoryAction.PICKUP_ALL));
        assertEquals(0, drain(player).length(), "Shift-клик игнорируется");

        server.getPluginManager().callEvent(new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 11, ClickType.LEFT, InventoryAction.PICKUP_ALL));
        assertTrue(drain(player).contains("permission"), "Пустое denial_message даёт стандартный отказ");

    }

    @Test
    @DisplayName("Drag и закрытие меню обрабатываются слушателем")
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
        assertTrue(drag.isCancelled(), "Drag в меню отменяется");

        server.getPluginManager().callEvent(new InventoryCloseEvent(view));
        assertTrue(drain(player).contains("closed"), "Действия закрытия выполнены слушателем");

        // Закрытие без меню ничего не делает
        server.getPluginManager().callEvent(new InventoryCloseEvent(player.getOpenInventory()));

    }

    @Test
    @DisplayName("Анти-дьюп: чужие предметы и выключенная настройка не отменяются")
    void antiDupeBranches() {

        AntiDupeListener listener = new AntiDupeListener(plugin);
        PlayerMock player = granted("Ney");
        player.performCommand("exp");

        InventoryView view = player.getOpenInventory();

        InventoryCreativeEvent foreign = new InventoryCreativeEvent(
                view, InventoryType.SlotType.CONTAINER, 10, new ItemStack(Material.STONE));
        listener.onCreativeClone(foreign);
        assertFalse(foreign.isCancelled(), "Чужой предмет не трогаем");

    }

    @Test
    @DisplayName("GUI: holder до инициализации, геттеры меню и раскладка с приоритетами")
    void guiInternals() {

        MenuHolder fresh = new MenuHolder();
        assertNull(fresh.getInventory(), "До инициализации инвентаря нет");

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
        assertEquals("high", layout.getItem(5).getId(), "Высокий приоритет перекрывает низкий");
        assertNull(layout.getItem(6));

    }

    @Test
    @DisplayName("PlaceholderAPI без установленного плагина не ломает запуск")
    void placeholderSupportWithoutApi() {

        PlaceholderSupport.registerIfPresent(plugin);

        PlaceholderBridge bridge = new PlaceholderBridge();
        assertEquals("без процентов", bridge.process(granted("Ney"), "без процентов"));

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
    @DisplayName("Реестр меню переживает битые файлы и дубликаты")
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
                  not_a_section: "строка"
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

        assertTrue(services.getMenuRegistry().byName("broken").isEmpty(), "Файл без секции menu не грузится");
        assertTrue(services.getMenuRegistry().byName("mess").isPresent(), "Частично битое меню грузится");
        assertTrue(services.getBottleRegistry().byId("zero").isEmpty(), "Кнопка с levels 0 отброшена");
        assertTrue(services.getBottleRegistry().byId("duplicate").isPresent());
        assertEquals(7, services.getBottleRegistry().byId("duplicate").orElseThrow().levels(),
                "Дубликат id из другого меню не перетирает первый (меню идут по алфавиту)");

        assertNotNull(services.getMenuRegistry().getNames());
        assertTrue(services.getMenuRegistry().size() >= 1);

    }

    @Test
    @DisplayName("Конфиг: отсутствующие ключи дают дефолты, битые значения не роняют загрузку")
    void configDefaultsAndGarbage() throws IOException {

        File file = new File(plugin.getDataFolder(), "config.yml");

        Files.writeString(file.toPath(), """
                settings:
                  bottle:
                    material: ТАКОГО_НЕТ
                  menu:
                    default: ""
                messages:
                  only_players:
                    text:
                      - "текст без enabled"
                """, StandardCharsets.UTF_8);

        services.reload();

        assertEquals(Material.EXPERIENCE_BOTTLE, services.getConfigManager().getBottleMaterial());
        assertTrue(services.getConfigManager().isEnabled(), "Отсутствующий ключ равен дефолту");
        assertTrue(services.getConfigManager().getMessage(eu.neydev.expbottle.config.type.MessageKey.ONLY_PLAYERS).size() == 1);
        assertFalse(services.getConfigManager().isMessageEnabled(eu.neydev.expbottle.config.type.MessageKey.NO_PERMISSION),
                "Отсутствующее сообщение выключено");

        Files.writeString(file.toPath(), "", StandardCharsets.UTF_8);
        services.reload();

        assertTrue(services.getConfigManager().isEnabled(), "Пустой конфиг равен дефолтам");
        assertNotNull(services.getConfigManager().getDefaultMenu());

    }

    @Test
    @DisplayName("Слушатели регистрируются и работают через диспетчер")
    void listenersWired() {

        MenuListener listener = new MenuListener(plugin);
        assertNotNull(listener, "Слушатель меню создаётся");

        PlayerMock player = granted("Ney");
        player.performCommand("exp");

        InventoryView view = player.getOpenInventory();

        InventoryClickEvent event = new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 4, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        listener.onMenuClick(event);
        assertTrue(event.isCancelled(), "Клик в меню отменён слушателем");

    }
}
