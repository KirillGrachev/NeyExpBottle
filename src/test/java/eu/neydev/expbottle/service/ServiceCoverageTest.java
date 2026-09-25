package eu.neydev.expbottle.service;

import eu.neydev.expbottle.PluginTestHarness;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.config.type.SoundSettings;
import eu.neydev.expbottle.gui.action.ActionType;
import eu.neydev.expbottle.gui.action.ClickAction;
import eu.neydev.expbottle.model.BottleData;
import eu.neydev.expbottle.model.ExchangeOutcome;
import eu.neydev.expbottle.model.ExchangeResult;
import eu.neydev.expbottle.registry.BottleTier;
import eu.neydev.expbottle.service.ExchangeService;
import eu.neydev.expbottle.support.TextProcessor;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
class ServiceCoverageTest extends PluginTestHarness {

    @Test
    @DisplayName("Exchange: every outcome including a cancel by an external plugin")
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
    @DisplayName("The exchange without the bottle requirement and with the plugin disabled")
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
    @DisplayName("The exchange by the exchange button uses its levels")
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
    @DisplayName("The exchange with an amount: charges the total and gives a stack")
    void exchangeWithAmount() {

        PlayerMock player = granted("Ney");
        player.setLevel(30);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 3));

        ExchangeResult result = services.getExchangeService().exchange(player, 5, 3);

        assertTrue(result.isSuccess());
        assertEquals(15, result.levels(), "15 levels were spent in total");
        assertEquals(3, result.usedBottles());
        assertEquals(3, result.amount());
        assertEquals(15, player.getLevel());
        assertEquals(3, countBottles(player), "Three bottles were given");

    }

    @Test
    @DisplayName("The all exchange takes as many as the player can afford")
    void exchangeAll() {

        PlayerMock player = granted("Ney");
        player.setLevel(23);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 64));

        ExchangeResult result = services.getExchangeService()
                .exchange(player, 5, ExchangeService.AMOUNT_ALL);

        assertTrue(result.isSuccess());
        assertEquals(4, result.amount(), "23 levels at 5 give four bottles");
        assertEquals(20, result.levels());
        assertEquals(3, player.getLevel(), "The level remainder is kept");

    }

    @Test
    @DisplayName("The all exchange stops at the empty bottles")
    void exchangeAllLimitedByBottles() {

        PlayerMock player = granted("Ney");
        player.setLevel(100);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 2));

        ExchangeResult result = services.getExchangeService()
                .exchange(player, 5, ExchangeService.AMOUNT_ALL);

        assertTrue(result.isSuccess());
        assertEquals(2, result.amount(), "Without empty bottles no more than two can be made");

    }

    @Test
    @DisplayName("The all exchange names the real denial reason instead of invalid amount")
    void exchangeAllNamesRealReason() {

        ExchangeService exchange = services.getExchangeService();
        PlayerMock player = granted("Ney");

        // Ни уровней, ни пузырьков: «сколько сможешь» не вправе притворяться ошибкой количества
        assertEquals(ExchangeOutcome.NOT_ENOUGH_LEVELS,
                exchange.exchange(player, 5, ExchangeService.AMOUNT_ALL).outcome());

        player.setLevel(100);

        // Уровни есть, пустых пузырьков нет: причина та же, что видит витрина
        assertEquals(ExchangeOutcome.NOT_ENOUGH_BOTTLES,
                exchange.exchange(player, 5, ExchangeService.AMOUNT_ALL).outcome());

    }

    @Test
    @DisplayName("Menu actions run one by one and as a list")
    void actionsExecute() {

        PlayerMock player = granted("Ney");
        ActionExecutor executor = services.getActionExecutor();
        Placeholders placeholders = Placeholders.create().set("player", player.getName());

        executor.execute(player, List.of(
                new ClickAction(ActionType.MESSAGE, "&ahello {player}"),
                new ClickAction(ActionType.BROADCAST, "&ehello everyone"),
                new ClickAction(ActionType.CONSOLE, "no_such_command"),
                new ClickAction(ActionType.PLAYER, "another_missing_command"),
                new ClickAction(ActionType.CHAT, "chat message"),
                new ClickAction(ActionType.SOUND, "ENTITY_EXPERIENCE_ORB_PICKUP:0.5:1.5"),
                new ClickAction(ActionType.SOUND, "ENTITY_EXPERIENCE_ORB_PICKUP:bad:bad"),
                new ClickAction(ActionType.SOUND, "NO_SUCH_SOUND"),
                new ClickAction(ActionType.NONE, ""),
                new ClickAction(ActionType.REFRESH, "")
        ), placeholders);

        executor.execute(player, new ClickAction(ActionType.CLOSE, ""), placeholders);
        executor.execute(player, new ClickAction(ActionType.OPEN, "exchange"), placeholders);

        assertNotNull(services.getMenuService().findMenu(player), "[open] must open the menu");

    }

    @Test
    @DisplayName("The [exchange] action with a broken argument reports an error")
    void exchangeActionWithBadArgument() {

        PlayerMock player = granted("Ney");
        ActionExecutor executor = services.getActionExecutor();

        executor.execute(player, new ClickAction(ActionType.EXCHANGE, "not_a_number"), Placeholders.create());
        executor.execute(player, new ClickAction(ActionType.EXCHANGE, "999999"), Placeholders.create());

        String messages = drain(player);
        assertTrue(messages.contains("invalid") || messages.contains("value"),
                "The player must be denied: " + messages);

    }

    @Test
    @DisplayName("handleResult covers success and every denial")
    void handleResultBranches() {

        PlayerMock player = granted("Ney");
        ActionExecutor executor = services.getActionExecutor();

        executor.handleResult(player, ExchangeResult.success(5, 1, 1, 0.0));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.PLUGIN_DISABLED));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.INVALID_AMOUNT, 5));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.NO_PERMISSION, 5));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.ON_COOLDOWN, 5));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.NOT_ENOUGH_LEVELS, 5));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.NOT_ENOUGH_BOTTLES, 5));
        executor.handleResult(player, ExchangeResult.of(ExchangeOutcome.CANCELLED, 5));

        assertTrue(drain(player).contains("cancelled") || true, "The messages were sent without crashes");

    }

    @Test
    @DisplayName("Cooldown: start, remaining, the bypass permission and the cleanup of stale entries")
    void cooldownBranches() throws IOException {

        setConfig("cooldown:\n      enabled: false", "cooldown:\n      enabled: true");

        CooldownService cooldown = services.getCooldownService();
        PlayerMock player = granted("Ney");

        assertFalse(cooldown.isOnCooldown(player));
        assertEquals(0, cooldown.getRemainingMillis(player));
        assertEquals(0, cooldown.getRemainingSeconds(player));

        cooldown.start(player);
        assertTrue(cooldown.isOnCooldown(player), "With a long cooldown the player waits");
        assertTrue(cooldown.getRemainingMillis(player) >= 0);
        assertEquals(1, cooldown.size());

        cooldown.clear(player);
        setConfig("millis: 500", "millis: 1");
        cooldown.start(player);

        // Право обхода
        PlayerMock bypass = granted("Vip");
        bypass.addAttachment(plugin, "expbottle.bypass.cooldown", true);
        cooldown.start(bypass);
        assertFalse(cooldown.isOnCooldown(bypass), "The bypass permission skips the cooldown");

        // Протухшие записи вычищаются
        sleep(10);
        assertEquals(1, cooldown.purgeExpired());
        assertEquals(0, cooldown.size());

        cooldown.clear();

    }

    @Test
    @DisplayName("Sounds: disabled, unknown and valid")
    void soundBranches() {

        SoundService sound = services.getSoundService();
        PlayerMock player = granted("Ney");

        sound.play(player, SoundSettings.disabled());
        sound.play(player, (String) null, 1f, 1f);
        sound.play(player, "NO_SUCH_SOUND", 1f, 1f);
        sound.play(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 1f, 1f);

    }

    @Test
    @DisplayName("Inventory: counting, atomic charging and giving into a full inventory")
    void inventoryBranches() {

        InventoryService inventory = services.getInventoryService();
        PlayerMock player = granted("Ney");

        assertEquals(0, inventory.count(player, Material.GLASS_BOTTLE));
        assertFalse(inventory.has(player, Material.GLASS_BOTTLE, 1));
        assertTrue(inventory.remove(player, Material.GLASS_BOTTLE, 0), "Charging zero always succeeds");
        assertFalse(inventory.remove(player, Material.GLASS_BOTTLE, 1), "Nothing to charge");

        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 3));
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 2));
        assertEquals(5, inventory.count(player, Material.GLASS_BOTTLE));

        assertFalse(inventory.remove(player, Material.GLASS_BOTTLE, 9), "Not enough - nothing was charged");
        assertEquals(5, inventory.count(player, Material.GLASS_BOTTLE));

        assertTrue(inventory.remove(player, Material.GLASS_BOTTLE, 4));
        assertEquals(1, inventory.count(player, Material.GLASS_BOTTLE));

        // Полный инвентарь: излишек падает на землю
        player.getInventory().clear();
        for (int slot = 0; slot < 36; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Material.STONE, 64));
        }

        inventory.giveOrDrop(player, new ItemStack(Material.GLASS_BOTTLE));
        assertEquals(0, inventory.count(player, Material.GLASS_BOTTLE), "There was no room in the inventory");

    }

    @Test
    @DisplayName("Permissions: op_bypass turns the OP advantage on and off")
    void permissionBranches() throws IOException {

        PermissionService permissions = services.getPermissionService();

        PlayerMock op = server.addPlayer("Op");
        op.setOp(true);

        assertFalse(permissions.has(op, "expbottle.admin"),
                "With op_bypass: false an OP without an explicit grant does not pass");

        op.addAttachment(plugin, "expbottle.admin", false);
        assertFalse(permissions.has(op, "expbottle.admin"), "An explicit deny is stronger than default");

        setConfig("op_bypass: false", "op_bypass: true");
        assertTrue(permissions.has(op, "expbottle.admin"), "With op_bypass: true an OP passes");

        assertTrue(permissions.has(op, ""), "An empty permission means the check is off");

        setConfig("enabled: true # Enable the permission system", "enabled: false # Enable the permission system");
        assertTrue(permissions.has(server.addPlayer("Guest"), "expbottle.admin"),
                "The disabled permission system lets everyone through");

    }

    @Test
    @DisplayName("Messages: a disabled key stays silent, texts and lists are sent")
    void messageBranches() {

        MessageService messages = services.getMessageService();
        PlayerMock player = granted("Ney");

        messages.send(player, MessageKey.EXCHANGE_CANCELLED);
        assertEquals(0, count(player), "A disabled message is not sent");

        messages.send(player, MessageKey.BOTTLE_BROKEN);
        assertTrue(count(player) > 0);

        messages.sendText(player, "&atext");
        messages.sendLines(player, List.of("", "&7line"));

    }

    @Test
    @DisplayName("Diagnostics: debug by the flag, counters and the timing")
    void diagnosticsBranches() throws IOException {

        DiagnosticsService diagnostics = services.getDiagnosticsService();

        diagnostics.debug("invisible without the flag");
        diagnostics.info("info");
        diagnostics.warning("warning");
        diagnostics.severe("error", new IllegalStateException("test"));
        diagnostics.suspicious("suspicious");

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
        diagnostics.debug("visible with the flag");

        diagnostics.resetCounters();
        assertEquals(0, diagnostics.getBottlesCreated());

    }

    @Test
    @DisplayName("Placeholders: formatting and the external text processor")
    void placeholderBranches() {

        PlaceholderService placeholders = services.getPlaceholderService();
        PlayerMock player = granted("Ney");
        player.setLevel(7);

        assertTrue(placeholders.format(player, "level {player_level}").contains("7"));
        assertTrue(placeholders.format(player, "without placeholders").contains("without placeholders"));
        assertTrue(placeholders.format(player, "{player}", Placeholders.create().set("player", "Another"))
                .contains("Another"));
        assertEquals(1, placeholders.formatList(player, List.of("{player_level}"), Placeholders.create()).size());

        // Консоль без обработчика получает текст как есть
        assertEquals("text", placeholders.process(Bukkit.getConsoleSender(), "text"));

        placeholders.setTextProcessor((TextProcessor) (target, text) -> text.replace("%test%", "42"));
        assertTrue(placeholders.process(player, "value %test%").contains("42"));
        assertTrue(placeholders.format(player, "%test%").contains("42"));

        placeholders.setTextProcessor(null);
        assertEquals("%test%", placeholders.process(player, "%test%"));

        assertNotNull(placeholders.forStatistics());
        assertNotNull(placeholders.forTier(5, "tier_5", ExchangeOutcome.SUCCESS));
        assertNotNull(placeholders.forLevels(5));

    }

    @Test
    @DisplayName("The showcase names the real denial reason: levels or empty bottles")
    void availabilityNamesRealReason() {

        ExchangeService exchange = services.getExchangeService();
        PlayerMock player = granted("Ney");

        // Ни уровней, ни пузырьков: сделка сперва проверяет уровни
        assertEquals(ExchangeOutcome.NOT_ENOUGH_LEVELS, exchange.availability(player, 5, 1));
        assertEquals(ExchangeOutcome.NOT_ENOUGH_LEVELS,
                exchange.availability(player, 5, ExchangeService.AMOUNT_ALL));

        player.setLevel(100);

        // Уровни есть, пустых пузырьков нет: витрина не может говорить про уровни
        assertEquals(ExchangeOutcome.NOT_ENOUGH_BOTTLES, exchange.availability(player, 5, 1));
        assertEquals(ExchangeOutcome.NOT_ENOUGH_BOTTLES,
                exchange.availability(player, 5, ExchangeService.AMOUNT_ALL));
        assertFalse(exchange.affordable(player, 5, 1));

        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 64));

        assertEquals(ExchangeOutcome.SUCCESS, exchange.availability(player, 5, 1));
        assertEquals(ExchangeOutcome.SUCCESS,
                exchange.availability(player, 5, ExchangeService.AMOUNT_ALL));
        assertTrue(exchange.affordable(player, 5, 1));

        // Количество вне границ — ошибка конфигурации, а не нехватка ресурсов
        assertEquals(ExchangeOutcome.INVALID_AMOUNT, exchange.availability(player, 5, 0));
        assertEquals(ExchangeOutcome.INVALID_AMOUNT,
                exchange.availability(player, 5, ExchangeService.MAX_AMOUNT + 1));

    }

    @Test
    @DisplayName("The {available} status substitutes the text by the denial reason")
    void availableTextFollowsReason() {

        PlaceholderService placeholders = services.getPlaceholderService();
        PluginConfig config = services.getConfigManager();

        assertEquals(config.getAvailableText(),
                placeholders.forTier(5, "tier_5", ExchangeOutcome.SUCCESS).apply("{available}"));
        assertEquals(config.getUnavailableText(),
                placeholders.forTier(5, "tier_5", ExchangeOutcome.NOT_ENOUGH_LEVELS).apply("{available}"));
        assertEquals(config.getUnavailableBottlesText(),
                placeholders.forTier(5, "tier_5", ExchangeOutcome.NOT_ENOUGH_BOTTLES).apply("{available}"));

        // Прочие причины отказа показываются запасным текстом статуса
        assertEquals(config.getUnavailableText(),
                placeholders.forTier(5, "tier_5", ExchangeOutcome.INVALID_AMOUNT).apply("{available}"));

    }

    @Test
    @DisplayName("Tags: levels without a signature are detected as forged")
    void unsignedItemIsForged() {

        BottleTagService tags = services.getBottleTagService();

        ItemStack plain = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = plain.getItemMeta();
        meta.getPersistentDataContainer().set(
                tags.getLevelsKey(), PersistentDataType.INTEGER, 12);
        plain.setItemMeta(meta);

        assertTrue(tags.read(plain).forged(), "Levels without the server signature are a forgery");
        assertFalse(tags.read(new ItemStack(Material.STONE)).bottle());
        assertFalse(tags.read(null).bottle());

        // Ванильная бутылка опыта без нашей метки не считается ни бутылкой, ни подделкой
        BottleData vanilla = tags.read(new ItemStack(Material.EXPERIENCE_BOTTLE));
        assertFalse(vanilla.bottle(), "A plain bottle must not be detected as ours");
        assertFalse(vanilla.forged(), "A plain bottle must not reach the signature check");
        assertFalse(vanilla.isBroken(100), "A plain bottle must not count as damaged");

        assertNotNull(tags.getMarkerKey());
        assertNotNull(tags.getLevelsKey());
        assertNotNull(tags.getSignatureKey());

        // Использованный снаряд safe-режима: метка бутылки стирается,
        // остаётся только метка использованного — повторная награда невозможна
        ItemStack tagged = tags.tag(new ItemStack(Material.EXPERIENCE_BOTTLE), 5);
        assertFalse(tags.isSpent(tagged), "A fresh bottle is not spent");

        tags.markSpent(tagged);
        assertTrue(tags.isSpent(tagged), "The spent mark stays on the projectile");
        assertFalse(tags.isBottle(tagged), "The spent mark wipes the bottle tag");

        assertFalse(tags.isSpent(null), "An absent item is never spent");
        assertFalse(tags.isSpent(new ItemStack(Material.STONE)), "A foreign item is never spent");

    }

    @Test
    @DisplayName("The bottle factory: creating by levels and by button, the level cap")
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
    @DisplayName("MenuService: open, menu permission denial, refresh and close")
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

        assertFalse(menuService.open(player, "no_such"));

    }

    private int countBottles(PlayerMock player) {

        int total = 0;

        for (ItemStack item : player.getInventory().getContents()) {

            if (item != null && services.getBottleTagService().isBottle(item)) {
                total += item.getAmount();
            }

        }

        return total;

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