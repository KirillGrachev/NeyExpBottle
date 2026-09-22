package eu.neydev.expbottle.config;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.config.type.MessageSettings;
import eu.neydev.expbottle.config.type.SoundSettings;
import eu.neydev.expbottle.util.ValueResolver;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Загрузка и кеширование config.yml.
 */
public class ConfigManager implements PluginConfig {

    private static final String CONFIG_FILE = "config.yml";

    private static final String PATH_ENABLED = "settings.enabled";
    private static final String PATH_DEBUG = "settings.debug";

    private static final String PATH_MENU_DEFAULT = "settings.menu.default";
    private static final String PATH_MENU_AVAILABLE = "settings.menu.available_text";
    private static final String PATH_MENU_UNAVAILABLE = "settings.menu.unavailable_text";

    private static final String PATH_PERMISSIONS_ENABLED = "settings.permissions.enabled";
    private static final String PATH_PERMISSION_USE = "settings.permissions.use";
    private static final String PATH_PERMISSION_EXCHANGE = "settings.permissions.exchange";
    private static final String PATH_PERMISSION_ADMIN = "settings.permissions.admin";
    private static final String PATH_PERMISSION_BYPASS_COOLDOWN = "settings.permissions.bypass_cooldown";

    private static final String PATH_BOTTLE_MATERIAL = "settings.bottle.material";
    private static final String PATH_BOTTLE_NAME = "settings.bottle.name";
    private static final String PATH_BOTTLE_LORE = "settings.bottle.lore";
    private static final String PATH_BOTTLE_GLOW = "settings.bottle.glow";
    private static final String PATH_BOTTLE_MAX_LEVELS = "settings.bottle.max_levels";
    private static final String PATH_INSTRUCTION_ENABLED = "settings.bottle.instruction.enabled";
    private static final String PATH_INSTRUCTION_TEXT = "settings.bottle.instruction.text";

    private static final String PATH_PERMISSIONS_OP_BYPASS = "settings.permissions.op_bypass";

    private static final String PATH_BOTTLE_THROWABLE = "settings.bottle.throwable";
    private static final String PATH_BOTTLE_RELEASE = "settings.bottle.release_on_break";
    private static final String PATH_BOTTLE_PICKUP_RADIUS = "settings.bottle.pickup_radius";
    private static final String PATH_BOTTLE_BREAK_SOUND = "settings.bottle.break_sound";

    private static final String PATH_EXCHANGE_REQUIRE_BOTTLES = "settings.exchange.require_empty_bottles";
    private static final String PATH_EXCHANGE_BOTTLE_MATERIAL = "settings.exchange.empty_bottle_material";
    private static final String PATH_EXCHANGE_SOUND = "settings.exchange.sound";
    private static final String PATH_EXCHANGE_FAIL_SOUND = "settings.exchange.fail_sound";
    private static final String PATH_COOLDOWN_ENABLED = "settings.exchange.cooldown.enabled";
    private static final String PATH_COOLDOWN_MILLIS = "settings.exchange.cooldown.millis";


    private static final Material DEFAULT_BOTTLE_MATERIAL = Material.EXPERIENCE_BOTTLE;
    private static final Material DEFAULT_EMPTY_BOTTLE_MATERIAL = Material.GLASS_BOTTLE;

    private static final String DEFAULT_BOTTLE_NAME =
            "<gradient:#5AFB08:#A4FDB1>Experience Bottle</gradient> &8» &f{levels} LVL";

    private static final List<String> DEFAULT_BOTTLE_LORE = List.of(
            "&7&m                        ",
            " &7▪ &fLevels: &e{levels}",
            " &7▪ &fExperience inside: &e{exp}",
            "&7&m                        "
    );

    private static final String DEFAULT_INSTRUCTION_TEXT = " #FFD700➤ &fRMB: &7use";

    private static final String DEFAULT_MENU = "exchange";
    private static final String DEFAULT_AVAILABLE_TEXT = "&a✔ &fAvailable";
    private static final String DEFAULT_UNAVAILABLE_TEXT = "&c✖ &fNot enough levels";

