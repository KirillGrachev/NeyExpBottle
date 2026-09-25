package eu.neydev.expbottle.service;

import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.registry.MenuRegistry;
import eu.neydev.expbottle.model.ExchangeOutcome;
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
    private final AmountSelectionService amountSelectionService;
    private final CooldownService cooldownService;
    private final MenuRegistry menuRegistry;

    private volatile TextProcessor textProcessor;

    public PlaceholderService(@NotNull PluginConfig config,
                              @NotNull ExperienceService experienceService,
                              @NotNull DiagnosticsService diagnosticsService,
                              @NotNull AmountSelectionService amountSelectionService,
                              @NotNull CooldownService cooldownService,
                              @NotNull MenuRegistry menuRegistry) {
        this.config = config;
        this.experienceService = experienceService;
        this.diagnosticsService = diagnosticsService;
        this.amountSelectionService = amountSelectionService;
        this.cooldownService = cooldownService;
        this.menuRegistry = menuRegistry;
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

        int selected = amountSelectionService.getSelected(player);
        String label = amountSelectionService.label(selected);
        String allLabel = config.getAmount().allLabel();

        return Placeholders.create()
                .set("player", player.getName())
                .set("player_level", player.getLevel())
                .set("player_exp", experienceService.getTotalExperience(player))
                .set("player_progress", experienceService.getProgressPercent(player))
                .set("player_levels", formatLevels(experienceService.getExactLevels(player)))
                .set("max_levels", config.getMaxBottleLevels())
                .set("amount_selected", amountSelectionService.raw(selected))
                .set("amount_label", label)
                .set("all_label", allLabel)
                .set("amount_hint", amountHint(label, allLabel));

    }

    /**
     * Подсказка правой кнопки из {@code settings.amount.hint} с уже раскрытыми
     * внутренними плейсхолдерами.
     *
     * <p>Шаблон подсказки сам содержит {@code {amount_label}}: раскрываем его
     * заранее, чтобы значение {@code {amount_hint}} приходило в лор готовым и
     * ни один путь вывода не показывал нераскрытые скобки.</p>
     *
     * @param label   подпись выбранного количества
     * @param allLabel подпись варианта «всё»
     * @return готовый шаблон подсказки (цвета ещё не применены)
     */
    private @NotNull String amountHint(@NotNull String label, @NotNull String allLabel) {

        return Placeholders.create()
                .set("amount_label", label)
                .set("all_label", allLabel)
                .apply(config.getAmount().hint());

    }

    /**
     * Плейсхолдеры окна меню: игровые плюс имя меню, остаток кулдауна
     * и состояние переключателя количества {@code {amount_cycle}}.
     *
     * @param player   игрок
     * @param menuName имя открытого меню
     * @return набор значений
     */
    public @NotNull Placeholders forMenu(@NotNull Player player, @NotNull String menuName) {

        Placeholders placeholders = forPlayer(player)
                .set("menu", menuName)
                .set("cooldown", cooldownService.getRemainingSeconds(player));

        // Выключенный переключатель не рекламирует себя: подсказка про ПКМ
        // гаснет, а витрина получает {amount_cycle} для условий и лора
        boolean cycleEnabled = cycleEnabled(menuName);

        placeholders.set("amount_cycle", cycleEnabled);

        if (!cycleEnabled) {
            placeholders.set("amount_hint", "");
        }

        return placeholders;

    }

    /**
     * Разрешено ли переключение количества в меню: собственное
     * {@code cycle_amount} меню важнее глобального тумблера.
     *
     * @param menuName имя меню
     * @return true если правый клик переключает количество
     */
    private boolean cycleEnabled(@NotNull String menuName) {

        boolean global = config.getAmount().cycleOnRightClick();

        return menuRegistry.byName(menuName)
                .map(definition -> definition.cycleEnabled(global))
                .orElse(global);

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
     * @param levels       уровни кнопки
     * @param tierId       идентификатор предмета
     * @param availability исход проверки ресурсов сервиса обмена
     * @return набор значений
     */
    public @NotNull Placeholders forTier(int levels, @NotNull String tierId,
                                         @NotNull ExchangeOutcome availability) {

        return forLevels(levels)
                .set("tier", tierId)
                .set("available", availableText(availability));

    }

    /**
     * Текст статуса {@code {available}}: причину отказа определяет вызывающий
     * код, а текст — конфиг, поэтому витрина не врала о причине: «не хватает
     * уровней» и «не хватает пустых пузырьков» — разные строки.
     *
     * @param availability исход проверки ресурсов
     * @return настроенный текст статуса
     */
    private @NotNull String availableText(@NotNull ExchangeOutcome availability) {

        if (availability.isSuccess()) {
            return config.getAvailableText();
        }

        if (availability == ExchangeOutcome.NOT_ENOUGH_BOTTLES) {
            return config.getUnavailableBottlesText();
        }

        return config.getUnavailableText();

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