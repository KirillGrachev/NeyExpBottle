package eu.neydev.expbottle.config.type;

import org.jetbrains.annotations.NotNull;

/**
 * Настройка звука.
 *
 * <p>Имя хранится строкой намеренно: в 1.21.2+ {@code org.bukkit.Sound}
 * перестал быть перечислением, и скомпилированная против старого API ссылка
 * на константу уронила бы сервер. Строку разбирает {@code SoundUtil}.</p>
 *
 * @param enabled включать ли звук
 * @param name    имя звука из конфига
 * @param volume  громкость
 * @param pitch   высота
 */
public record SoundSettings(boolean enabled, @NotNull String name, float volume, float pitch) {

    public static @NotNull SoundSettings of(@NotNull String name, float volume, float pitch) {
        return new SoundSettings(true, name, volume, pitch);
    }

    public static @NotNull SoundSettings disabled() {
        return new SoundSettings(false, "", 1.0f, 1.0f);
    }
}
