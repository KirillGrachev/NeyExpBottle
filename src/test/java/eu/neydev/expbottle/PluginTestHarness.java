package eu.neydev.expbottle;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.enchantments.EnchantmentMock;
import eu.neydev.expbottle.gui.Menu;
import eu.neydev.expbottle.gui.MenuHolder;
import eu.neydev.expbottle.service.PluginServices;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import eu.neydev.expbottle.util.HexColorUtil;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Общий каркас интеграционных тестов: поднимает мок-сервер с плагином и даёт
 * набор действий игроком, меню и конфигом.
 *
 * <p>Раньше эти хелперы жили копией в каждом тестовом классе; теперь любая
 * интеграционная проверка начинается с наследования каркаса.</p>
 */
public abstract class PluginTestHarness {

    protected static final int SLOT_TIER_5 = 21;
    protected static final int SLOT_INFO = 4;

    protected static final List<String> MENU_PLACEHOLDERS = List.of(
            "amount_selected", "amount_label", "all_label", "amount_hint", "amount_options",
            "player", "player_level", "player_exp", "player_progress", "player_levels",
            "levels", "exp", "required_exp", "available", "tier", "max_levels");

    protected ServerMock server;
    protected NeyExpBottle plugin;
    protected PluginServices services;

    @BeforeEach
    void setUpHarness() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(NeyExpBottle.class);
        services = plugin.getServices();
    }

    @AfterEach
    void tearDownHarness() {

        server.getScheduler().cancelTasks(plugin);
        MockBukkit.unmock();

    }

    protected PlayerMock granted(String name) {

        PlayerMock player = server.addPlayer(name);

        for (String permission : List.of("expbottle.use", "expbottle.exchange", "expbottle.admin")) {
            player.addAttachment(plugin, permission, true);
        }

        return player;

    }

    /**
     * ОП с явной выдачей прав: при op_bypass: false мок-сервер (не регистрирующий
     * default'ы из plugin.yml) отказал бы даже ОП-игроку.
     */
    protected PlayerMock operator(String name) {

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
    protected void registerMockEnchantments() {
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

    protected void writeMenu(String name, String yaml) throws IOException {
        File folder = new File(plugin.getDataFolder(), "menus");
        Files.writeString(new File(folder, name + ".yml").toPath(), yaml, StandardCharsets.UTF_8);
    }

    protected void setConfigValue(String from, String to) throws IOException {

        File file = new File(plugin.getDataFolder(), "config.yml");
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        if (!content.contains(from)) {
            throw new AssertionError("config.yml has no line '" + from + "'");
        }

        Files.writeString(file.toPath(), content.replace(from, to), StandardCharsets.UTF_8);

    }

    /**
     * Правка config.yml с обязательной перезагрузкой: частый шаг тестов сервисов.
     */
    protected void setConfig(String from, String to) throws IOException {
        setConfigValue(from, to);
        services.reload();
    }

    protected @NotNull InventoryClickEvent click(PlayerMock player, int slot) {
        return click(player, slot, ClickType.LEFT);
    }

    protected @NotNull InventoryClickEvent click(PlayerMock player, int slot, ClickType type) {

        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(), InventoryType.SlotType.CONTAINER, slot,
                type, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);

        return event;

    }

    /**
     * Название и лор предмета одной строкой: проверкам удобен единый текст.
     */
    protected @NotNull String loreAndName(@NotNull ItemStack item) {

        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta == null || meta.getLore() == null ? List.of() : meta.getLore();

        return (meta == null ? "" : meta.getDisplayName()) + "\n" + String.join("\n", lore);

    }

    protected boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    /**
     * Правый клик предметом в руке: событие прогоняется через плагины,
     * тест получает его же для проверок(useItemInHand и компания).
     */
    protected @NotNull PlayerInteractEvent rightClick(PlayerMock player, @NotNull ItemStack item) {

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, item, null, BlockFace.NORTH);
        server.getPluginManager().callEvent(event);

        return event;

    }

    /**
     * Первая бутылка плагина в инвентаре игрока или {@code null}.
     */
    protected ItemStack findBottle(PlayerMock player) {

        for (ItemStack item : player.getInventory().getContents()) {

            if (item != null && services.getBottleTagService().isBottle(item)) {
                return item;
            }

        }

        return null;

    }

    /**
     * Собирает все отправленные игроку сообщения в одну строку в нижнем регистре.
     */
    protected @NotNull String drainMessages(PlayerMock player) {

        List<String> messages = new ArrayList<>();
        String message;

        while ((message = player.nextMessage()) != null) {
            messages.add(HexColorUtil.strip(message));
        }

        return String.join(" ", messages).toLowerCase(Locale.ROOT);

    }

    protected @NotNull String loreOf(PlayerMock player, int slot) {
        return String.join("\n", loreLines(player, slot));
    }

    /**
     * Строки лора предмета в слоте: отдельным списком, когда тесту важна
     * каждая строка (например, ведущий пробел выравнивания).
     */
    protected @NotNull List<String> loreLines(PlayerMock player, int slot) {

        org.bukkit.inventory.ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);

        if (item == null) {
            throw new AssertionError("Slot " + slot + " has no item");
        }

        List<String> lore = item.getItemMeta().getLore();
        return lore == null ? List.of() : lore;

    }

    protected Menu findMenu(PlayerMock player) {

        InventoryView view = player.getOpenInventory();
        Inventory top = view == null ? null : view.getTopInventory();

        if (top == null || !(top.getHolder() instanceof MenuHolder holder)) {
            return null;
        }

        return holder.getMenu();

    }

    /**
     * Дочернее меню с кнопкой возврата: {@code {menu}} и {@code {cooldown}}
     * в названии и лоре дают тесту видеть подстановку плейсхолдеров.
     */
    protected void writeChildMenu() throws IOException {

        File file = new File(plugin.getDataFolder(), "menus/child.yml");

        Files.writeString(file.toPath(), String.join("\n",
                "menu:",
                "  title: \"&#8ce0ffChild &f{menu} &7cd:{cooldown}\"",
                "  size: 9",
                "items:",
                "  back:",
                "    type: CUSTOM",
                "    slot: 4",
                "    material: ARROW",
                "    name: \"&#8ce0ffBack\"",
                "    lore:",
                "      - \" &7Return to {menu}\"",
                "      - \" &7cd:{cooldown}\"",
                "    click:",
                "      - \"[back]\"",
                ""), StandardCharsets.UTF_8);

    }
}