    private final NeyExpBottle plugin;
    private final Logger logger;
    private final Map<MessageKey, MessageSettings> messages = new EnumMap<>(MessageKey.class);

    private FileConfiguration config;

    private boolean enabled;
    private boolean debug;

    private String defaultMenu;
    private String availableText;
    private String unavailableText;

    private boolean permissionsEnabled;
    private String permissionUse;
    private String permissionExchange;
    private String permissionAdmin;
    private String permissionBypassCooldown;

    private Material bottleMaterial;
    private String bottleName;
    private List<String> bottleLore;
    private boolean bottleGlow;
    private int maxBottleLevels;
    private boolean instructionEnabled;
    private String instructionText;

    private boolean opBypass;
    private boolean bottleThrowable;
    private boolean releaseOnBreak;
    private double pickupRadius;
    private SoundSettings breakSound;

    private boolean requireEmptyBottles;
    private Material emptyBottleMaterial;
    private SoundSettings exchangeSound;
    private SoundSettings failSound;
    private boolean cooldownEnabled;
    private long cooldownMillis;


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

        enabled = config.getBoolean(PATH_ENABLED, true);
        debug = config.getBoolean(PATH_DEBUG, false);

        defaultMenu = config.getString(PATH_MENU_DEFAULT, DEFAULT_MENU);
        availableText = config.getString(PATH_MENU_AVAILABLE, DEFAULT_AVAILABLE_TEXT);
        unavailableText = config.getString(PATH_MENU_UNAVAILABLE, DEFAULT_UNAVAILABLE_TEXT);

        permissionsEnabled = config.getBoolean(PATH_PERMISSIONS_ENABLED, true);
        permissionUse = config.getString(PATH_PERMISSION_USE, "expbottle.use");
        permissionExchange = config.getString(PATH_PERMISSION_EXCHANGE, "expbottle.exchange");
        permissionAdmin = config.getString(PATH_PERMISSION_ADMIN, "expbottle.admin");
        permissionBypassCooldown = config.getString(PATH_PERMISSION_BYPASS_COOLDOWN, "expbottle.bypass.cooldown");

        bottleMaterial = ValueResolver.material(
                config.getString(PATH_BOTTLE_MATERIAL), DEFAULT_BOTTLE_MATERIAL, logger);
        bottleName = config.getString(PATH_BOTTLE_NAME, DEFAULT_BOTTLE_NAME);
        bottleLore = readLore(PATH_BOTTLE_LORE, DEFAULT_BOTTLE_LORE);
        bottleGlow = config.getBoolean(PATH_BOTTLE_GLOW, true);
        maxBottleLevels = Math.max(1, config.getInt(PATH_BOTTLE_MAX_LEVELS, 1000));
        instructionEnabled = config.getBoolean(PATH_INSTRUCTION_ENABLED, true);
        instructionText = config.getString(PATH_INSTRUCTION_TEXT, DEFAULT_INSTRUCTION_TEXT);

