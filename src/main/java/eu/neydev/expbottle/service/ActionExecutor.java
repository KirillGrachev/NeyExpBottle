package eu.neydev.expbottle.service;

import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.gui.action.ClickAction;
import eu.neydev.expbottle.model.ExchangeOutcome;
import eu.neydev.expbottle.model.ExchangeResult;
import eu.neydev.expbottle.registry.BottleRegistry;
import eu.neydev.expbottle.registry.BottleTier;
import eu.neydev.expbottle.util.HexColorUtil;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Исполнитель действий меню: {@code [message]}, {@code [console]}, {@code [sound]},
 * {@code [exchange]}, {@code [open]}, {@code [refresh]}, {@code [close]}, {@code [chat]},
 * {@code [broadcast]}, {@code [player]}, {@code [none]}.
 *
 * <p>Получает {@link PluginServices} целиком намеренно: действию может понадобиться
 * любой сервис, а жёсткая ссылка на {@link MenuService} создала бы цикл
 * (меню открывает действия, действия открывают меню).</p>
 */
public class ActionExecutor {

    private static final String SOUND_SEPARATOR = ":";

    private final PluginServices services;
    private final PluginConfig config;
    private final MessageService messageService;
    private final SoundService soundService;
    private final ExchangeService exchangeService;
    private final PlaceholderService placeholderService;
    private final BottleRegistry bottleRegistry;
    private final DiagnosticsService diagnosticsService;

    public ActionExecutor(@NotNull PluginServices services) {

        this.services = services;
        this.config = services.getConfigManager();
        this.messageService = services.getMessageService();
        this.soundService = services.getSoundService();
        this.exchangeService = services.getExchangeService();
        this.placeholderService = services.getPlaceholderService();
        this.bottleRegistry = services.getBottleRegistry();
        this.diagnosticsService = services.getDiagnosticsService();

    }

    /**
     * Выполняет список действий по порядку.
     *
     * @param player       игрок
     * @param actions      действия
     * @param placeholders значения плейсхолдеров
     */
    public void execute(@NotNull Player player, @NotNull List<ClickAction> actions,
                        @NotNull Placeholders placeholders) {

        for (ClickAction action : actions) {
            execute(player, action, placeholders);
        }

    }

    /**
     * Выполняет одно действие.
     *
     * @param player       игрок
     * @param action       действие
     * @param placeholders значения плейсхолдеров
     */
    public void execute(@NotNull Player player, @NotNull ClickAction action,
                        @NotNull Placeholders placeholders) {

        String argument = placeholders.apply(placeholderService.process(player, action.argument()));

        switch (action.type()) {

            case MESSAGE -> messageService.sendText(player, argument);
            case BROADCAST -> Bukkit.broadcastMessage(HexColorUtil.color(argument));
            case CLOSE -> services.getMenuService().close(player);
            case CONSOLE -> dispatch(Bukkit.getConsoleSender(), argument);
            case PLAYER -> dispatch(player, argument);
            case CHAT -> player.chat(argument);
            case SOUND -> playSound(player, argument);
            case REFRESH -> services.getMenuService().refresh(player);
            case OPEN -> services.getMenuService().open(player, argument);
            case EXCHANGE -> handleExchange(player, argument);
            case NONE -> diagnosticsService.debug("[none] - no-op action for " + player.getName());

        }

    }

    /**
     * Обмен уровней на бутылку: {@code [exchange] 5} или {@code [exchange] tier_5}.
     */
    private void handleExchange(@NotNull Player player, @NotNull String argument) {

        int levels = resolveLevels(argument);

        if (levels <= 0) {

            messageService.send(player, MessageKey.INVALID_AMOUNT, Placeholders.create()
                    .set("levels", argument)
                    .set("max_levels", config.getMaxBottleLevels()));
            return;

        }

        handleResult(player, exchangeService.exchange(player, levels));

    }

