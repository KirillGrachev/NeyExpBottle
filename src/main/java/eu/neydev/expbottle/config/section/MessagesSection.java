package eu.neydev.expbottle.config.section;

import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.config.type.MessageSettings;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Секция {@code messages}: тексты и флаги всех сообщений плагина.
 */
public final class MessagesSection {

    private MessagesSection() {
    }

    public static @NotNull Map<MessageKey, MessageSettings> read(@NotNull FileConfiguration config,
                                                                 @NotNull Logger logger) {

        Map<MessageKey, MessageSettings> messages = new EnumMap<>(MessageKey.class);

        for (MessageKey key : MessageKey.values()) {
            messages.put(key, readMessage(config, logger, key));
        }

        return messages;

    }

    private static @NotNull MessageSettings readMessage(@NotNull FileConfiguration config, @NotNull Logger logger,
                                                        @NotNull MessageKey key) {

        String path = key.getPath();

        if (!config.isConfigurationSection(path)) {
            logger.warning("config.yml has no section '" + path + "' - message disabled");
            return MessageSettings.disabled();
        }

        boolean enabled = config.getBoolean(path + ".enabled", true);
        List<String> text = config.getStringList(path + ".text");

        return new MessageSettings(enabled, List.copyOf(text));

    }
}
