package eu.neydev.expbottle.config;

import eu.neydev.expbottle.config.type.FillMode;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.gui.MenuLayout;
import eu.neydev.expbottle.gui.item.MenuItem;
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
 * прогоняется через боевой код разбора ({@link MenuItem#from}), поэтому опечатка
 * в разметке видна ещё до запуска сервера.</p>
 */
class ConfigContractTest {

    private static final Logger LOGGER = Logger.getLogger("ConfigContractTest");

    private static final File CONFIG_FILE = new File("src/main/resources/config.yml");
    private static final File MENU_FILE = new File("src/main/resources/menus/exchange.yml");
    private static final File PLUGIN_FILE = new File("src/main/resources/plugin.yml");

    private static YamlConfiguration load(File file) {

        assertTrue(file.exists(), "Файл не найден: " + file.getAbsolutePath());
        return YamlConfiguration.loadConfiguration(file);

    }

    private static List<String> pathConstants(Class<?> type) throws IllegalAccessException {

        List<String> paths = new ArrayList<>();

        for (Field field : type.getDeclaredFields()) {

            int modifiers = field.getModifiers();

            if (!field.getName().startsWith("PATH_")) {
                continue;
            }

            assertTrue(Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers),
                    field.getName() + " должна быть static final");

            field.setAccessible(true);
            paths.add((String) field.get(null));

        }

        assertTrue(paths.size() >= 5, "В " + type.getSimpleName() + " подозрительно мало путей");
        return paths;

    }

    @Test
    @DisplayName("Все пути config.yml существуют")
    void allConfigPathsExist() throws IllegalAccessException {

        YamlConfiguration config = load(CONFIG_FILE);

        for (String path : pathConstants(ConfigManager.class)) {
            assertTrue(config.isSet(path), "В config.yml нет пути: " + path);
        }

    }

    @Test
    @DisplayName("Каждому ключу сообщения соответствует секция в config.yml")
    void allMessageKeysExist() {

        YamlConfiguration config = load(CONFIG_FILE);

        for (MessageKey key : MessageKey.values()) {

            String path = key.getPath();

            assertTrue(config.isSet(path + ".enabled"), "Нет " + path + ".enabled");
            assertTrue(config.isList(path + ".text"), "Нет списка " + path + ".text");

        }

    }

    @Test
    @DisplayName("Меню по умолчанию разбирается боевым кодом")
    void defaultMenuParses() {

        YamlConfiguration yaml = load(MENU_FILE);

        ConfigurationSection menu = yaml.getConfigurationSection("menu");
        assertNotNull(menu, "В menus/exchange.yml нет секции menu");

        int size = menu.getInt("size", 54);
        assertTrue(size >= 9 && size <= 54 && size % 9 == 0, "Некорректный размер меню: " + size);

        ConfigurationSection items = yaml.getConfigurationSection("items");
        assertNotNull(items, "В menus/exchange.yml нет секции items");

        List<MenuItem> parsed = new ArrayList<>();

        for (String key : items.getKeys(false)) {

            ConfigurationSection entry = items.getConfigurationSection(key);
            assertNotNull(entry, "items." + key + " не является секцией");

            parsed.add(MenuItem.from(key, entry, LOGGER, size));

        }

        assertFalse(parsed.isEmpty(), "Меню не содержит предметов");

        long tiers = parsed.stream().filter(item -> item.getType() == MenuItemType.TIER).count();
        assertEquals(5, tiers, "Ожидалось пять кнопок обмена");

        long closes = parsed.stream().filter(item -> item.getType() == MenuItemType.CLOSE).count();
        assertEquals(1, closes, "Должна быть одна кнопка закрытия");

        for (MenuItem item : parsed) {

            if (item.getType() == MenuItemType.TIER) {
                assertTrue(item.getLevels() > 0, item.getId() + ": levels должен быть больше нуля");
            }

        }

        // Раскладка строится без исключений и держит слоты в диапазоне
        MenuLayout layout = MenuLayout.of(parsed);

        for (int slot : layout.itemsBySlot().keySet()) {
            assertTrue(slot >= 0 && slot < size, "Слот " + slot + " вне диапазона меню");
        }

        assertNotNull(layout.getItem(21), "В слоте 21 ожидается кнопка обмена");

    }

    @Test
    @DisplayName("Внутри одного приоритета слоты не пересекаются")
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
                assertTrue(used.add(slot), "В приоритете " + item.getPriority()
                        + " слот " + slot + " занят дважды (предмет " + item.getId() + ")");
            }

        }

    }

    @Test
    @DisplayName("Более высокий приоритет перекрывает низкий")
    void higherPriorityWinsSlot() {

        MenuLayout layout = MenuLayout.of(parseDefaultMenu());

        MenuItem info = layout.getItem(4);
        assertNotNull(info, "В слоте 4 ничего нет");
        assertEquals("info", info.getId(), "Рамку должен перекрывать предмет с большим приоритетом");

    }

    private List<MenuItem> parseDefaultMenu() {

        YamlConfiguration yaml = load(MENU_FILE);

        int size = yaml.getInt("menu.size", 54);
        ConfigurationSection items = yaml.getConfigurationSection("items");
        assertNotNull(items, "В menus/exchange.yml нет секции items");

        List<MenuItem> parsed = new ArrayList<>();

        for (String key : items.getKeys(false)) {

            ConfigurationSection entry = items.getConfigurationSection(key);
            assertNotNull(entry, "items." + key + " не является секцией");

            parsed.add(MenuItem.from(key, entry, LOGGER, size));

        }

        parsed.sort(Comparator.comparingInt(MenuItem::getPriority));
        return parsed;

    }

    @Test
    @DisplayName("plugin.yml содержит обязательные поля")
    void pluginDescriptorIsValid() {

        YamlConfiguration descriptor = load(PLUGIN_FILE);

        assertEquals("NeyExpBottle", descriptor.getString("name"));
        assertEquals("eu.neydev.expbottle.NeyExpBottle", descriptor.getString("main"));
        assertTrue(descriptor.isSet("api-version"), "Не указана api-version");
        assertTrue(descriptor.isSet("commands.exp"), "Не объявлена команда /exp");
        assertTrue(descriptor.isSet("commands.neyexpbottle"), "Не объявлена команда /neyexpbottle");

        for (String permission : List.of("expbottle.use", "expbottle.exchange",
                "expbottle.admin", "expbottle.bypass.cooldown")) {
            assertTrue(descriptor.isSet("permissions." + permission), "Нет права " + permission);
        }

    }
}
