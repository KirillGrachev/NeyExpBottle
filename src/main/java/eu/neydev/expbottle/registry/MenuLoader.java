package eu.neydev.expbottle.registry;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.gui.action.ClickAction;
import eu.neydev.expbottle.gui.condition.Condition;
import eu.neydev.expbottle.gui.item.MenuItem;
import eu.neydev.expbottle.gui.item.MenuItemParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Читает меню с диска и собирает из них {@link MenuDefinition}.
 *
 * <p>Отвечает только за файловый слой: создать папку {@code menus/}, положить
 * меню по умолчанию, разобрать каждый {@code .yml} и предупредить о старом
 * одиночном {@code menu.yml}. Хранением и поиском занимается {@link MenuRegistry}.</p>
 */
public class MenuLoader {

    private static final String MENUS_FOLDER = "menus";
    private static final String DEFAULT_MENU_RESOURCE = "menus/exchange.yml";
    private static final String LEGACY_FILE = "menu.yml";

    private static final int MIN_SIZE = 9;
    private static final int MAX_SIZE = 54;
    private static final int ROW = 9;

    private static final String DEFAULT_TITLE = "&#FF6B6B⚗ &fEXPERIENCE EXCHANGE &#4ECDC4⚗";

    private final NeyExpBottle plugin;
    private final Logger logger;

    public MenuLoader(@NotNull NeyExpBottle plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Читает все меню из папки {@code menus/}.
     *
     * @param menus карта, которую нужно заполнить (ключ — имя меню в нижнем регистре)
     */
    public void load(@NotNull Map<String, MenuDefinition> menus) {

        menus.clear();
        saveDefaults();

        File folder = new File(plugin.getDataFolder(), MENUS_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));

        if (files == null || files.length == 0) {
            logger.warning("Folder " + MENUS_FOLDER + "/ has no menus - /exp will open nothing");
            warnAboutLegacyFile();
            return;
        }

        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {

            String name = file.getName().substring(0, file.getName().length() - 4);

            try {
                menus.put(name.toLowerCase(Locale.ROOT), read(name, file));
            } catch (RuntimeException exception) {
                logger.warning("Failed to load menu " + file.getName() + ": " + exception.getMessage());
            }

        }

        warnAboutLegacyFile();

    }

    /**
     * Создаёт папку menus/ и кладёт туда меню по умолчанию, если её ещё нет.
     */
    private void saveDefaults() {

        File folder = new File(plugin.getDataFolder(), MENUS_FOLDER);

        // Недостижимо в тестах: песочница пускает mkdirs всегда
        if (!folder.exists() && !folder.mkdirs()) {
            logger.warning("Failed to create folder " + MENUS_FOLDER + "/");
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));

        if (files != null && files.length > 0) {
            return;
        }

        saveDefaultMenu(DEFAULT_MENU_RESOURCE);

    }

    /**
     * Кладёт встроенное меню в папку menus/.
     */
    private void saveDefaultMenu(@NotNull String resource) {
        try {
            plugin.saveResource(resource, false);
        } catch (IllegalArgumentException exception) {
            logger.warning("Failed to create the default menu " + resource + ": " + exception.getMessage());
        }
    }

    /**
     * Старый одиночный menu.yml больше не читается — предупреждаем администратора.
     */
    private void warnAboutLegacyFile() {

        File legacy = new File(plugin.getDataFolder(), LEGACY_FILE);

        if (legacy.exists()) {
            logger.warning("Found " + LEGACY_FILE + " of the old format - it is ignored. "
                    + "Move the layout into " + MENUS_FOLDER + "/ (see the README, Menus section)");
        }

    }

    private @NotNull MenuDefinition read(@NotNull String name, @NotNull File file) {

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("menu");

        if (section == null) {
            throw new IllegalStateException("no 'menu' section");
        }

        int size = readSize(section.getInt("size", MAX_SIZE), name);

        ConfigurationSection itemsSection = yaml.getConfigurationSection("items");
        List<MenuItem> items = readItems(itemsSection, size, name);

        return new MenuDefinition(
                name,
                section.getString("title", DEFAULT_TITLE),
                size,
                section.getString("permission", ""),
                readCondition(section.getString("open_requirement"), name + ".open_requirement"),
                section.getString("denial_message", ""),
                section.isSet("cycle_amount") ? section.getBoolean("cycle_amount") : null,
                Math.max(0, section.getInt("update_interval", 0)),
                ClickAction.parseList(section.getStringList("open_actions"), logger, name + ".open_actions"),
                ClickAction.parseList(section.getStringList("close_actions"), logger, name + ".close_actions"),
                items
        );

    }

    private int readSize(int raw, @NotNull String name) {

        if (raw < MIN_SIZE || raw > MAX_SIZE) {
            logger.warning("Menu " + name + ": size " + raw + " out of range "
                    + MIN_SIZE + "-" + MAX_SIZE + " - using " + MAX_SIZE);
            return MAX_SIZE;
        }

        if (raw % ROW != 0) {

            int corrected = Math.min(MAX_SIZE, ((raw / ROW) + 1) * ROW);
            logger.warning("Menu " + name + ": size must be a multiple of " + ROW + ", " + raw + " -> " + corrected);
            return corrected;

        }

        return raw;

    }

    private @NotNull List<MenuItem> readItems(@Nullable ConfigurationSection section, int size,
                                              @NotNull String name) {

        List<MenuItem> items = new ArrayList<>();

        if (section == null) {
            logger.warning("Menu " + name + ": no 'items' section - the menu will be empty");
            return items;
        }

        for (String key : section.getKeys(false)) {

            ConfigurationSection entry = section.getConfigurationSection(key);

            if (entry == null) {
                logger.warning("Menu " + name + ": items." + key + " is not a section - skipping");
                continue;
            }

            items.add(MenuItemParser.parse(key.toLowerCase(Locale.ROOT), entry, logger, size));

        }

        // Приоритет решает, кто перекроет слот при конфликте: сортируем стабильно
        items.sort(Comparator.comparingInt(MenuItem::getPriority));
        return items;

    }

    private @NotNull Condition readCondition(@Nullable String raw, @NotNull String context) {
        try {
            return Condition.parse(raw);
        } catch (IllegalArgumentException exception) {
            logger.warning(context + ": " + exception.getMessage() + " - condition ignored");
            return Condition.alwaysTrue();
        }
    }
}