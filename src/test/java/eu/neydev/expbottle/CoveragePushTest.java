package eu.neydev.expbottle;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import eu.neydev.expbottle.config.type.FillMode;
import eu.neydev.expbottle.gui.action.ActionType;
import eu.neydev.expbottle.gui.action.ClickAction;
import eu.neydev.expbottle.gui.condition.ComparisonOperator;
import eu.neydev.expbottle.gui.item.MenuItem;
import eu.neydev.expbottle.gui.item.MenuItemType;
import eu.neydev.expbottle.event.BottleThrowHandler;
import eu.neydev.expbottle.service.PluginServices;
import eu.neydev.expbottle.util.ItemBuilder;
import eu.neydev.expbottle.util.Placeholders;
import eu.neydev.expbottle.util.SignatureUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Добивание веток: операторы сравнения, сборщик предметов, инвентарь, подписи,
 * меню-сервис, действия, рендер с заполнением и админ-команда с консоли.
 */
class CoveragePushTest {

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

    @Test
    @DisplayName("Операторы сравнения: числа и строки во всех ветках")
    void comparisonOperators() {

        Placeholders values = Placeholders.create()
                .set("num", 10)
                .set("text", "Ney");

        assertTrue(ComparisonOperator.EQUALS.matches("10", "10"));
        assertFalse(ComparisonOperator.EQUALS.matches("10", "11"));
        assertTrue(ComparisonOperator.NOT_EQUALS.matches("10", "11"));
        assertFalse(ComparisonOperator.NOT_EQUALS.matches("10", "10"));
        assertTrue(ComparisonOperator.GREATER.matches("10", "9"));
        assertFalse(ComparisonOperator.GREATER.matches("9", "10"));
        assertTrue(ComparisonOperator.GREATER_OR_EQUAL.matches("10", "10"));
        assertFalse(ComparisonOperator.GREATER_OR_EQUAL.matches("9", "10"));
        assertTrue(ComparisonOperator.LESS.matches("9", "10"));
        assertFalse(ComparisonOperator.LESS.matches("10", "9"));
        assertTrue(ComparisonOperator.LESS_OR_EQUAL.matches("10", "10"));
        assertFalse(ComparisonOperator.LESS_OR_EQUAL.matches("11", "10"));

        assertTrue(ComparisonOperator.EQUALS.matches("Ney", "ney"), "Равенство строк без учёта регистра");
        assertTrue(ComparisonOperator.NOT_EQUALS.matches("Ney", "Steve"));
        assertTrue(ComparisonOperator.GREATER.matches("b", "a"), "Строки сравниваются лексикографически");
        assertFalse(ComparisonOperator.GREATER.matches("a", "b"));
        assertTrue(ComparisonOperator.GREATER_OR_EQUAL.matches("a", "a"));
        assertFalse(ComparisonOperator.GREATER_OR_EQUAL.matches("a", "b"));
        assertTrue(ComparisonOperator.LESS.matches("a", "b"));
        assertFalse(ComparisonOperator.LESS.matches("b", "a"));
        assertTrue(ComparisonOperator.LESS_OR_EQUAL.matches("a", "a"));
        assertFalse(ComparisonOperator.LESS_OR_EQUAL.matches("b", "a"));

        assertEquals(ComparisonOperator.GREATER_OR_EQUAL, ComparisonOperator.fromSymbol(">="));
        assertEquals(ComparisonOperator.LESS_OR_EQUAL, ComparisonOperator.fromSymbol("<="));
        assertEquals(ComparisonOperator.NOT_EQUALS, ComparisonOperator.fromSymbol("!="));
        assertEquals(ComparisonOperator.EQUALS, ComparisonOperator.fromSymbol("=="));
        assertEquals(ComparisonOperator.GREATER, ComparisonOperator.fromSymbol(">"));
        assertEquals(ComparisonOperator.LESS, ComparisonOperator.fromSymbol("<"));
        assertNull(ComparisonOperator.fromSymbol(null));
        assertNull(ComparisonOperator.fromSymbol("~"));
        assertNotNull(ComparisonOperator.GREATER.getSymbol());

        assertTrue(ComparisonOperator.GREATER_OR_EQUAL.matches(values.apply("{num}"), "10"));
        assertTrue(ComparisonOperator.EQUALS.matches(values.apply("{text}"), "ney"));

    }

