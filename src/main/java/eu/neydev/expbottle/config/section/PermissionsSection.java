package eu.neydev.expbottle.config.section;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Секция {@code settings.permissions}: узлы прав и режимы проверок.
 *
 * @param enabled        включать ли проверки прав
 * @param use            узел права использования
 * @param exchange       узел права обмена
 * @param admin          узел права админских подкоманд
 * @param bypassCooldown узел права обхода кулдауна
 * @param opBypass       дают ли op-права обход всех проверок
 */
public record PermissionsSection(boolean enabled, @NotNull String use, @NotNull String exchange,
                                 @NotNull String admin, @NotNull String bypassCooldown, boolean opBypass) {

    private static final String PATH_ENABLED = "settings.permissions.enabled";
    private static final String PATH_USE = "settings.permissions.use";
    private static final String PATH_EXCHANGE = "settings.permissions.exchange";
    private static final String PATH_ADMIN = "settings.permissions.admin";
    private static final String PATH_BYPASS_COOLDOWN = "settings.permissions.bypass_cooldown";
    private static final String PATH_OP_BYPASS = "settings.permissions.op_bypass";

    public static @NotNull PermissionsSection read(@NotNull FileConfiguration config) {

        return new PermissionsSection(
                config.getBoolean(PATH_ENABLED, true),
                config.getString(PATH_USE, "expbottle.use"),
                config.getString(PATH_EXCHANGE, "expbottle.exchange"),
                config.getString(PATH_ADMIN, "expbottle.admin"),
                config.getString(PATH_BYPASS_COOLDOWN, "expbottle.bypass.cooldown"),
                config.getBoolean(PATH_OP_BYPASS, false)
        );

    }
}
