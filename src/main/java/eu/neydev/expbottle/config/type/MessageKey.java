package eu.neydev.expbottle.config.type;

import org.jetbrains.annotations.NotNull;

/**
 * Ключи сообщений: путь в config.yml больше нельзя опечатать в коде.
 *
 * <p>В старой версии пути передавались строками ({@code getMessage("reload-start")}),
 * из-за чего часть сообщений молча отсутствовала.</p>
 */
public enum MessageKey {

    ONLY_PLAYERS("messages.only_players"),
    NO_PERMISSION("messages.no_permission"),
    PLUGIN_DISABLED("messages.plugin_disabled"),

    MENU_NOT_FOUND("messages.menu_not_found"),
    MENU_USAGE("messages.menu_usage"),
    EXCHANGE_USAGE("messages.exchange_usage"),
    MENU_DENIED("messages.menu_denied"),

    NOT_ENOUGH_LEVELS("messages.not_enough_levels"),
    NOT_ENOUGH_BOTTLES("messages.not_enough_bottles"),
    ON_COOLDOWN("messages.on_cooldown"),
    EXCHANGE_CANCELLED("messages.exchange_cancelled"),

    BOTTLE_CREATED("messages.bottle_created"),
    BOTTLE_BROKEN("messages.bottle_broken"),
    BOTTLE_FORGED("messages.bottle_forged"),

    RELOAD_START("messages.reload_start"),
    RELOAD_SUCCESS("messages.reload_success"),
    RELOAD_TIME("messages.reload_time"),
    RELOAD_ERROR("messages.reload_error"),

    GIVE_SUCCESS("messages.give_success"),
    GIVE_RECEIVED("messages.give_received"),
    GIVE_USAGE("messages.give_usage"),
    PLAYER_NOT_FOUND("messages.player_not_found"),
    INVALID_TIER("messages.invalid_tier"),
    INVALID_AMOUNT("messages.invalid_amount"),

    INFO("messages.info"),
    HELP("messages.help");

    private final String path;

    MessageKey(@NotNull String path) {
        this.path = path;
    }

    public @NotNull String getPath() {
        return path;
    }
}