    @Test
    @DisplayName("ItemBuilder: все ветки меты и блеск без реестра зачарований")
    void itemBuilderAllBranches() {

        ItemStack item = new ItemBuilder(Material.PAPER, 2)
                .setName("имя")
                .setLore(List.of("один", "два"))
                .addLore("три")
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .setGlow(true)
                .setUnbreakable(false)
                .setCustomModelData(0)
                .setAmount(5)
                .build();

        assertEquals(5, item.getAmount());
        assertEquals(3, item.getItemMeta().getLore().size());
        assertEquals("имя", item.getItemMeta().getDisplayName());

        ItemStack air = new ItemBuilder(Material.AIR)
                .setName("имя")
                .setLore(List.of("лор"))
                .addLore("ещё")
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlow(true)
                .setUnbreakable(true)
                .setCustomModelData(3)
                .build();

        assertNotNull(air);

    }

    @Test
    @DisplayName("Инвентарь: выдача со свободным местом и списание ровно стака")
    void inventoryRemainingBranches() {

        PlayerMock player = granted("Ney");

        services.getInventoryService().giveOrDrop(player, new ItemStack(Material.GLASS_BOTTLE));
        assertEquals(1, services.getInventoryService().count(player, Material.GLASS_BOTTLE),
                "Со свободным местом предмет попадает в инвентарь");

        assertTrue(services.getInventoryService().remove(player, Material.GLASS_BOTTLE, 1));
        assertEquals(0, services.getInventoryService().count(player, Material.GLASS_BOTTLE));

    }

    @Test
    @DisplayName("Подписи: hex без падений и регистронезависимая проверка")
    void signatureRemainingBranches() {

        byte[] secret = SignatureUtil.loadOrCreateSecret(plugin.getDataFolder(), plugin.getLogger());

        String signature = SignatureUtil.sign(secret, 1);
        assertEquals(16, signature.length());
        assertTrue(SignatureUtil.verify(secret, 1, signature.toUpperCase(Locale.ROOT)));

    }

    @Test
    @DisplayName("MenuService: право меню и запасное меню по умолчанию")
    void menuServiceRemainingBranches() throws IOException {

        writeMenu("aaa_first", """
                menu:
                  title: "First"
                  size: 9
                items:
                  deco:
                    type: DECORATION
                    slot: 4
                    material: STONE
                    name: " "
                """);

        writeMenu("locked", """
                menu:
                  title: "Locked"
                  size: 9
                  permission: "test.locked"
                items:
                  deco:
                    type: DECORATION
                    slot: 4
                    material: STONE
                    name: " "
                """);

        setConfig("default: exchange", "default: нет_такого_меню");
        services.reload();

        PlayerMock player = granted("Ney");

        assertTrue(services.getMenuService().open(player), "Дефолт падает на первое по алфавиту");
        assertEquals("aaa_first", services.getMenuService().findMenu(player).getName());
        player.closeInventory();

        assertFalse(services.getMenuService().open(player, "locked"), "Меню с правом не пускает без права");
        assertTrue(drain(player).contains("permission"));

    }

