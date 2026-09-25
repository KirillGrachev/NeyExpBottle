package eu.neydev.expbottle.util;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Определяет версию ядра один раз при загрузке класса.
 *
 * <p>Старый подход с разбором {@code Bukkit.getServer().getClass().getPackage().getName()}
 * ломается на Paper (пакет не содержит версии) и выполняется на каждый вызов цвета —
 * здесь версия кешируется в статических полях.</p>
 */
public final class ServerVersion {

    private static final String RAW;

    // Не final намеренно: тестам нужно прогонять ветки для ядер без HEX
    private static int major;
    private static int minor;

    static {

        String raw = "unknown";
        int major = 1;
        int minor = 16;

        try {

            raw = Bukkit.getBukkitVersion();          // например: 1.20.4-R0.1-SNAPSHOT
            String numeric = raw.split("-")[0];       // 1.20.4
            String[] parts = numeric.split("\\.");

            major = Integer.parseInt(parts[0]);
            minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        } catch (Throwable ignored) {
            // Ядро ещё не инициализировано (юнит-тесты) — работаем со значением по умолчанию
        }

        RAW = raw;
        ServerVersion.major = major;
        ServerVersion.minor = minor;

    }

    /**
     * Подменяет версию для тестов веток совместимости.
     *
     * @param testMajor мажорная версия
     * @param testMinor минорная версия
     */
    static void setForTests(int testMajor, int testMinor) {
        major = testMajor;
        minor = testMinor;
    }

    private ServerVersion() {
    }

    /**
     * Проверяет, что версия ядра не ниже указанной.
     *
     * @param major мажорная версия (1)
     * @param minor минорная версия (16)
     * @return true если текущее ядро подходит
     */
    public static boolean isAtLeast(int major, int minor) {

        if (ServerVersion.major != major) {
            return ServerVersion.major > major;
        }

        return ServerVersion.minor >= minor;

    }

    /**
     * HEX-цвета клиент понимает начиная с 1.16.
     * На более старых ядрах {@link HexColorUtil} подставляет ближайший legacy-цвет.
     *
     * @return true если HEX поддерживается
     */
    public static boolean isHexSupported() {
        return isAtLeast(1, 16);
    }

    public static int getMajor() {
        return major;
    }

    public static int getMinor() {
        return minor;
    }

    public static @NotNull String getRaw() {
        return RAW;
    }

    @Override
    public String toString() {
        return RAW;
    }
}