package eu.neydev.expbottle.config;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.config.section.AmountSection;
import eu.neydev.expbottle.config.section.BottleSection;
import eu.neydev.expbottle.config.section.ExchangeSection;
import eu.neydev.expbottle.config.section.MenuSection;
import eu.neydev.expbottle.config.section.MessagesSection;
import eu.neydev.expbottle.config.section.PermissionsSection;
import eu.neydev.expbottle.config.type.AmountSettings;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.config.type.MessageSettings;
import eu.neydev.expbottle.config.type.SoundSettings;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Загрузка и кеширование config.yml.
 *
 * <p>Фасад над секционными ридерами пакета {@code config.section}: сам класс
 * держит только флаги ядра и иммутабельные срезы секций, а разбор yaml живёт
 * рядом с каждой секцией. Публичный контракт {@link PluginConfig} не меняется.</p>
 */
public class ConfigManager implements PluginConfig {

    private static final String CONFIG_FILE = "config.yml";

    private final NeyExpBottle plugin;
    private final Logger logger;

    private FileConfiguration config;

    private boolean enabled;
    private boolean debug;

    private MenuSection menu;
    private PermissionsSection permissions;
    private BottleSection bottle;
    private ExchangeSection exchange;
    private AmountSettings amount;
    private Map<MessageKey, MessageSettings> messages = Map.of();

    public ConfigManager(@NotNull NeyExpBottle plugin) {

        this.plugin = plugin;
        this.logger = plugin.getLogger();

        saveDefaultConfig();
        loadConfig();
        cacheConfigValues();

    }

    private void saveDefaultConfig() {
        plugin.saveDefaultConfig();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), CONFIG_FILE);
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void cacheConfigValues() {

        enabled = config.getBoolean("settings.enabled", true);
        debug = config.getBoolean("settings.debug", false);

        menu = MenuSection.read(config);
        permissions = PermissionsSection.read(config);
        bottle = BottleSection.read(config, logger);
        exchange = ExchangeSection.read(config, logger);
        amount = AmountSection.read(config, logger);
        messages = MessagesSection.read(config, logger);

        warnAboutRemovedSections();

    }

    /**
     * Защита от дюпа больше не настраивается: если в файле осталась старая
     * секция, объясняем один раз, что она игнорируется.
     */
    private void warnAboutRemovedSections() {

        if (config.isSet("settings.anti_dupe")) {
            logger.warning("settings.anti_dupe is ignored: dupe protection is built in "
                    + "and always on. Remove the section from config.yml.");
        }

        if (config.isSet("settings.amount_menu")) {
            logger.warning("settings.amount_menu is ignored: amount selection lives in settings.amount "
                    + "and right click cycles the amount on the exchange button. "
                    + "Remove the old section from config.yml.");
        }

        if (config.isSet("settings.amount.mode") || config.isSet("settings.amount.menu")
                || config.isSet("settings.amount.hint_menu")) {
            logger.warning("settings.amount.mode/menu/hint_menu are ignored: the amount menu is gone, "
                    + "right click cycles the amount on the exchange button unless the switch is off. "
                    + "Remove these keys from config.yml.");
        }

        if (config.isSet("settings.bottle.release_on_break")) {
            logger.warning("settings.bottle.release_on_break is removed: a broken bottle always pays out "
                    + "(levels to the nearest player, orbs when nobody is nearby), so stored experience "
                    + "can no longer be destroyed by a config key. Remove the key from config.yml.");
        }

        if (config.isSet("settings.bottle.throwable")) {
            logger.warning("settings.bottle.throwable is removed: the throw behaviour follows "
                    + "settings.bottle.safe_mode (safe: the bottle is used on the click; otherwise "
                    + "the vanilla throw). Remove the key from config.yml.");
        }

        if (config.isSet("settings.amount.hint_cycle")) {
            logger.warning("settings.amount.hint_cycle was renamed to settings.amount.hint; "
                    + "the old key is ignored.");
        }

    }

    /**
     * Перечитывает config.yml с диска.
     */
    public void reload() {
        loadConfig();
        cacheConfigValues();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isDebugEnabled() {
        return debug;
    }

    @Override
    public @NotNull String getDefaultMenu() {
        return menu.defaultMenu();
    }

    @Override
    public @NotNull String getAvailableText() {
        return menu.availableText();
    }

    @Override
    public @NotNull String getUnavailableText() {
        return menu.unavailableText();
    }

    @Override
    public @NotNull String getUnavailableBottlesText() {
        return menu.unavailableBottlesText();
    }

    @Override
    public boolean arePermissionsEnabled() {
        return permissions.enabled();
    }

    @Override
    public @NotNull String getPermissionUse() {
        return permissions.use();
    }

    @Override
    public @NotNull String getPermissionExchange() {
        return permissions.exchange();
    }

    @Override
    public @NotNull String getPermissionAdmin() {
        return permissions.admin();
    }

    @Override
    public @NotNull String getPermissionBypassCooldown() {
        return permissions.bypassCooldown();
    }

    @Override
    public @NotNull Material getBottleMaterial() {
        return bottle.material();
    }

    @Override
    public @NotNull String getBottleName() {
        return bottle.name();
    }

    @Override
    public @NotNull List<String> getBottleLore() {
        return bottle.lore();
    }

    @Override
    public boolean isBottleGlowEnabled() {
        return bottle.glow();
    }

    @Override
    public boolean isInstructionEnabled() {
        return bottle.instructionEnabled();
    }

    @Override
    public @NotNull String getInstructionText() {
        return bottle.instructionText();
    }

    @Override
    public int getMaxBottleLevels() {
        return bottle.maxLevels();
    }

    @Override
    public boolean isOpBypassEnabled() {
        return permissions.opBypass();
    }

    @Override
    public boolean isSafeMode() {
        return bottle.safeMode();
    }

    @Override
    public @NotNull AmountSettings getAmount() {
        return amount;
    }

    @Override
    public double getPickupRadius() {
        return bottle.pickupRadius();
    }

    @Override
    public @NotNull SoundSettings getBreakSound() {
        return bottle.breakSound();
    }

    @Override
    public boolean areEmptyBottlesRequired() {
        return exchange.requireEmptyBottles();
    }

    @Override
    public @NotNull Material getEmptyBottleMaterial() {
        return exchange.emptyBottleMaterial();
    }

    @Override
    public @NotNull SoundSettings getExchangeSound() {
        return exchange.exchangeSound();
    }

    @Override
    public @NotNull SoundSettings getFailSound() {
        return exchange.failSound();
    }

    @Override
    public boolean isCooldownEnabled() {
        return exchange.cooldownEnabled();
    }

    @Override
    public long getCooldownMillis() {
        return exchange.cooldownMillis();
    }

    @Override
    public boolean isMessageEnabled(@NotNull MessageKey key) {
        return messages.getOrDefault(key, MessageSettings.disabled()).enabled();
    }

    @Override
    public @NotNull List<String> getMessage(@NotNull MessageKey key) {
        return messages.getOrDefault(key, MessageSettings.disabled()).text();
    }
}