    @Test
    @DisplayName("Действия: неизвестное меню, звук без параметров и валидный обмен")
    void actionExecutorRemainingBranches() {

        PlayerMock player = granted("Ney");
        player.setLevel(10);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));

        services.getActionExecutor().execute(player, List.of(
                new ClickAction(ActionType.OPEN, "нет_такого"),
                new ClickAction(ActionType.SOUND, "ENTITY_EXPERIENCE_ORB_PICKUP"),
                new ClickAction(ActionType.EXCHANGE, "5")
        ), Placeholders.create());

        assertEquals(5, player.getLevel(), "[exchange] из действия списал уровни");

    }

    @Test
    @DisplayName("Рендер: заполнение всех и пустых слотов, динамические предметы")
    void rendererFills() throws IOException {

        writeMenu("fills", """
                menu:
                  title: "Fills"
                  size: 27
                  update_interval: 0
                items:
                  empty:
                    type: DECORATION
                    material: BLACK_STAINED_GLASS_PANE
                    name: " "
                    priority: 1
                    slots: ["empty"]
                  info:
                    type: INFO
                    slot: 4
                    material: BOOK
                    name: "Уровень {player_level}"
                    priority: 2
                  hidden:
                    type: CUSTOM
                    slot: 10
                    material: DIAMOND
                    name: "Hidden"
                    priority: 3
                    view_requirement: "{player_level} >= 1000"
                """);

        services.reload();

        PlayerMock player = granted("Ney");
        player.setLevel(3);
        player.performCommand("exp fills");

        var menu = services.getMenuService().findMenu(player);
        assertNotNull(menu);

        assertEquals(Material.BLACK_STAINED_GLASS_PANE, menu.getInventory().getItem(0).getType(),
                "Свободные слоты закрыты заполнением empty");
        assertEquals(Material.BOOK, menu.getInventory().getItem(4).getType(),
                "Информационный предмет перекрывает фон");
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, menu.getInventory().getItem(10).getType(),
                "Слот скрытого предмета закрыт фоном");

        player.setLevel(8);
        menu.refresh();
        assertTrue(menu.getInventory().getItem(4).getItemMeta().getDisplayName().contains("8"),
                "Динамический предмет перерисован");

    }

    @Test
    @DisplayName("Бросок: выключенная выдача опыта не сыплет орбы")
    void throwReleaseDisabled() throws IOException {

        setConfig("release_on_break: true", "release_on_break: false");

        BottleThrowHandler handler = new BottleThrowHandler(services);
        ItemStack bottle = services.getBottleFactory().create(10);

        assertEquals(0, handler.handleBreak(bottle, null, null),
                "При release_on_break: false орбов нет");

    }

    @Test
    @DisplayName("MenuItem: дефолты билдера и все геттеры")
    void menuItemDefaults() {

        MenuItem item = MenuItem.builder("plain").build();

        assertEquals("plain", item.getId());
        assertEquals(MenuItemType.CUSTOM, item.getType());
        assertEquals(Material.STONE, item.getMaterial());
        assertEquals(1, item.getAmount());
        assertEquals(FillMode.SLOTS, item.getFillMode());
        assertTrue(item.getSlots().isEmpty());
        assertEquals("", item.getName());
        assertTrue(item.getLore().isEmpty());
        assertFalse(item.isGlow());
        assertEquals(0, item.getCustomModelData());
        assertFalse(item.isUnbreakable());
        assertTrue(item.getItemFlags().isEmpty());
        assertEquals(1, item.getPriority());
        assertFalse(item.isRefresh());
        assertEquals(0, item.getLevels());
        assertNull(item.getSkullOwner());
        assertNull(item.getSkullTexture());
        assertTrue(item.getEnchantments().isEmpty());
        assertFalse(item.isHideEnchantments());
        assertFalse(item.isFill());
        assertNotNull(item.getViewRequirement());
        assertNotNull(item.getClickRequirement());
        assertEquals("", item.getDenialMessage());
        assertTrue(item.getClickActions().isEmpty());

    }

    @Test
    @DisplayName("Админ-команда с консоли: инфо и меню без игрока")
    void adminFromConsole() {

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "neyexpbottle info");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "neyexpbottle menu exchange");

        PlayerMock admin = granted("Admin");
        admin.performCommand("neyexpbottle give Admin 5 x");
        assertTrue(drain(admin).contains("invalid") || drain(admin).contains("value"));

        admin.performCommand("neyexpbottle чего_угодно");
        assertTrue(drain(admin).contains("commands"));

    }

    @Test
    @DisplayName("Реестр меню: имена, размер и отсутствующие меню")
    void menuRegistryGetters() {

        assertNotNull(services.getMenuRegistry().getNames());
        assertEquals(1, services.getMenuRegistry().size());
        assertTrue(services.getMenuRegistry().byName("нет_такого").isEmpty());
        assertNotNull(services.getMenuRegistry().getDefault("exchange"));
        assertNotNull(services.getMenuRegistry().getDefault(null));

    }
}
