package eu.neydev.expbottle.config.section;

import eu.neydev.expbottle.config.type.SoundSettings;
import eu.neydev.expbottle.util.ValueResolver;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Секция {@code settings.bottle}: предмет бутылки, её витрина и поведение броска.
 *
 * @param material         материал предмета бутылки
 * @param name             шаблон названия
 * @param lore             шаблон лора
 * @param glow             блеск бутылки
 * @param maxLevels        верхняя граница уровней в одной бутылке
 * @param instructionEnabled показывать ли подсказку использования
 * @param instructionText    текст подсказки использования
 * @param safeMode         безопасный режим броска
 * @param pickupRadius     радиус подбора опыта броска
 * @param breakSound       звук разрушения бутылки
 */
public record BottleSection(@NotNull Material material, @NotNull String name, @NotNull List<String> lore,
                            boolean glow, int maxLevels, boolean instructionEnabled, @NotNull String instructionText,
                            boolean safeMode, double pickupRadius,
                            @NotNull SoundSettings breakSound) {

    private static final Material DEFAULT_MATERIAL = Material.EXPERIENCE_BOTTLE;

    private static final String DEFAULT_NAME =
            "<gradient:#5AFB08:#A4FDB1>Experience Bottle</gradient> &8» &f{levels} LVL";

    private static final List<String> DEFAULT_LORE = List.of(
            "&7&m                        ",
            " &7▪ &fLevels: &e{levels}",
            " &7▪ &fExperience inside: &e{exp}",
            "&7&m                        "
    );

    private static final String DEFAULT_INSTRUCTION_TEXT = " #FFD700➤ &fRMB: &7use";

    private static final String PATH_MATERIAL = "settings.bottle.material";
    private static final String PATH_NAME = "settings.bottle.name";
    private static final String PATH_LORE = "settings.bottle.lore";
    private static final String PATH_GLOW = "settings.bottle.glow";
    private static final String PATH_MAX_LEVELS = "settings.bottle.max_levels";
    private static final String PATH_INSTRUCTION_ENABLED = "settings.bottle.instruction.enabled";
    private static final String PATH_INSTRUCTION_TEXT = "settings.bottle.instruction.text";
    private static final String PATH_SAFE_MODE = "settings.bottle.safe_mode";
    private static final String PATH_PICKUP_RADIUS = "settings.bottle.pickup_radius";
    private static final String PATH_BREAK_SOUND = "settings.bottle.break_sound";

    public static @NotNull BottleSection read(@NotNull FileConfiguration config, @NotNull Logger logger) {

        return new BottleSection(
                ValueResolver.material(config.getString(PATH_MATERIAL), DEFAULT_MATERIAL, logger),
                config.getString(PATH_NAME, DEFAULT_NAME),
                readLore(config, PATH_LORE, DEFAULT_LORE),
                config.getBoolean(PATH_GLOW, true),
                Math.max(1, config.getInt(PATH_MAX_LEVELS, 1000)),
                config.getBoolean(PATH_INSTRUCTION_ENABLED, true),
                config.getString(PATH_INSTRUCTION_TEXT, DEFAULT_INSTRUCTION_TEXT),
                config.getBoolean(PATH_SAFE_MODE, false),
                Math.max(0.0, config.getDouble(PATH_PICKUP_RADIUS, 4.0)),
                Sounds.read(config, PATH_BREAK_SOUND, SoundSettings.of("ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f))
        );

    }

    private static @NotNull List<String> readLore(@NotNull FileConfiguration config, @NotNull String path,
                                                  @NotNull List<String> fallback) {

        if (!config.isList(path)) {
            return new ArrayList<>(fallback);
        }

        List<String> lore = config.getStringList(path);
        return lore.isEmpty() ? new ArrayList<>(fallback) : new ArrayList<>(lore);

    }
}
