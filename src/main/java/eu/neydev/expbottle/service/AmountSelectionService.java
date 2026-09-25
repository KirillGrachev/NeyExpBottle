package eu.neydev.expbottle.service;

import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.config.type.AmountSettings;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Выбранное игроком количество бутылок для обмена.
 *
 * <p>Состояние живёт в памяти и осознанно не сохраняется между заходами:
 * количество — такая же временная настройка сессии, как открытое окно.
 * При выходе игрока запись убирается {@link #clear(UUID)}.</p>
 */
public class AmountSelectionService {

    private static final String TOKEN_ALL = "all";

    private final PluginConfig config;
    private final DiagnosticsService diagnosticsService;
    private final Map<UUID, Integer> selections = new ConcurrentHashMap<>();

    public AmountSelectionService(@NotNull PluginConfig config,
                                  @NotNull DiagnosticsService diagnosticsService) {
        this.config = config;
        this.diagnosticsService = diagnosticsService;
    }

    /**
     * Текущее выбранное количество: сохранённое или первый вариант из конфига.
     *
     * @param player игрок
     * @return количество либо {@link ExchangeService#AMOUNT_ALL}
     */
    public int getSelected(@NotNull Player player) {
        return selections.getOrDefault(player.getUniqueId(), parse(options().get(0)));
    }

    /**
     * Переключает количество на следующий вариант списка (по кругу).
     *
     * @param player игрок
     * @return новое выбранное количество
     */
    public int cycle(@NotNull Player player) {

        List<String> amounts = options();
        int index = indexOf(amounts, getSelected(player));
        int next = parse(amounts.get((index + 1) % amounts.size()));

        selections.put(player.getUniqueId(), next);
        return next;

    }

    /**
     * Выбирает количество по токену из конфига ({@code 1}, {@code 16}, {@code all}).
     * Неизвестные токены игнорируются: кнопка меню не должна уметь ставить
     * количество, которого нет в переключателе.
     *
     * @param player игрок
     * @param raw    токен варианта
     */
    public void select(@NotNull Player player, @NotNull String raw) {

        String token = normalize(raw);
        List<String> amounts = options();

        if (!amounts.contains(token)) {

            diagnosticsService.debug("Amount option '" + raw + "' is not in settings.amount.amounts - ignored");
            return;

        }

        selections.put(player.getUniqueId(), parse(token));

    }

    /**
     * Убирает запись игрока (выход с сервера).
     *
     * @param uuid UUID игрока
     */
    public void clear(@NotNull UUID uuid) {
        selections.remove(uuid);
    }

    /**
     * Подпись варианта для текстов: число или {@code all_label} из конфига.
     *
     * @param amount количество либо {@link ExchangeService#AMOUNT_ALL}
     * @return подпись
     */
    public @NotNull String label(int amount) {

        if (amount == ExchangeService.AMOUNT_ALL) {
            return config.getAmount().allLabel();
        }

        return String.valueOf(amount);

    }

    /**
     * Токен варианта для действий и условий: число или {@code all}.
     *
     * @param amount количество либо {@link ExchangeService#AMOUNT_ALL}
     * @return токен
     */
    public @NotNull String raw(int amount) {

        if (amount == ExchangeService.AMOUNT_ALL) {
            return TOKEN_ALL;
        }

        return String.valueOf(amount);

    }

    /**
     * Единый разбор токена количества: им пользуются и переключатель, и
     * аргумент действия {@code [exchange]}, и чат-команда — границы ошибки
     * поэтому одни и те же во всех входах.
     *
     * @param raw токен: {@code all} либо число из 1..{@link ExchangeService#MAX_AMOUNT}
     * @return количество, {@link ExchangeService#AMOUNT_ALL} или 0, если токен некорректен
     */
    public static int parseAmount(@NotNull String raw) {

        String token = raw.trim();

        if (token.equalsIgnoreCase(TOKEN_ALL)) {
            return ExchangeService.AMOUNT_ALL;
        }

        try {

            int amount = Integer.parseInt(token);
            return amount < 1 || amount > ExchangeService.MAX_AMOUNT ? 0 : amount;

        } catch (NumberFormatException exception) {
            return 0;
        }

    }

    /**
     * Разбирает токен варианта из конфига. Список {@code amounts} приходит
     * уже проверенным {@code ConfigManager}, поэтому ноль здесь означает
     * разве что устаревший токен — на него даём единицу.
     *
     * @param token токен варианта
     * @return количество либо {@link ExchangeService#AMOUNT_ALL}
     */
    public static int parse(@NotNull String token) {

        int amount = parseAmount(token);
        return amount == 0 ? 1 : amount;

    }

    private @NotNull List<String> options() {

        AmountSettings settings = config.getAmount();
        return settings.amounts().isEmpty() ? List.of(TOKEN_ALL) : settings.amounts();

    }

    private int indexOf(@NotNull List<String> amounts, int amount) {

        for (int index = 0; index < amounts.size(); index++) {

            if (parse(amounts.get(index)) == amount) {
                return index;
            }

        }

        return -1;

    }

    private @NotNull String normalize(@NotNull String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}