        opBypass = config.getBoolean(PATH_PERMISSIONS_OP_BYPASS, false);
        bottleThrowable = config.getBoolean(PATH_BOTTLE_THROWABLE, true);
        releaseOnBreak = config.getBoolean(PATH_BOTTLE_RELEASE, true);
        pickupRadius = Math.max(0.0, config.getDouble(PATH_BOTTLE_PICKUP_RADIUS, 4.0));
        breakSound = readSound(PATH_BOTTLE_BREAK_SOUND,
                SoundSettings.of("ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f));

        requireEmptyBottles = config.getBoolean(PATH_EXCHANGE_REQUIRE_BOTTLES, true);
        emptyBottleMaterial = ValueResolver.material(
                config.getString(PATH_EXCHANGE_BOTTLE_MATERIAL), DEFAULT_EMPTY_BOTTLE_MATERIAL, logger);
        exchangeSound = readSound(PATH_EXCHANGE_SOUND,
                SoundSettings.of("ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.4f));
        failSound = readSound(PATH_EXCHANGE_FAIL_SOUND,
                SoundSettings.of("ENTITY_VILLAGER_NO", 1.0f, 1.0f));
        cooldownEnabled = config.getBoolean(PATH_COOLDOWN_ENABLED, false);
        cooldownMillis = Math.max(0L, config.getLong(PATH_COOLDOWN_MILLIS, 500L));


        cacheMessages();

    }

    private void cacheMessages() {

        messages.clear();

        for (MessageKey key : MessageKey.values()) {
            messages.put(key, readMessage(key));
        }

    }

    private @NotNull MessageSettings readMessage(@NotNull MessageKey key) {

        String path = key.getPath();

        if (!config.isConfigurationSection(path)) {
            logger.warning("config.yml has no section '" + path + "' - message disabled");
            return MessageSettings.disabled();
        }

        boolean messageEnabled = config.getBoolean(path + ".enabled", true);
        List<String> text = config.getStringList(path + ".text");

        return new MessageSettings(messageEnabled, List.copyOf(text));

    }

    private @NotNull List<String> readLore(@NotNull String path, @NotNull List<String> fallback) {

        if (!config.isList(path)) {
            return new ArrayList<>(fallback);
        }

        List<String> lore = config.getStringList(path);
        return lore.isEmpty() ? new ArrayList<>(fallback) : new ArrayList<>(lore);

    }

    private @NotNull SoundSettings readSound(@NotNull String path, @NotNull SoundSettings fallback) {

        return new SoundSettings(
                config.getBoolean(path + ".enabled", fallback.enabled()),
                config.getString(path + ".name", fallback.name()),
                (float) config.getDouble(path + ".volume", fallback.volume()),
                (float) config.getDouble(path + ".pitch", fallback.pitch())
        );

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
        return defaultMenu;
    }

    @Override
    public @NotNull String getAvailableText() {
        return availableText;
    }

    @Override
    public @NotNull String getUnavailableText() {
        return unavailableText;
    }

    @Override
    public boolean arePermissionsEnabled() {
        return permissionsEnabled;
    }

    @Override
    public @NotNull String getPermissionUse() {
        return permissionUse;
    }

    @Override
    public @NotNull String getPermissionExchange() {
        return permissionExchange;
    }

    @Override
    public @NotNull String getPermissionAdmin() {
        return permissionAdmin;
    }

    @Override
    public @NotNull String getPermissionBypassCooldown() {
        return permissionBypassCooldown;
    }

    @Override
    public @NotNull Material getBottleMaterial() {
        return bottleMaterial;
    }

    @Override
    public @NotNull String getBottleName() {
        return bottleName;
    }

    @Override
    public @NotNull List<String> getBottleLore() {
        return bottleLore;
    }

    @Override
    public boolean isBottleGlowEnabled() {
        return bottleGlow;
    }

    @Override
    public boolean isInstructionEnabled() {
        return instructionEnabled;
    }

    @Override
    public @NotNull String getInstructionText() {
        return instructionText;
    }

    @Override
    public int getMaxBottleLevels() {
        return maxBottleLevels;
    }

    @Override
    public boolean isOpBypassEnabled() {
        return opBypass;
    }

    @Override
    public boolean isBottleThrowable() {
        return bottleThrowable;
    }

    @Override
    public boolean isReleaseOnBreak() {
        return releaseOnBreak;
    }

    @Override
    public double getPickupRadius() {
        return pickupRadius;
    }

    @Override
    public @NotNull SoundSettings getBreakSound() {
        return breakSound;
    }

    @Override
    public boolean areEmptyBottlesRequired() {
        return requireEmptyBottles;
    }

    @Override
    public @NotNull Material getEmptyBottleMaterial() {
        return emptyBottleMaterial;
    }

    @Override
    public @NotNull SoundSettings getExchangeSound() {
        return exchangeSound;
    }

    @Override
    public @NotNull SoundSettings getFailSound() {
        return failSound;
    }

    @Override
    public boolean isCooldownEnabled() {
        return cooldownEnabled;
    }

    @Override
    public long getCooldownMillis() {
        return cooldownMillis;
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
