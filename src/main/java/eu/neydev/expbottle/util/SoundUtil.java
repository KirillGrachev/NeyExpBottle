package eu.neydev.expbottle.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Разрешение имён звуков между поколениями ядер.
 *
 * <p>До 1.21.2 {@code org.bukkit.Sound} — перечисление, и имя из конфига
 * ({@code ENTITY_EXPERIENCE_ORB_PICKUP}) подходит как есть. Начиная с 1.21.2
 * звук стал элементом реестра, {@code valueOf} больше не существует, а ключ
 * выглядит как {@code entity.experience_orb.pickup}.</p>
 *
 * <p>Поэтому звук воспроизводится строковым методом
 * {@code Player#playSound(Location, String, float, float)}, который есть во всех
 * поддерживаемых версиях, а имя заранее приводится к понятному ядру формату.</p>
 */
public final class SoundUtil {

    /** Пустая строка — маркер «звук не найден» (ConcurrentHashMap не хранит null). */
    private static final String UNKNOWN = "";

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private SoundUtil() {
    }

    /**
     * Приводит имя звука из конфига к формату текущего ядра.
     *
     * @param name имя из конфига
     * @return ключ для {@code playSound} или {@code null}, если звук не существует
     */
    public static @Nullable String resolveKey(@Nullable String name) {

        if (ValueResolver.isBlank(name)) {
            return null;
        }

        String key = name.trim().toUpperCase(Locale.ROOT);
        String resolved = CACHE.computeIfAbsent(key, SoundUtil::resolve);

        return resolved.isEmpty() ? null : resolved;

    }

    /**
     * Проверяет, существует ли звук на текущем ядре.
     *
     * @param name имя звука
     * @return true если звук известен серверу
     */
    public static boolean isKnown(@Nullable String name) {
        return resolveKey(name) != null;
    }

    private static @NotNull String resolve(@NotNull String name) {

        if (isEnumConstant(name)) {
            return name;
        }

        String registryKey = name.toLowerCase(Locale.ROOT).replace('_', '.');

        if (isRegistryKey(registryKey)) {
            return registryKey;
        }

        return UNKNOWN;

    }

    /**
     * Ядра до 1.21.2: {@code Sound} — перечисление.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean isEnumConstant(@NotNull String name) {

        try {

            if (!Sound.class.isEnum()) {
                return false;
            }

            return Enum.valueOf((Class<? extends Enum>) Sound.class, name) != null;

        } catch (Throwable ignored) {
            return false;
        }

    }

    /**
     * Ядра 1.21.2+: {@code Sound} — элемент реестра.
     * Реестр ищем отражением, чтобы не требовать новый API при компиляции.
     */
    @SuppressWarnings("deprecation")
    private static boolean isRegistryKey(@NotNull String key) {

        try {

            Class<?> registryClass = Class.forName("org.bukkit.Registry");

            Method getRegistry = registryClass.getMethod("getRegistry", Class.class);
            Object registry = getRegistry.invoke(null, Sound.class);

            if (registry == null) {
                return false;
            }

            Method get = registryClass.getMethod("get", NamespacedKey.class);
            return get.invoke(registry, new NamespacedKey("minecraft", key)) != null;

        } catch (Throwable ignored) {
            return false;
        }

    }
}