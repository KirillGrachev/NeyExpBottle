package eu.neydev.expbottle.config.type;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Сообщение из config.yml.
 *
 * @param enabled включать ли сообщение
 * @param text    строки (сырые шаблоны, красятся при отправке)
 */
public record MessageSettings(boolean enabled, @NotNull List<String> text) {

    private static final MessageSettings DISABLED = new MessageSettings(false, List.of());

    public static @NotNull MessageSettings disabled() {
        return DISABLED;
    }
}
