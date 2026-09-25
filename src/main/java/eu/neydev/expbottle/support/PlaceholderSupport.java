package eu.neydev.expbottle.support;

import eu.neydev.expbottle.NeyExpBottle;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Подключение PlaceholderAPI, если он установлен.
 *
 * <p>Проверка наличия плагина вынесена в отдельный класс намеренно: сам
 * {@link ExpBottleExpansion} и {@link PlaceholderBridge} ссылаются на классы
 * PlaceholderAPI, и загрузка такого класса без установленной библиотеки
 * закончилась бы {@code NoClassDefFoundError}. Здесь ошибка перехватывается.</p>
 */
public final class PlaceholderSupport {

    private static final String PLUGIN_NAME = "PlaceholderAPI";

    private PlaceholderSupport() {
    }

    public static void registerIfPresent(@NotNull NeyExpBottle plugin) {

        if (Bukkit.getPluginManager().getPlugin(PLUGIN_NAME) == null) {
            return;
        }

        try {

            plugin.getServices().getPlaceholderService().setTextProcessor(new PlaceholderBridge());

            new ExpBottleExpansion(plugin).register();
            plugin.getLogger().info("PlaceholderAPI integration enabled");

        } catch (Throwable throwable) {
            plugin.getLogger().warning("Failed to hook PlaceholderAPI: " + throwable.getMessage());
        }

    }
}