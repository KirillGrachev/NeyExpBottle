package eu.neydev.expbottle;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.enchantments.EnchantmentMock;
import eu.neydev.expbottle.event.BottleThrowHandler;
import eu.neydev.expbottle.gui.Menu;
import eu.neydev.expbottle.gui.MenuHolder;
import eu.neydev.expbottle.model.BottleData;
import eu.neydev.expbottle.util.ExperienceFormula;
import eu.neydev.expbottle.util.HexColorUtil;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
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
 * Сквозные сценарии на мок-сервере: загрузка, меню, обмен, броски,
 * подписи, права и движок меню (действия, условия, автообновление).
 */
class NeyExpBottleIntegrationTest {

    private static final int SLOT_TIER_5 = 21;
    private static final int SLOT_INFO = 4;

    private ServerMock server;
    private NeyExpBottle plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(NeyExpBottle.class);
    }

    @AfterEach
    void tearDown() {

        // Меню с update_interval держит повторяющуюся задачу в планировщике.
        // На реальном сервере её снимают InventoryCloseEvent и отключение плагина,
        // а мок-сервер без этого бесконечно ждал бы задачи в unmock().
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
        }

        server.getScheduler().cancelTasks(plugin);
        MockBukkit.unmock();

    }

    @Test
    @DisplayName("Плагин включается, меню и кнопки обмена читаются из menus/")
    void pluginStartsUp() {

        assertNotNull(plugin.getServices());
        assertTrue(plugin.getConfigManager().isEnabled());

        assertEquals(1, plugin.getServices().getMenuRegistry().size(), "Меню по умолчанию не загрузилось");
        assertTrue(plugin.getServices().getMenuRegistry().byName("exchange").isPresent());
        assertEquals(5, plugin.getBottleRegistry().size());
        assertEquals(5, plugin.getBottleRegistry().byId("tier_5").orElseThrow().levels());

        assertTrue(new File(plugin.getDataFolder(), "menus/exchange.yml").exists(),
                "Файл меню по умолчанию не создан");

    }

    @Test
    @DisplayName("/exp открывает меню с нашим holder'ом")
    void commandOpensMenu() {

        PlayerMock player = operator("Ney");
        player.performCommand("exp");

        Menu menu = findMenu(player);

        assertNotNull(menu, "Меню не открылось");
        assertEquals("exchange", menu.getName());
        assertEquals(54, menu.getInventory().getSize());
        assertNotNull(menu.getInventory().getItem(SLOT_TIER_5), "В слоте кнопки обмена нет предмета");

    }

    @Test
    @DisplayName("Обмен списывает уровни, пузырёк и выдаёт бутылку")
    void exchangeCreatesBottle() {

        PlayerMock player = operator("Ney");
        player.setLevel(30);
        player.setExp(0.0f);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));

        player.performCommand("exp");
        click(player, SLOT_TIER_5);

        assertEquals(25, player.getLevel(), "Уровни списались неверно. Сообщения: " + drainMessages(player));
        assertFalse(player.getInventory().contains(Material.GLASS_BOTTLE), "Пустой пузырёк не списан");

        ItemStack bottle = findBottle(player);
        assertNotNull(bottle, "Бутылка опыта не выдана");

        BottleData data = plugin.getServices().getBottleTagService().read(bottle);
        assertTrue(data.bottle());
        assertEquals(5, data.levels());
        assertFalse(data.forged(), "Бутылка плагина должна нести валидную подпись");
        assertEquals(1, plugin.getServices().getDiagnosticsService().getBottlesCreated());

    }

    @Test
    @DisplayName("Обмен обновляет меню: без опыта кнопка гаснет")
    void exchangeRefreshesMenu() {

        PlayerMock player = operator("Ney");
        player.setLevel(5);
        player.setExp(0.0f);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 2));

        player.performCommand("exp");

        click(player, SLOT_TIER_5);
        assertEquals(0, player.getLevel());

        click(player, SLOT_TIER_5);
        assertEquals(0, player.getLevel(), "Второй обмен прошёл без опыта");
        assertTrue(drainMessages(player).contains("don't have"));

    }

    @Test
    @DisplayName("Без опыта обмен не проходит и ничего не выдаёт")
    void exchangeWithoutExperienceFails() {

        PlayerMock player = operator("Ney");
        player.setLevel(1);
        player.setExp(0.0f);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));

        player.performCommand("exp");
        click(player, SLOT_TIER_5);

        assertEquals(1, player.getLevel(), "Уровень не должен измениться");
        assertTrue(player.getInventory().contains(Material.GLASS_BOTTLE), "Пузырёк не должен списаться");
        assertNull(findBottle(player), "Бутылка не должна появиться");

        String messages = drainMessages(player);
        assertTrue(messages.contains("don't have"), "Игрок не получил отказ: " + messages);

    }

    @Test
    @DisplayName("Без пустого пузырька обмен не проходит")
    void exchangeWithoutEmptyBottleFails() {

        PlayerMock player = operator("Ney");
        player.setLevel(30);
        player.setExp(0.0f);

        player.performCommand("exp");
        click(player, SLOT_TIER_5);

        assertEquals(30, player.getLevel(), "Опыт списался без пузырька");
        assertNull(findBottle(player));

    }

    @Test
    @DisplayName("/exp exchange работает из чата как кнопка меню")
    void exchangeCommandWorks() {

        PlayerMock player = operator("Ney");
        player.setLevel(20);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));

        player.performCommand("exp exchange 5");

        assertEquals(15, player.getLevel(), "Обмен из чата не списал уровни");
        assertNotNull(findBottle(player), "Обмен из чата не выдал бутылку");

        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
        player.performCommand("exp exchange tier_10");
        assertEquals(5, player.getLevel(), "Обмен по id кнопки не сработал");

    }

    @Test
    @DisplayName("Предметы меню нельзя вытащить кликом")
    void menuItemsCannotBeTaken() {

        PlayerMock player = operator("Ney");
        player.performCommand("exp");

        InventoryClickEvent event = click(player, SLOT_INFO);

        assertTrue(event.isCancelled(), "Клик в меню не отменён");
        assertFalse(player.getInventory().contains(Material.EXPERIENCE_BOTTLE));

    }

    @Test
    @DisplayName("Нашу бутылку можно бросить как ванильную")
    void thrownBottleIsAllowedToFly() {

        PlayerMock player = operator("Ney");
        ItemStack bottle = plugin.getServices().getBottleFactory().create(10);
        player.getInventory().setItemInMainHand(bottle);

        PlayerInteractEvent event = rightClick(player, bottle);

        assertNotEquals(Event.Result.DENY, event.useItemInHand(),
                "Бросок нашей бутылки не должен отменяться");

    }

    @Test
    @DisplayName("settings.bottle.throwable: false выключает бросок")
    void throwCanBeDisabledByConfig() throws IOException {

        setConfigValue("throwable: true", "throwable: false");
        plugin.getServices().reload();

        PlayerMock player = operator("Ney");
        ItemStack bottle = plugin.getServices().getBottleFactory().create(10);
        player.getInventory().setItemInMainHand(bottle);

        PlayerInteractEvent event = rightClick(player, bottle);

        assertEquals(Event.Result.DENY, event.useItemInHand(),
                "При throwable: false клик должен отменяться");

    }

    @Test
    @DisplayName("Повреждённая бутылка не сыплет опыт при разбивании")
    void brokenBottleReleasesNothing() {

        int maxLevels = plugin.getConfigManager().getMaxBottleLevels();
        ItemStack bottle = plugin.getServices().getBottleTagService()
                .tag(new ItemStack(Material.EXPERIENCE_BOTTLE), maxLevels + 500);

        BottleThrowHandler handler = new BottleThrowHandler(plugin.getServices());

        assertEquals(0, handler.handleBreak(bottle, null, null),
                "Бутылка сверх лимита уровней не должна ничего сыпать");

    }

    @Test
    @DisplayName("Бутылка без подписи сервера считается подделкой и отклоняется")
    void forgedBottleIsRejected() {

        ItemStack forged = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = forged.getItemMeta();
        meta.getPersistentDataContainer().set(
                plugin.getServices().getBottleTagService().getLevelsKey(),
                PersistentDataType.INTEGER, 500);
        forged.setItemMeta(meta);

        BottleData data = plugin.getServices().getBottleTagService().read(forged);
        assertTrue(data.bottle());
        assertTrue(data.forged(), "Бутылка без подписи должна определяться как подделка");

        PlayerMock player = operator("Ney");
        player.getInventory().setItemInMainHand(forged);

        PlayerInteractEvent event = rightClick(player, forged);
        assertEquals(Event.Result.DENY, event.useItemInHand(), "Подделку нельзя бросать");
        assertTrue(drainMessages(player).contains("not created by the server"));

        BottleThrowHandler handler = new BottleThrowHandler(plugin.getServices());
        assertEquals(0, handler.handleBreak(forged, player.getLocation(), player),
                "Подделка не должна сыпать опыт");

    }

    @Test
    @DisplayName("Валидная бутылка сыплет ровно сохранённый опыт")
    void validBottleReleasesStoredExperience() {

        ItemStack bottle = plugin.getServices().getBottleFactory().create(10);
        BottleThrowHandler handler = new BottleThrowHandler(plugin.getServices());

        assertEquals(ExperienceFormula.expFromLevels(10),
                handler.handleBreak(bottle, null, null),
                "Орбов должно быть ровно столько, сколько опыта лежало в бутылке");
        assertEquals(1, plugin.getServices().getDiagnosticsService().getBottlesUsed());

    }

    @Test
    @DisplayName("Поломка рядом с игроком начисляет уровни, а не сырые очки опыта")
    void breakNearPlayerGrantsLevels() {

        PlayerMock player = operator("Ney");
        player.setLevel(30);

        ItemStack bottle = plugin.getServices().getBottleFactory().create(5);
        BottleThrowHandler handler = new BottleThrowHandler(plugin.getServices());

        assertEquals(0, handler.handleBreak(bottle, player.getLocation(), null),
                "Орбы не сыпятся, когда игрок рядом");
        assertEquals(35, player.getLevel(), "Пять уровней бутылки добавились к тридцати");

    }

    @Test
    @DisplayName("Поломка в пустом месте сыплет орбы, чтобы опыт не потерялся")
    void breakFarAwayDropsOrbs() {

        PlayerMock player = operator("Ney");
        player.setLevel(30);

        ItemStack bottle = plugin.getServices().getBottleFactory().create(5);
        BottleThrowHandler handler = new BottleThrowHandler(plugin.getServices());

        org.bukkit.Location far = new org.bukkit.Location(player.getWorld(), 1000, 100, 1000);

        assertEquals(ExperienceFormula.expFromLevels(5), handler.handleBreak(bottle, far, null),
                "Вдали от игроков сыпятся орбы на полную стоимость бутылки");
        assertEquals(30, player.getLevel(), "Уровни напрямую не начисляются");

    }

    @Test
    @DisplayName("Креатив не может клонировать наши бутылки")
    void creativeCloneIsCancelled() {

        PlayerMock player = operator("Ney");
        player.performCommand("exp");

        ItemStack bottle = plugin.getServices().getBottleFactory().create(5);

        InventoryCreativeEvent event = new InventoryCreativeEvent(
                player.getOpenInventory(), InventoryType.SlotType.CONTAINER, 10, bottle);
        event.setCurrentItem(bottle);

        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled(), "Креативное клонирование бутылки должно отменяться");

    }

    @Test
    @DisplayName("op_bypass: false отказывает OP-игроку без права")
    void opWithoutPermissionIsDeniedWhenOpBypassFalse() {

        assertFalse(plugin.getConfigManager().isOpBypassEnabled(),
                "op_bypass по умолчанию должен быть false");

        PlayerMock op = server.addPlayer("Admin");
        op.setOp(true);

        op.performCommand("exp");

        assertNull(findMenu(op), "OP без expbottle.use должен получать отказ при op_bypass: false");
        assertTrue(drainMessages(op).contains("permission"));

        op.addAttachment(plugin, "expbottle.use", true);
        op.performCommand("exp");
        assertNotNull(findMenu(op), "С явной выдачей права меню должно открыться");

    }

    @Test
    @DisplayName("BottleFactory не выпускает бутылку сверх лимита из конфига")
    void factoryClampsLevels() {

        int maxLevels = plugin.getConfigManager().getMaxBottleLevels();
        ItemStack bottle = plugin.getServices().getBottleFactory().create(maxLevels + 500);

        assertEquals(maxLevels, plugin.getServices().getBottleTagService().read(bottle).levels());

    }

    @Test
    @DisplayName("Без expbottle.use меню не открывается")
    void playerWithoutPermissionCannotOpenMenu() {

        PlayerMock player = server.addPlayer("Guest");
        player.addAttachment(plugin, "expbottle.use", false);

        player.performCommand("exp");

        assertNull(findMenu(player), "Меню открылось без права");
        assertTrue(drainMessages(player).contains("permission"), "Игрок не получил сообщение о правах");

    }

    @Test
    @DisplayName("Команда выдачи создаёт бутылку у игрока")
    void giveCommandCreatesBottle() {

        PlayerMock player = server.addPlayer("Ney");
        PlayerMock admin = operator("Admin");

        admin.performCommand("neyexpbottle give Ney tier_10");

        ItemStack bottle = findBottle(player);
        assertNotNull(bottle, "Бутылка не выдана");
        assertEquals(10, plugin.getServices().getBottleTagService().read(bottle).levels());

    }

    @Test
    @DisplayName("Перезагрузка подхватывает новые меню и сохраняет работоспособность")
    void reloadKeepsPluginWorking() throws IOException {

        PlayerMock admin = operator("Admin");
        admin.performCommand("neyexpbottle reload");

        String messages = drainMessages(admin);
        assertTrue(messages.contains("reloaded"), "Нет подтверждения перезагрузки: " + messages);
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

        assertEquals(2, plugin.getServices().getMenuRegistry().size(), "Новое меню не подхвачено");
        assertEquals(6, plugin.getBottleRegistry().size(), "Новая кнопка обмена не зарегистрирована");
        assertTrue(plugin.getBottleRegistry().byId("button").isPresent());

        admin.performCommand("exp second");
        assertEquals("second", findMenu(admin).getName());

    }

    @Test
    @DisplayName("Неизвестное меню сообщает имя и ничего не открывает")
    void unknownMenuIsReported() {

        PlayerMock player = operator("Ney");
        player.performCommand("exp nosuchmenu");

        assertNull(findMenu(player));
        assertTrue(drainMessages(player).contains("not found"));

    }

    @Test
    @DisplayName("Действия клика выполняются: message, exchange, close, open")
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
        assertTrue(drainMessages(player).contains("works ney"), "Действие [message] не выполнено");
        assertNotNull(findMenu(player), "Меню закрылось после [message]");

        click(player, 11);
        assertEquals(17, player.getLevel(), "Действие [exchange] не списало уровни");
        assertNotNull(findBottle(player), "Действие [exchange] не выдало бутылку");

        click(player, 13);
        assertEquals("exchange", findMenu(player).getName(), "Действие [open] не переключило меню");

        click(player, 49);
        assertNull(findMenu(player), "Действие [close] не закрыло меню");

    }

    @Test
    @DisplayName("view_requirement скрывает предмет, click_requirement блокирует клик")
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
        assertTrue(isEmpty(top.getItem(10)), "Предмет с невыполненным view_requirement должен отсутствовать");
        assertEquals(Material.GOLD_INGOT, top.getItem(11).getType(), "Предмет с выполненным условием не показан");

        click(player, 12);
        String messages = drainMessages(player);
        assertTrue(messages.contains("need 100 levels"), "Нет сообщения об отказе: " + messages);
        assertFalse(messages.contains("must not appear"), "Действия выполнены вопреки условию");

    }

    @Test
    @DisplayName("Право меню и open_requirement проверяются до открытия")
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
        assertNull(findMenu(guest), "Меню открылось без права test.vip");

        guest.addAttachment(plugin, "test.vip", true);
        guest.performCommand("exp vip");
        assertNull(findMenu(guest), "Меню открылось при невыполненном open_requirement");
        assertTrue(drainMessages(guest).contains("need level 10"));

        guest.setLevel(15);
        guest.performCommand("exp vip");
        assertNotNull(findMenu(guest), "Меню не открылось, хотя все условия выполнены");

    }

    @Test
    @DisplayName("update_interval сам перерисовывает динамические предметы")
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
                "Меню не обновилось по update_interval");

    }

    @Test
    @DisplayName("Предметы меню поддерживают чары со скрытием и головы игроков")
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
        assertFalse(sword.getEnchantments().isEmpty(), "Зачарование не применилось");
        assertTrue(sword.getItemMeta().hasItemFlag(ItemFlag.HIDE_ENCHANTS),
                "Информация о чарах должна быть скрыта");

        ItemStack head = menu.getInventory().getItem(12);
        assertNotNull(head);
        assertEquals(Material.PLAYER_HEAD, head.getType());

    }

    @Test
    @DisplayName("Действия закрытия выполняются при закрытии меню")
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

        assertTrue(drainMessages(player).contains("menu closed"), "Действия закрытия не выполнены");

    }

    /**
     * ОП с явной выдачей прав: при op_bypass: false мок-сервер (не регистрирующий
     * default'ы из plugin.yml) отказал бы даже ОП-игроку.
     */
    private PlayerMock operator(String name) {

        PlayerMock player = server.addPlayer(name);
        player.setOp(true);

        for (String permission : List.of("expbottle.use", "expbottle.exchange",
                "expbottle.admin", "expbottle.bypass.cooldown")) {
            player.addAttachment(plugin, permission, true);
        }

        return player;

    }

    /**
     * У мок-сервера нет ванильного реестра зачарований, поэтому тест регистрирует
     * мок-зачарования под ключами, к которым приводят алиасы конфига.
     */
    @SuppressWarnings("deprecation")
    private void registerMockEnchantments() {

        registerMockEnchantment("unbreaking", "UNBREAKING");
        registerMockEnchantment("luck", "LUCK");

    }

    @SuppressWarnings("deprecation")
    private void registerMockEnchantment(String key, String legacyName) {

        NamespacedKey namespacedKey = new NamespacedKey("minecraft", key);

        if (Enchantment.getByKey(namespacedKey) != null) {
            return;
        }

        try {
            Enchantment.registerEnchantment(new EnchantmentMock(namespacedKey, legacyName));
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // Уже зарегистрировано или реестр закрыт в этой JVM
        }

    }

    private void writeMenu(String name, String yaml) throws IOException {

        File folder = new File(plugin.getDataFolder(), "menus");
        assertTrue(folder.exists() || folder.mkdirs(), "Не удалось создать папку menus/");

        Files.writeString(new File(folder, name + ".yml").toPath(), yaml, StandardCharsets.UTF_8);

    }

    private void setConfigValue(String from, String to) throws IOException {

        File file = new File(plugin.getDataFolder(), "config.yml");
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        assertTrue(content.contains(from), "В конфиге нет строки '" + from + "'");
        Files.writeString(file.toPath(), content.replace(from, to), StandardCharsets.UTF_8);

    }

    private Menu findMenu(PlayerMock player) {

        InventoryView view = player.getOpenInventory();
        Inventory top = view == null ? null : view.getTopInventory();

        if (top == null || !(top.getHolder() instanceof MenuHolder holder)) {
            return null;
        }

        return holder.getMenu();

    }

    private InventoryClickEvent click(PlayerMock player, int rawSlot) {

        InventoryView view = player.getOpenInventory();

        InventoryClickEvent event = new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, rawSlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        server.getPluginManager().callEvent(event);
        return event;

    }

    private PlayerInteractEvent rightClick(PlayerMock player, ItemStack item) {

        PlayerInteractEvent event = new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_AIR, item, null, BlockFace.SELF, EquipmentSlot.HAND);

        server.getPluginManager().callEvent(event);
        return event;

    }

    private ItemStack findBottle(PlayerMock player) {

        for (ItemStack item : player.getInventory().getContents()) {

            if (item != null && plugin.getServices().getBottleTagService().isBottle(item)) {
                return item;
            }

        }

        return null;

    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    private List<String> loreAndName(ItemStack item) {

        List<String> text = new ArrayList<>();

        if (item == null || !item.hasItemMeta() || item.getItemMeta() == null) {
            return text;
        }

        text.add(HexColorUtil.strip(item.getItemMeta().getDisplayName()));

        if (item.getItemMeta().getLore() != null) {
            item.getItemMeta().getLore().forEach(line -> text.add(HexColorUtil.strip(line)));
        }

        return text;

    }

    /**
     * Собирает все отправленные игроку сообщения в одну строку в нижнем регистре.
     */
    private String drainMessages(PlayerMock player) {

        List<String> messages = new ArrayList<>();
        String message;

        while ((message = player.nextMessage()) != null) {
            messages.add(HexColorUtil.strip(message));
        }

        return String.join(" ", messages).toLowerCase(Locale.ROOT);

    }
}
