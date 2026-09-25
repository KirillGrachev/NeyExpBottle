package eu.neydev.expbottle.util;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Разрешение имён зачарований между поколениями ядер.
 *
 * <p>Часть зачарований переименовывали ({@code DURABILITY} → {@code UNBREAKING}),
 * а в 1.21+ {@code Enchantment} стал интерфейсом-реестром. Здесь имя из конфига
 * приводится к тому, что есть на текущем сервере.</p>
 */
public final class EnchantmentUtil {

    /** Старые имена → новые, чтобы конфиг не зависел от версии. */
    private static final Map<String, String> LEGACY_ALIASES = Map.of(
            "DURABILITY", "UNBREAKING",
            "UNBREAKING", "DURABILITY",
            "DIG_SPEED", "EFFICIENCY",
            "EFFICIENCY", "DIG_SPEED",
            "PROTECTION_ENVIRONMENTAL", "PROTECTION",
            "PROTECTION", "PROTECTION_ENVIRONMENTAL"
    );

    private EnchantmentUtil() {
    }

    /**
     * Ищет зачарование по имени из конфига.
     *
     * @param name имя в любом регистре, старое или новое
     * @return зачарование или {@code null}, если на этом ядре его нет
     */
    public static @Nullable Enchantment resolve(@Nullable String name) {

        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String normalized = name.trim().toUpperCase(Locale.ROOT);

        Enchantment enchantment = find(normalized);

        if (enchantment != null) {
            return enchantment;
        }

        String alias = LEGACY_ALIASES.get(normalized);
        return alias == null ? null : find(alias);

    }

    /**
     * Сначала ищем по ключу реестра ({@code minecraft:unbreaking}): этот путь
     * работает и на старых ядрах, и на новых, где {@code Enchantment} стал
     * интерфейсом. Legacy-имена и getByName — запасной вариант.
     */
    private static @Nullable Enchantment find(@NotNull String name) {

        Enchantment byKey = findByKey(name);

        if (byKey != null) {
            return byKey;
        }

        try {
            return Enchantment.getByName(name);
        } catch (Throwable ignored) {
            return null;
        }

    }

    @SuppressWarnings("deprecation")
    private static @Nullable Enchantment findByKey(@NotNull String name) {

        try {

            String normalized = name.trim().toLowerCase(Locale.ROOT);

            NamespacedKey key = normalized.contains(":")
                    ? NamespacedKey.fromString(normalized)
                    : NamespacedKey.minecraft(normalized);

            if (key == null) {
                return null;
            }

            return Enchantment.getByKey(key);

        } catch (Throwable ignored) {
            return null;
        }

    }
}