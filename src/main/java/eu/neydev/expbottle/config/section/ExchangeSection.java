package eu.neydev.expbottle.config.section;

import eu.neydev.expbottle.config.type.SoundSettings;
import eu.neydev.expbottle.util.ValueResolver;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Секция {@code settings.exchange}: требования сделки, её звуки и кулдаун.
 *
 * @param requireEmptyBottles требовать ли пустые пузырьки за обмен
 * @param emptyBottleMaterial материал пустого пузырька
 * @param exchangeSound       звук успешного обмена
 * @param failSound           звук отказа
 * @param cooldownEnabled     включён ли кулдаун обмена
 * @param cooldownMillis      длительность кулдауна в миллисекундах
 */
public record ExchangeSection(boolean requireEmptyBottles, @NotNull Material emptyBottleMaterial,
                              @NotNull SoundSettings exchangeSound, @NotNull SoundSettings failSound,
                              boolean cooldownEnabled, long cooldownMillis) {

    private static final Material DEFAULT_EMPTY_BOTTLE_MATERIAL = Material.GLASS_BOTTLE;

    private static final String PATH_REQUIRE_BOTTLES = "settings.exchange.require_empty_bottles";
    private static final String PATH_BOTTLE_MATERIAL = "settings.exchange.empty_bottle_material";
    private static final String PATH_SOUND = "settings.exchange.sound";
    private static final String PATH_FAIL_SOUND = "settings.exchange.fail_sound";
    private static final String PATH_COOLDOWN_ENABLED = "settings.exchange.cooldown.enabled";
    private static final String PATH_COOLDOWN_MILLIS = "settings.exchange.cooldown.millis";

    public static @NotNull ExchangeSection read(@NotNull FileConfiguration config, @NotNull Logger logger) {

        return new ExchangeSection(
                config.getBoolean(PATH_REQUIRE_BOTTLES, true),
                ValueResolver.material(config.getString(PATH_BOTTLE_MATERIAL), DEFAULT_EMPTY_BOTTLE_MATERIAL, logger),
                Sounds.read(config, PATH_SOUND, SoundSettings.of("ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.4f)),
                Sounds.read(config, PATH_FAIL_SOUND, SoundSettings.of("ENTITY_VILLAGER_NO", 1.0f, 1.0f)),
                config.getBoolean(PATH_COOLDOWN_ENABLED, false),
                Math.max(0L, config.getLong(PATH_COOLDOWN_MILLIS, 500L))
        );

    }
}
