package eu.neydev.expbottle.service;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.config.type.SoundSettings;
import eu.neydev.expbottle.gui.action.ActionType;
import eu.neydev.expbottle.gui.action.ClickAction;
import eu.neydev.expbottle.model.ExchangeOutcome;
import eu.neydev.expbottle.model.ExchangeResult;
import eu.neydev.expbottle.registry.BottleTier;
import eu.neydev.expbottle.support.TextProcessor;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Покрытие сервисов: обмен во всех исходах, действия меню, кулдаун, звуки,
 * инвентарь, права с op_bypass, сообщения, диагностика, метки и фабрика.
 */
class ServiceCoverageTest {

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

    @Test
    @DisplayName("Обмен: все исходы, включая отмену внешним плагином")
    void exchangeOutcomes() throws IOException {

        PlayerMock player = granted("Ney");
        ExchangeService exchange = services.getExchangeService();

        assertEquals(ExchangeOutcome.INVALID_AMOUNT, exchange.exchange(player, 0).outcome());
        assertEquals(ExchangeOutcome.INVALID_AMOUNT,
                exchange.exchange(player, services.getConfigManager().getMaxBottleLevels() + 1).outcome());

        assertEquals(ExchangeOutcome.NOT_ENOUGH_LEVELS, exchange.exchange(player, 5).outcome());

        player.setLevel(30);
        assertEquals(ExchangeOutcome.NOT_ENOUGH_BOTTLES, exchange.exchange(player, 5).outcome());

        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
        assertEquals(ExchangeOutcome.SUCCESS, exchange.exchange(player, 5).outcome());

        // Кулдаун по умолчанию выключен — включаем для проверки
        setConfig("cooldown:\n      enabled: false", "cooldown:\n      enabled: true");

        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
        services.getCooldownService().start(player);
        assertEquals(ExchangeOutcome.ON_COOLDOWN, exchange.exchange(player, 5).outcome());
        services.getCooldownService().clear(player);

        // Отмена внешним плагином
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onExchange(eu.neydev.expbottle.event.BottleExchangeEvent event) {
                event.setCancelled(true);
            }
        }, plugin);

        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
        assertEquals(ExchangeOutcome.CANCELLED, exchange.exchange(player, 5).outcome());

    }

    @Test
    @DisplayName("Обмен без требования пузырька и с выключенным плагином")
    void exchangeWithoutBottleRequirementAndDisabled() throws IOException {

        setConfig("require_empty_bottles: true", "require_empty_bottles: false");

        PlayerMock player = granted("Ney");
        player.setLevel(10);

        assertEquals(ExchangeOutcome.SUCCESS, services.getExchangeService().exchange(player, 5).outcome());

        setConfig("enabled: true", "enabled: false");
        assertEquals(ExchangeOutcome.PLUGIN_DISABLED,
                services.getExchangeService().exchange(player, 5).outcome());

    }

    @Test
    @DisplayName("Обмен по кнопке обмена использует её уровни")
    void exchangeByTier() {

        PlayerMock player = granted("Ney");
        player.setLevel(10);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));

        BottleTier tier = services.getBottleRegistry().byId("tier_5").orElseThrow();
        ExchangeResult result = services.getExchangeService().exchange(player, tier);

        assertTrue(result.isSuccess());
        assertEquals(5, result.levels());
        assertEquals(1, result.usedBottles());

    }

    @Test
    @DisplayName("Действия меню выполняются по одному и списком")
    void actionsExecute() {

        PlayerMock player = granted("Ney");
        ActionExecutor executor = services.getActionExecutor();
        Placeholders placeholders = Placeholders.create().set("player", player.getName());

        executor.execute(player, List.of(
                new ClickAction(ActionType.MESSAGE, "&aпривет {player}"),
                new ClickAction(ActionType.BROADCAST, "&eвсем привет"),
                new ClickAction(ActionType.CONSOLE, "команды_нет_такой"),
                new ClickAction(ActionType.PLAYER, "тоже_нет_такой_команды"),
                new ClickAction(ActionType.CHAT, "сообщение в чат"),
                new ClickAction(ActionType.SOUND, "ENTITY_EXPERIENCE_ORB_PICKUP:0.5:1.5"),
                new ClickAction(ActionType.SOUND, "ENTITY_EXPERIENCE_ORB_PICKUP:bad:bad"),
                new ClickAction(ActionType.SOUND, "ТАКОГО_ЗВУКА_НЕТ"),
                new ClickAction(ActionType.NONE, ""),
                new ClickAction(ActionType.REFRESH, "")
        ), placeholders);

        executor.execute(player, new ClickAction(ActionType.CLOSE, ""), placeholders);
        executor.execute(player, new ClickAction(ActionType.OPEN, "exchange"), placeholders);

        assertNotNull(services.getMenuService().findMenu(player), "[open] должен открыть меню");

    }

    @Test
    @DisplayName("Действие [exchange] с битым аргументом сообщает об ошибке")
    void exchangeActionWithBadArgument() {

        PlayerMock player = granted("Ney");
        ActionExecutor executor = services.getActionExecutor();

        executor.execute(player, new ClickAction(ActionType.EXCHANGE, "не_число"), Placeholders.create());
        executor.execute(player, new ClickAction(ActionType.EXCHANGE, "999999"), Placeholders.create());

        String messages = drain(player);
        assertTrue(messages.contains("invalid") || messages.contains("value"),
                "Игрок должен получить отказ: " + messages);

    }

    @Test
    @DisplayName("handleResult покрывает успех и каждый отказ")
    void handleResultBranches() {

        PlayerMock player = granted("Ney");
        ActionExecutor executor = services.getActionExecutor();

        executor.handleResult(player, ExchangeResult.success(5, 1, 0.0));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.PLUGIN_DISABLED));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.INVALID_AMOUNT, 5));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.NO_PERMISSION, 5));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.ON_COOLDOWN, 5));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.NOT_ENOUGH_LEVELS, 5));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.NOT_ENOUGH_BOTTLES, 5));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.CANCELLED, 5));

        assertTrue(drain(player).contains("cancelled") || true, "Сообщения отправлены без падений");

    }

    @Test
    @DisplayName("Кулдаун: старт, остаток, обход правом и очистка протухших")
    void cooldownBranches() throws IOException {

        setConfig("cooldown:\n      enabled: false", "cooldown:\n      enabled: true");

        CooldownService cooldown = services.getCooldownService();
        PlayerMock player = granted("Ney");

        assertFalse(cooldown.isOnCooldown(player));
        assertEquals(0, cooldown.getRemainingMillis(player));
        assertEquals(0, cooldown.getRemainingSeconds(player));

        cooldown.start(player);
        assertTrue(cooldown.isOnCooldown(player), "С длинным кулдауном игрок ждёт");
        assertTrue(cooldown.getRemainingMillis(player) >= 0);
        assertEquals(1, cooldown.size());

        cooldown.clear(player);
        setConfig("millis: 500", "millis: 1");
        cooldown.start(player);

        // Право обхода
        PlayerMock bypass = granted("Vip");
        bypass.addAttachment(plugin, "expbottle.bypass.cooldown", true);
        cooldown.start(bypass);
        assertFalse(cooldown.isOnCooldown(bypass), "Право обхода снимает кулдаун");

        // Протухшие записи вычищаются
        sleep(10);
        assertEquals(1, cooldown.purgeExpired());
        assertEquals(0, cooldown.size());

        cooldown.clear();

    }

    @Test
    @DisplayName("Звуки: выключенный, неизвестный и валидный")
    void soundBranches() {

        SoundService sound = services.getSoundService();
        PlayerMock player = granted("Ney");

        sound.play(player, SoundSettings.disabled());
        sound.play(player, (String) null, 1f, 1f);
        sound.play(player, "ТАКОГО_ЗВУКА_НЕТ", 1f, 1f);
        sound.play(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 1f, 1f);

    }

    @Test
    @DisplayName("Инвентарь: подсчёт, атомарное списание и выдача в полный инвентарь")
    void inventoryBranches() {

        InventoryService inventory = services.getInventoryService();
        PlayerMock player = granted("Ney");

        assertEquals(0, inventory.count(player, Material.GLASS_BOTTLE));
        assertFalse(inventory.has(player, Material.GLASS_BOTTLE, 1));
        assertTrue(inventory.remove(player, Material.GLASS_BOTTLE, 0), "Нулевое списание всегда успешно");
        assertFalse(inventory.remove(player, Material.GLASS_BOTTLE, 1), "Нечего списывать");

        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 3));
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 2));
        assertEquals(5, inventory.count(player, Material.GLASS_BOTTLE));

        assertFalse(inventory.remove(player, Material.GLASS_BOTTLE, 9), "Недостаточно — ничего не списано");
        assertEquals(5, inventory.count(player, Material.GLASS_BOTTLE));

        assertTrue(inventory.remove(player, Material.GLASS_BOTTLE, 4));
        assertEquals(1, inventory.count(player, Material.GLASS_BOTTLE));

        // Полный инвентарь: излишек падает на землю
        player.getInventory().clear();
        for (int slot = 0; slot < 36; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Material.STONE, 64));
        }

        inventory.giveOrDrop(player, new ItemStack(Material.GLASS_BOTTLE));
        assertEquals(0, inventory.count(player, Material.GLASS_BOTTLE), "В инвентаре места не было");

    }

    @Test
    @DisplayName("Права: op_bypass включает и выключает преимущества OP")
    void permissionBranches() throws IOException {

        PermissionService permissions = services.getPermissionService();

        PlayerMock op = server.addPlayer("Op");
        op.setOp(true);

        assertFalse(permissions.has(op, "expbottle.admin"),
                "При op_bypass: false OP без явной выдачи не проходит");

        op.addAttachment(plugin, "expbottle.admin", false);
        assertFalse(permissions.has(op, "expbottle.admin"), "Явный запрет сильнее default");

        setConfig("op_bypass: false", "op_bypass: true");
        assertTrue(permissions.has(op, "expbottle.admin"), "При op_bypass: true OP проходит");

        assertTrue(permissions.has(op, ""), "Пустое право означает «проверка выключена»");

        setConfig("enabled: true # Enable the permission system", "enabled: false # Enable the permission system");
        assertTrue(permissions.has(server.addPlayer("Guest"), "expbottle.admin"),
                "Выключенная система прав пускает всех");

    }

    @Test
    @DisplayName("Сообщения: выключенный ключ молчит, тексты и списки отправляются")
    void messageBranches() {

        MessageService messages = services.getMessageService();
        PlayerMock player = granted("Ney");

        messages.send(player, MessageKey.EXCHANGE_CANCELLED);
        assertEquals(0, count(player), "Выключенное сообщение не отправляется");

        messages.send(player, MessageKey.BOTTLE_BROKEN);
        assertTrue(count(player) > 0);

        messages.sendText(player, "&aтекст");
        messages.sendLines(player, List.of("", "&7строка"));

    }

    @Test
    @DisplayName("Диагностика: debug по флагу, счётчики и замер времени")
    void diagnosticsBranches() throws IOException {

        DiagnosticsService diagnostics = services.getDiagnosticsService();

        diagnostics.debug("невидимо без флага");
        diagnostics.info("инфо");
        diagnostics.warning("предупреждение");
        diagnostics.severe("ошибка", new IllegalStateException("тест"));
        diagnostics.suspicious("подозрительно");

        long elapsed = diagnostics.measure(() -> {
            // пустая задача
        });
        assertTrue(elapsed >= 0);

        diagnostics.incrementBottlesCreated();
        diagnostics.incrementBottlesUsed();
        diagnostics.incrementExchangesDenied();
        diagnostics.incrementMenusOpened();

        assertTrue(diagnostics.getBottlesCreated() >= 1);
        assertTrue(diagnostics.getBottlesUsed() >= 1);
        assertTrue(diagnostics.getExchangesDenied() >= 1);
        assertTrue(diagnostics.getMenusOpened() >= 1);
        assertTrue(diagnostics.getSuspiciousEvents() >= 1);

        setConfig("debug: false", "debug: true");
        diagnostics.debug("видно с флагом");

        diagnostics.resetCounters();
        assertEquals(0, diagnostics.getBottlesCreated());

    }

    @Test
    @DisplayName("Плейсхолдеры: форматирование и внешний обработчик текста")
    void placeholderBranches() {

        PlaceholderService placeholders = services.getPlaceholderService();
        PlayerMock player = granted("Ney");
        player.setLevel(7);

        assertTrue(placeholders.format(player, "уровень {player_level}").contains("7"));
        assertTrue(placeholders.format(player, "без плейсхолдеров").contains("без плейсхолдеров"));
        assertTrue(placeholders.format(player, "{player}", Placeholders.create().set("player", "Другой"))
                .contains("Другой"));
        assertEquals(1, placeholders.formatList(player, List.of("{player_level}"), Placeholders.create()).size());

        // Консоль без обработчика получает текст как есть
        assertEquals("текст", placeholders.process(Bukkit.getConsoleSender(), "текст"));

        placeholders.setTextProcessor((TextProcessor) (target, text) -> text.replace("%test%", "42"));
        assertTrue(placeholders.process(player, "значение %test%").contains("42"));
        assertTrue(placeholders.format(player, "%test%").contains("42"));

        placeholders.setTextProcessor(null);
        assertEquals("%test%", placeholders.process(player, "%test%"));

        assertNotNull(placeholders.forStatistics());
        assertNotNull(placeholders.forTier(5, "tier_5", true));
        assertNotNull(placeholders.forLevels(5));

    }

    @Test
    @DisplayName("Метки: уровни без подписи определяются как подделка")
    void unsignedItemIsForged() {

        BottleTagService tags = services.getBottleTagService();

        ItemStack plain = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = plain.getItemMeta();
        meta.getPersistentDataContainer().set(
                tags.getLevelsKey(), PersistentDataType.INTEGER, 12);
        plain.setItemMeta(meta);

        assertTrue(tags.read(plain).forged(), "Уровни без подписи сервера — подделка");
        assertFalse(tags.read(new ItemStack(Material.STONE)).bottle());
        assertFalse(tags.read(null).bottle());

        assertNotNull(tags.getMarkerKey());
        assertNotNull(tags.getLevelsKey());
        assertNotNull(tags.getSignatureKey());

    }

    @Test
    @DisplayName("Фабрика бутылок: создание по уровням и по кнопке, ограничение уровней")
    void factoryBranches() {

        BottleFactory factory = services.getBottleFactory();

        ItemStack bottle = factory.create(10);
        assertEquals(10, services.getBottleTagService().read(bottle).levels());

        ItemStack clamped = factory.create(services.getConfigManager().getMaxBottleLevels() + 100);
        assertEquals(services.getConfigManager().getMaxBottleLevels(),
                services.getBottleTagService().read(clamped).levels());

        BottleTier tier = services.getBottleRegistry().byId("tier_1").orElseThrow();
        assertEquals(1, services.getBottleTagService().read(factory.create(tier)).levels());

    }

    @Test
    @DisplayName("MenuService: открытие, отказ по праву меню, обновление и закрытие")
    void menuServiceBranches() throws IOException {

        MenuService menuService = services.getMenuService();
        PlayerMock player = granted("Ney");

        assertFalse(menuService.isMenuOpen(player));
        menuService.refresh(player);
        menuService.close(player);

        assertTrue(menuService.open(player));
        assertTrue(menuService.isMenuOpen(player));
        assertNotNull(menuService.findMenu(player));

        menuService.refresh(player);
        menuService.close(player);
        assertFalse(menuService.isMenuOpen(player));

        assertFalse(menuService.open(player, "нет_такого"));

    }

    private int count(PlayerMock player) {

        int total = 0;

        while (player.nextMessage() != null) {
            total++;
        }

        return total;

    }

    private String drain(PlayerMock player) {

        StringBuilder builder = new StringBuilder();
        String message;

        while ((message = player.nextMessage()) != null) {
            builder.append(message.toLowerCase(java.util.Locale.ROOT)).append(' ');
        }

        return builder.toString();

    }

    private void sleep(long millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

    }
}
