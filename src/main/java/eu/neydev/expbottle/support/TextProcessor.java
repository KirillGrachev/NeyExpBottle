package eu.neydev.expbottle.support;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Дополнительная обработка текста внешним плагином (например, PlaceholderAPI).
 *
 * <p>Вынесено в интерфейс намеренно: {@code PlaceholderService} хранит ссылку
 * на него и не знает о классах PlaceholderAPI, поэтому отсутствие библиотеки
 * на сервере не приводит к {@code NoClassDefFoundError}.</p>
 */
public interface TextProcessor {

    /**
     * Обрабатывает текст.
     *
     * @param player игрок-контекст
     * @param text   исходный текст
     * @return текст с раскрытыми внешними плейсхолдерами
     */
    @NotNull String process(@NotNull Player player, @NotNull String text);
}
