package eu.neydev.expbottle.service;

import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.registry.BottleRegistry;
import eu.neydev.expbottle.util.ExperienceFormula;
import eu.neydev.expbottle.util.HexColorUtil;
import eu.neydev.expbottle.util.Placeholders;
import eu.neydev.expbottle.support.TextProcessor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Единая точка сборки плейсхолдеров и подготовки текста.
 *
 * <p>Порядок обработки: внешние плейсхолдеры (PlaceholderAPI, если установлен) →
 * наши {@code {ключ}} → цвета. Поэтому значение нашего плейсхолдера может
 * содержать HEX-коды, а в конфигах работают и {@code %vault_balance%}, и {@code {player_exp}}.</p>
 */
public class PlaceholderService {

    private final PluginConfig config;
    private final ExperienceService experienceService;
    private final DiagnosticsService diagnosticsService;

    private volatile TextProcessor textProcessor;

    public PlaceholderService(@NotNull PluginConfig config,
                              @NotNull ExperienceService experienceService,
                              @NotNull DiagnosticsService diagnosticsService) {
        this.config = config;
        this.experienceService = experienceService;
        this.diagnosticsService = diagnosticsService;
    }

    /**
     * Подключает внешнюю обработку текста (PlaceholderAPI).
     *
     * @param textProcessor обработчик или {@code null}
     */
    public void setTextProcessor(@Nullable TextProcessor textProcessor) {
        this.textProcessor = textProcessor;
    }

    /**
     * Плейсхолдеры игрока.
     *
     * @param player игрок
     * @return набор значений
     */
    public @NotNull Placeholders forPlayer(@NotNull Player player) {

        return Placeholders.create()
                .set("player", player.getName())
                .set("player_level", player.getLevel())
                .set("player_exp", experienceService.getTotalExperience(player))
                .set("player_progress", experienceService.getProgressPercent(player))
                .set("player_levels", formatLevels(experienceService.getExactLevels(player)))
                .set("max_levels", config.getMaxBottleLevels());

    }

    /**
     * Плейсхолдеры количества уровней.
     *
     * @param levels уровни
     * @return набор значений
     */
    public @NotNull Placeholders forLevels(int levels) {

        int experience = ExperienceFormula.expFromLevels(levels);

        return Placeholders.create()
                .set("levels", levels)
                .set("exp", experience)
                .set("required_exp", experience);

    }

    /**
     * Плейсхолдеры кнопки обмена.
     *
     * @param levels    уровни кнопки
     * @param tierId    идентификатор предмета
     * @param available хватает ли игроку опыта
     * @return набор значений
     */
    public @NotNull Placeholders forTier(int levels, @NotNull String tierId, boolean available) {

        return forLevels(levels)
                .set("tier", tierId)
                .set("available", available ? config.getAvailableText() : config.getUnavailableText());

    }

    /**
     * Плейсхолдеры счётчиков плагина.
     *
     * @return набор значений
     */
    public @NotNull Placeholders forStatistics() {

        return Placeholders.create()
                .set("created", diagnosticsService.getBottlesCreated())
                .set("used", diagnosticsService.getBottlesUsed())
                .set("denied", diagnosticsService.getExchangesDenied())
                .set("suspicious", diagnosticsService.getSuspiciousEvents())
                .set("menus_opened", diagnosticsService.getMenusOpened());

    }

    /**
     * Раскрывает внешние плейсхолдеры, если обработчик подключён.
     *
     * @param sender получатель (для не-игроков текст возвращается как есть)
     * @param text   исходный текст
     * @return текст с раскрытыми внешними плейсхолдерами
     */
    public @NotNull String process(@Nullable CommandSender sender, @NotNull String text) {

        TextProcessor processor = this.textProcessor;

        if (processor == null || !(sender instanceof Player player)) {
            return text;
        }

        try {
            return processor.process(player, text);
        } catch (Throwable throwable) {
            diagnosticsService.debug("External text processor returned an error: " + throwable.getMessage());
            return text;
        }

    }

    /**
     * Готовит текст к выводу: внешние плейсхолдеры → наши → цвета.
     *
     * @param player       игрок-контекст
     * @param template     сырой шаблон
     * @param placeholders наши плейсхолдеры
     * @return готовый текст
     */
    public @NotNull String format(@NotNull Player player, @NotNull String template,
                                  @NotNull Placeholders placeholders) {
        return HexColorUtil.color(placeholders.apply(process(player, template)));
    }

    public @NotNull String format(@NotNull Player player, @NotNull String template) {
        return format(player, template, forPlayer(player));
    }

    /**
     * Готовит список строк к выводу.
     *
     * @param player       игрок-контекст
     * @param lines        сырые шаблоны
     * @param placeholders наши плейсхолдеры
     * @return готовые строки
     */
    public @NotNull List<String> formatList(@NotNull Player player, @NotNull List<String> lines,
                                            @NotNull Placeholders placeholders) {

        List<String> result = new ArrayList<>(lines.size());

        for (String line : lines) {
            result.add(format(player, line, placeholders));
        }

        return result;

    }

    /**
     * Целые уровни выводим без дробной части, остальные — с двумя знаками.
     */
    private @NotNull String formatLevels(double levels) {

        if (levels == Math.floor(levels)) {
            return String.valueOf((long) levels);
        }

        return String.format(Locale.ROOT, "%.2f", levels);

    }
}
