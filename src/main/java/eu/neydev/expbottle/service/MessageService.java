package eu.neydev.expbottle.service;

import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.util.HexColorUtil;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Отправка сообщений из config.yml.
 *
 * <p>Порядок операций важен: сначала подставляются плейсхолдеры, потом применяется
 * цвет. Благодаря этому значения плейсхолдеров тоже могут содержать HEX-коды.</p>
 */
public class MessageService {

    private final PluginConfig config;

    public MessageService(@NotNull PluginConfig config) {
        this.config = config;
    }

    public void send(@NotNull CommandSender sender, @NotNull MessageKey key) {
        send(sender, key, Placeholders.create());
    }

    /**
     * Отправляет сообщение по ключу, если оно включено в конфиге.
     *
     * @param sender       получатель
     * @param key          ключ сообщения
     * @param placeholders значения плейсхолдеров
     */
    public void send(@NotNull CommandSender sender, @NotNull MessageKey key,
                     @NotNull Placeholders placeholders) {

        if (!config.isMessageEnabled(key)) {
            return;
        }

        List<String> lines = config.getMessage(key);

        if (lines.isEmpty()) {
            return;
        }

        sendLines(sender, placeholders.apply(lines));

    }

    /**
     * Отправляет произвольный текст (используется для справок и списков из конфига).
     *
     * @param sender получатель
     * @param text   сырой текст с цветовыми кодами
     */
    public void sendText(@NotNull CommandSender sender, @NotNull String text) {
        sender.sendMessage(HexColorUtil.color(text));
    }

    public void sendLines(@NotNull CommandSender sender, @NotNull List<String> lines) {

        for (String line : lines) {
            sender.sendMessage(HexColorUtil.color(line));
        }

    }
}