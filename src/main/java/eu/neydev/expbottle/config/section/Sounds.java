package eu.neydev.expbottle.config.section;

import eu.neydev.expbottle.config.type.SoundSettings;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Чтение звуковых настроек: одна сигнатура на все секции конфигурации.
 */
public final class Sounds {

    private Sounds() {
    }

    public static @NotNull SoundSettings read(@NotNull FileConfiguration config, @NotNull String path,
                                              @NotNull SoundSettings fallback) {
        return new SoundSettings(
                config.getBoolean(path + ".enabled", fallback.enabled()),
                config.getString(path + ".name", fallback.name()),
                (float) config.getDouble(path + ".volume", fallback.volume()),
                (float) config.getDouble(path + ".pitch", fallback.pitch())
        );
    }
}
