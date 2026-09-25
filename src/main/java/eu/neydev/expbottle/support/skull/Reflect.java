package eu.neydev.expbottle.support.skull;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Общие отражательные приёмчики, не знающие про голову.
 *
 * <p>Поиск классов (в том числе craftbukkit-пакетов с номером версии), полей по
 * имени типа и безопасные вызовы методов, имена которых менялись между версиями
 * ядра. Любая ошибка глотается и возвращается {@code null}/{@code false} —
 * вызывающий код просто пробует следующий вариант.</p>
 */
final class Reflect {

    private Reflect() {
    }

    /**
     * Ищет craftbukkit-класс: сначала в пакете ядра, затем по известным версиям.
     *
     * <p>Вызывается только пока подбор способа не завершён, поэтому отдельный
     * кеш здесь не нужен.</p>
     */
    static @Nullable Class<?> resolveCraftClass(@NotNull String simpleName) {

        try {

            String base = Bukkit.getServer().getClass().getPackage().getName();
            return Class.forName(base + "." + simpleName);

        } catch (Throwable ignored) {
            // Ниже пробуем пакеты с номером версии
        }

        for (String suffix : new String[] {"v1_21_R4", "v1_21_R3", "v1_21_R2", "v1_21_R1", "v1_20_R4",
                "v1_20_R3", "v1_20_R2", "v1_20_R1", "v1_19_R3", "v1_18_R2", "v1_17_R1", "v1_16_R3",
                "v1_16_R2", "v1_16_R1"}) {

            try {
                return Class.forName("org.bukkit.craftbukkit." + suffix + "." + simpleName);
            } catch (Throwable ignored) {
                // Пробуем следующую версию пакета
            }

        }

        return null;

    }

    static @Nullable Class<?> firstClass(@NotNull String... names) {

        for (String name : names) {

            try {
                return Class.forName(name);
            } catch (Throwable ignored) {
                // Пробуем следующее имя класса
            }

        }

        return null;

    }

    static @Nullable Object findFieldValue(@NotNull Object target, @NotNull String simpleTypeName) {

        Field field = findFieldOfType(target.getClass(), simpleTypeName);

        if (field == null) {
            return null;
        }

        try {

            field.setAccessible(true);
            return field.get(target);

        } catch (Throwable ignored) {
            return null;
        }

    }

    static @Nullable Field findFieldOfType(@NotNull Class<?> type, @NotNull String... simpleTypeNames) {

        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {

            for (Field field : current.getDeclaredFields()) {

                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                String simpleName = field.getType().getSimpleName();

                for (String expected : simpleTypeNames) {

                    if (simpleName.equals(expected)) {
                        return field;
                    }

                }

            }

        }

        return null;

    }

    /**
     * Вызывает первый метод без аргументов, который найдётся по имени.
     */
    static @Nullable Object invokeAny(@NotNull Object target, @NotNull String... names) {

        for (String name : names) {

            try {
                return target.getClass().getMethod(name).invoke(target);
            } catch (Throwable ignored) {
                // Пробуем следующее имя метода
            }

        }

        return null;

    }

    /**
     * Ищет публичный метод, которого может не быть в compile-time API.
     *
     * <p>Ядра разных семейств добавляют методы ({@code createProfile},
     * {@code setOwnerProfile}) без оглядки на spigot-api, поэтому прямой вызов
     * {@code getMethod} с литеральным именем IDE считает ошибкой, хотя на
     * целевом ядре метод есть. Поиск собран здесь: имя приходит параметром,
     * а отсутствующий метод честно прилетает исключением вызывающему способу.</p>
     *
     * @param type           класс, в котором ищется метод
     * @param name           имя метода
     * @param parameterTypes типы параметров метода
     * @return найденный метод
     * @throws NoSuchMethodException если ядро не содержит метод
     */
    static @NotNull Method requireMethod(@NotNull Class<?> type, @NotNull String name,
                                         @NotNull Class<?>... parameterTypes) throws NoSuchMethodException {
        return type.getMethod(name, parameterTypes);
    }

    /**
     * Вызывает setter с одним аргументом, перебирая варианты имени метода.
     */
    static void invokeSetter(@NotNull Object target, @NotNull Class<?> argumentType,
                             String @NotNull [] names, @NotNull Object argument) {

        for (String candidate : names) {

            try {

                target.getClass().getMethod(candidate, argumentType).invoke(target, argument);
                return;

            } catch (Throwable ignored) {
                // Пробуем следующее имя метода
            }

        }

    }

    /**
     * Вызывает метод с ключом-строкой и аргументом заданного типа, перебирая
     * варианты имени: {@code put(String, Tag)}, {@code set(String, NBTBase)}…
     *
     * @return true если вызов прошёл
     */
    static boolean invokeAny(@NotNull Object target, @NotNull Class<?> argumentType,
                             String @NotNull [] names, @NotNull String key, @NotNull Object value) {

        for (String candidate : names) {

            try {

                target.getClass().getMethod(candidate, String.class, argumentType).invoke(target, key, value);
                return true;

            } catch (Throwable ignored) {
                // Пробуем следующее имя метода
            }

        }

        return false;

    }
}