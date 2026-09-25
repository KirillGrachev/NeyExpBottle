package eu.neydev.expbottle.config;

import eu.neydev.expbottle.config.type.FillMode;
import eu.neydev.expbottle.config.section.AmountSection;
import eu.neydev.expbottle.config.section.BottleSection;
import eu.neydev.expbottle.config.section.ExchangeSection;
import eu.neydev.expbottle.config.section.MenuSection;
import eu.neydev.expbottle.config.section.PermissionsSection;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.gui.MenuLayout;
import eu.neydev.expbottle.gui.item.MenuItem;
import eu.neydev.expbottle.gui.item.MenuItemParser;
import eu.neydev.expbottle.gui.item.MenuItemType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Контракт между кодом и файлами конфигурации.
 *
 * <p>Тест рефлективно собирает все константы {@code PATH_*} из {@link ConfigManager}
 * и проверяет, что каждый путь существует в {@code config.yml}. Меню по умолчанию
 * прогоняется через боевой код разбора ({@link MenuItemParser#parse}), поэтому опечатка
 * в разметке видна ещё до запуска сервера.</p>
 */
class ConfigContractTest {

    private static final Logger LOGGER = Logger.getLogger("ConfigContractTest");

    private static final File CONFIG_FILE = new File("src/main/resources/config.yml");
    private static final File MENU_FILE = new File("src/main/resources/menus/exchange.yml");
    private static final File PLUGIN_FILE = new File("src/main/resources/plugin.yml");

    private static YamlConfiguration load(File file) {

        assertTrue(file.exists(), "File not found: " + file.getAbsolutePath());
        return YamlConfiguration.loadConfiguration(file);

    }

    /**
     * Пути конфигурации живут рядом со своими секциями: собираем константы
     * всех ридеров и проверяем каждый путь по живому config.yml.
     */
    private static List<String> configPaths() throws IllegalAccessException {

        List<String> paths = new ArrayList<>();

        for (Class<?> type : List.of(ConfigManager.class, MenuSection.class, PermissionsSection.class,
                BottleSection.class, ExchangeSection.class, AmountSection.class)) {
            paths.addAll(pathConstants(type));
        }

        assertTrue(paths.size() >= 30, "config readers expose suspiciously few paths: " + paths.size());
        return paths;

    }

    private static List<String> pathConstants(Class<?> type) throws IllegalAccessException {

        List<String> paths = new ArrayList<>();

        for (Field field : type.getDeclaredFields()) {

            int modifiers = field.getModifiers();

            if (!field.getName().startsWith("PATH_")) {
                continue;
            }

            assertTrue(Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers),
                    field.getName() + " must be static final");

            field.setAccessible(true);
            paths.add((String) field.get(null));

        }

        return paths;

    }

    @Test
    @DisplayName("All config.yml paths exist")
    void allConfigPathsExist() throws IllegalAccessException {

        YamlConfiguration config = load(CONFIG_FILE);

        for (String path : configPaths()) {
            assertTrue(config.isSet(path), "config.yml has no path: " + path);
        }

    }

    @Test
    @DisplayName("Every message key has a section in config.yml")
    void allMessageKeysExist() {

        YamlConfiguration config = load(CONFIG_FILE);

        for (MessageKey key : MessageKey.values()) {

            String path = key.getPath();

            assertTrue(config.isSet(path + ".enabled"), "Missing " + path + ".enabled");
            assertTrue(config.isList(path + ".text"), "Missing list " + path + ".text");

        }

    }

    @Test
    @DisplayName("The default menu is parsed by the production code")
    void defaultMenuParses() {

        YamlConfiguration yaml = load(MENU_FILE);

        ConfigurationSection menu = yaml.getConfigurationSection("menu");
        assertNotNull(menu, "menus/exchange.yml has no menu section");

        int size = menu.getInt("size", 54);
        assertTrue(size >= 9 && size <= 54 && size % 9 == 0, "Invalid menu size: " + size);

        ConfigurationSection items = yaml.getConfigurationSection("items");
        assertNotNull(items, "menus/exchange.yml has no items section");

        List<MenuItem> parsed = new ArrayList<>();

        for (String key : items.getKeys(false)) {

            ConfigurationSection entry = items.getConfigurationSection(key);
            assertNotNull(entry, "items." + key + " is not a section");

            parsed.add(MenuItemParser.parse(key, entry, LOGGER, size));

        }

        assertFalse(parsed.isEmpty(), "The menu holds no items");

        long tiers = parsed.stream().filter(item -> item.getType() == MenuItemType.TIER).count();
        assertEquals(5, tiers, "Five exchange buttons were expected");

        long closes = parsed.stream().filter(item -> item.getType() == MenuItemType.CLOSE).count();
        assertEquals(1, closes, "There must be exactly one close button");

        for (MenuItem item : parsed) {

            if (item.getType() == MenuItemType.TIER) {
                assertTrue(item.getLevels() > 0, item.getId() + ": levels must be greater than zero");
            }

        }

        // Раскладка строится без исключений и держит слоты в диапазоне
        MenuLayout layout = MenuLayout.of(parsed);

        for (int slot : layout.itemsBySlot().keySet()) {
            assertTrue(slot >= 0 && slot < size, "Slot " + slot + " is out of the menu range");
        }

        assertNotNull(layout.getItem(21), "Slot 21 must hold the exchange button");

    }

    @Test
    @DisplayName("Slots do not overlap within one priority")
    void slotsDoNotConflictWithinSamePriority() {

        List<MenuItem> parsed = parseDefaultMenu();

        // Пересечение разных приоритетов — штатная ситуация: рамка рисуется первой,
        // затем её перекрывают кнопки. Неоднозначность возникает только внутри
        // одного приоритета, кто из двух предметов победит — не определено.
        Map<Integer, Set<Integer>> usedByPriority = new HashMap<>();

        for (MenuItem item : parsed) {

            if (item.getFillMode() != FillMode.SLOTS) {
                continue;
            }

            Set<Integer> used = usedByPriority.computeIfAbsent(item.getPriority(), key -> new HashSet<>());

            for (int slot : item.getSlots()) {
                assertTrue(used.add(slot), "In priority " + item.getPriority()
                        + " slot " + slot + " is used twice (item " + item.getId() + ")");
            }

        }

    }

    @Test
    @DisplayName("A higher priority overrides a lower one")
    void higherPriorityWinsSlot() {

        MenuLayout layout = MenuLayout.of(parseDefaultMenu());

        MenuItem info = layout.getItem(4);
        assertNotNull(info, "Slot 4 holds nothing");
        assertEquals("info", info.getId(), "The border must be overridden by the higher priority item");

    }

    private List<MenuItem> parseDefaultMenu() {

        YamlConfiguration yaml = load(MENU_FILE);

        int size = yaml.getInt("menu.size", 54);
        ConfigurationSection items = yaml.getConfigurationSection("items");
        assertNotNull(items, "menus/exchange.yml has no items section");

        List<MenuItem> parsed = new ArrayList<>();

        for (String key : items.getKeys(false)) {

            ConfigurationSection entry = items.getConfigurationSection(key);
            assertNotNull(entry, "items." + key + " is not a section");

            parsed.add(MenuItemParser.parse(key, entry, LOGGER, size));

        }

        parsed.sort(Comparator.comparingInt(MenuItem::getPriority));
        return parsed;

    }

    @Test
    @DisplayName("plugin.yml contains the required fields")
    void pluginDescriptorIsValid() {

        YamlConfiguration descriptor = load(PLUGIN_FILE);

        assertEquals("NeyExpBottle", descriptor.getString("name"));
        assertEquals("eu.neydev.expbottle.NeyExpBottle", descriptor.getString("main"));
        assertTrue(descriptor.isSet("api-version"), "api-version is missing");
        assertTrue(descriptor.isSet("commands.exp"), "The /exp command is not declared");
        assertTrue(descriptor.isSet("commands.neyexpbottle"), "The /neyexpbottle command is not declared");

        for (String permission : List.of("expbottle.use", "expbottle.exchange",
                "expbottle.admin", "expbottle.bypass.cooldown")) {
            assertTrue(descriptor.isSet("permissions." + permission), "Missing permission " + permission);
        }

    }
}