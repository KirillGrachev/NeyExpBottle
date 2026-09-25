package eu.neydev.expbottle.service;

import eu.neydev.expbottle.config.type.SoundSettings;
import eu.neydev.expbottle.util.SoundUtil;
import eu.neydev.expbottle.util.ValueResolver;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Воспроизведение звуков.
 *
 * <p>Звук играет строковым методом {@code playSound(Location, String, float, float)}:
 * он существует во всех поддерживаемых версиях, в отличие от {@code Sound.valueOf},
 * который сломался в 1.21.2, когда {@code Sound} перестал быть перечислением.</p>
 */
public class SoundService {

    private static final float MAX_VOLUME = 10.0f;
    private static final float MAX_PITCH = 2.0f;

    private final DiagnosticsService diagnosticsService;

    public SoundService(@NotNull DiagnosticsService diagnosticsService) {
        this.diagnosticsService = diagnosticsService;
    }

    public void play(@NotNull Player player, @NotNull SoundSettings settings) {

        if (!settings.enabled()) {
            return;
        }

        play(player, settings.name(), settings.volume(), settings.pitch());

    }

    /**
     * Играет звук, если имя существует на текущем ядре.
     *
     * @param player получатель
     * @param name   имя звука из конфига
     * @param volume громкость
     * @param pitch  высота
     */
    public void play(@NotNull Player player, @Nullable String name, float volume, float pitch) {

        String key = SoundUtil.resolveKey(name);

        if (key == null) {
            diagnosticsService.debug("Sound '" + name + "' not found on this core - skipping");
            return;
        }

        try {

            player.playSound(
                    player.getLocation(),
                    key,
                    ValueResolver.clamp(volume, 0.0f, MAX_VOLUME),
                    ValueResolver.clamp(pitch, 0.0f, MAX_PITCH)
            );

        } catch (Throwable throwable) {
            diagnosticsService.debug("Failed to play sound '" + key + "': " + throwable.getMessage());
        }

    }
}