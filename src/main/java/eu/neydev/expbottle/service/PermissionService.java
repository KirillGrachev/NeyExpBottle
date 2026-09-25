package eu.neydev.expbottle.service;

import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.util.ValueResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Проверка прав с учётом общего выключателя системы прав.
 */
public class PermissionService {

    private final PluginConfig config;

    public PermissionService(@NotNull PluginConfig config) {
        this.config = config;
    }

    /**
     * Проверяет, закрыт ли отправителю общий доступ к командам плагина.
     *
     * <p>Инверсия {@link #has}: guard-клаузулы команд читаются без двойного
     * отрицания.</p>
     *
     * @param sender отправитель
     * @return true если доступ запрещён
     */
    public boolean useDenied(@NotNull CommandSender sender) {
        return !has(sender, config.getPermissionUse());
    }

    /**
     * Проверяет, закрыт ли игроку обмен опыта на бутылки.
     *
     * <p>Инверсия {@link #has} по тому же правилу, что и
     * {@link #useDenied}.</p>
     *
     * @param sender отправитель
     * @return true если обмен запрещён
     */
    public boolean exchangeDenied(@NotNull CommandSender sender) {
        return !has(sender, config.getPermissionExchange());
    }

    public boolean canAdmin(@NotNull CommandSender sender) {
        return has(sender, config.getPermissionAdmin());
    }

    public boolean hasBypassCooldown(@NotNull CommandSender sender) {
        return has(sender, config.getPermissionBypassCooldown());
    }

    /**
     * Проверяет произвольное право (используется для прав конкретного меню).
     *
     * @param sender     отправитель
     * @param permission имя права, пустая строка означает «проверка выключена»
     * @return true если доступ разрешён
     */
    public boolean has(@NotNull CommandSender sender, @Nullable String permission) {

        if (!config.arePermissionsEnabled()) {
            return true;
        }

        if (ValueResolver.isBlank(permission)) {
            return true;
        }

        if (config.isOpBypassEnabled()) {
            return sender.hasPermission(permission);
        }

        return strictHas(sender, permission);

    }

    /**
     * Строгая проверка для {@code op_bypass: false}: OP и консоль преимуществ
     * не получают. Право есть, только если оно выдано явно (attachment) или
     * его {@code default} в plugin.yml работает для не-OP игрока.
     */
    private boolean strictHas(@NotNull CommandSender sender, @NotNull String permission) {

        // У консоли нет и не может быть attachments, опрашивать её не зачем
        if (!(sender instanceof ConsoleCommandSender)) {

            try {

                for (PermissionAttachmentInfo info : sender.getEffectivePermissions()) {

                    if (info.getPermission().equalsIgnoreCase(permission)) {
                        return info.getValue();
                    }

                }

            } catch (Throwable ignored) {
                // Экзотическое ядро не отдаёт эффективные права — уходим в default
            }

        }

        Permission registered = Bukkit.getPluginManager().getPermission(permission);
        return registered != null && registered.getDefault().getValue(false);

    }
}