    /**
     * Реакция на результат обмена: сообщение, звук, счётчики.
     *
     * @param player игрок
     * @param result результат обмена
     */
    public void handleResult(@NotNull Player player, @NotNull ExchangeResult result) {

        Placeholders placeholders = placeholderService.forPlayer(player)
                .merge(placeholderService.forLevels(result.levels()))
                .set("used_bottles", result.usedBottles())
                .set("cooldown", services.getCooldownService().getRemainingSeconds(player));

        if (result.isSuccess()) {

            messageService.send(player, MessageKey.BOTTLE_CREATED, placeholders);
            soundService.play(player, config.getExchangeSound());
            return;

        }

        diagnosticsService.incrementExchangesDenied();

        if (result.outcome() == ExchangeOutcome.CANCELLED) {
            return;
        }

        sendFailure(player, result.outcome(), placeholders);
        soundService.play(player, config.getFailSound());

    }

    private void sendFailure(@NotNull Player player, @NotNull ExchangeOutcome outcome,
                             @NotNull Placeholders placeholders) {

        switch (outcome) {

            case PLUGIN_DISABLED -> messageService.send(player, MessageKey.PLUGIN_DISABLED, placeholders);
            case INVALID_AMOUNT -> messageService.send(player, MessageKey.INVALID_AMOUNT, placeholders);
            case NO_PERMISSION -> messageService.send(player, MessageKey.NO_PERMISSION, placeholders);
            case ON_COOLDOWN -> messageService.send(player, MessageKey.ON_COOLDOWN, placeholders);
            case NOT_ENOUGH_LEVELS -> messageService.send(player, MessageKey.NOT_ENOUGH_LEVELS, placeholders);
            case NOT_ENOUGH_BOTTLES -> messageService.send(player, MessageKey.NOT_ENOUGH_BOTTLES, placeholders);
            case CANCELLED, SUCCESS -> diagnosticsService.debug("Unexpected exchange outcome: " + outcome);

        }

    }

    /**
     * Аргумент может быть идентификатором кнопки обмена или числом уровней.
     *
     * @param argument текст из конфига
     * @return количество уровней или -1, если значение некорректно
     */
    private int resolveLevels(@NotNull String argument) {

        Optional<BottleTier> tier = bottleRegistry.byId(argument);

        if (tier.isPresent()) {
            return tier.get().levels();
        }

        try {

            int levels = Integer.parseInt(argument.trim());

            if (levels < 1 || levels > config.getMaxBottleLevels()) {
                return -1;
            }

            return levels;

        } catch (NumberFormatException exception) {
            return -1;
        }

    }

    /**
     * Звук в формате {@code ИМЯ[:громкость[:высота]]}.
     */
    private void playSound(@NotNull Player player, @NotNull String argument) {

        String[] parts = argument.split(SOUND_SEPARATOR);

        if (parts.length == 0 || parts[0].trim().isEmpty()) {
            return;
        }

        float volume = parseFloat(parts, 1, 1.0f);
        float pitch = parseFloat(parts, 2, 1.0f);

        soundService.play(player, parts[0].trim(), volume, pitch);

    }

    private float parseFloat(String @NotNull [] parts, int index, float fallback) {

        if (parts.length <= index) {
            return fallback;
        }

        try {
            return Float.parseFloat(parts[index].trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }

    }

    /**
     * Выполняет команду от имени консоли или игрока.
     * Файлы меню считаются доверенной конфигурацией: {@code [console]} даёт полный доступ.
     */
    private void dispatch(@NotNull CommandSender sender, @NotNull String command) {

        if (command.isEmpty()) {
            return;
        }

        String normalized = command.startsWith("/") ? command.substring(1) : command;

        try {
            Bukkit.dispatchCommand(sender, normalized);
        } catch (Throwable throwable) {
            diagnosticsService.warning("Command '" + normalized + "' not met: " + throwable.getMessage());
        }

    }
}
