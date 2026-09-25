package eu.neydev.expbottle.support.skull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сборка authlib-профиля игрока с текстурой.
 *
 * <p>Создаёт {@code GameProfile}, его свойство {@code Property("textures", ...)}
 * и — для ядер 1.20.5+ — {@code ResolvableProfile}, набор полей которого
 * менялся между версиями. Всё через отражение: классы authlib и NMS недоступны
 * на этапе компиляции против старой Bukkit API.</p>
 */
final class GameProfileFactory {

    static final String TEXTURE_PROPERTY = "textures";
    static final String DEFAULT_OWNER_NAME = "NeyExpBottle";

    private GameProfileFactory() {
    }

    static @NotNull String ownerName(@Nullable String owner) {
        return owner == null || owner.isEmpty() ? DEFAULT_OWNER_NAME : owner;
    }

    /**
     * Стабильный UUID профиля: производная от текста текстуры.
     *
     * <p>Меню перерисовывается раз в секунду, и со случайным UUID клиент считал
     * голову новым игроком при каждом обновлении: скин запрашивался заново, и
     * голова «мигала» чужими текстурами из кэша. С производным UUID клиентский
     * кэш узнаёт голову и берёт скин без повторных запросов.</p>
     *
     * @param payload base64-блок профиля с текстурой
     * @return один и тот же UUID для одной и той же текстуры
     */
    static @NotNull UUID stableUuid(@NotNull String payload) {
        return UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
    }

    static @NotNull Object newGameProfile(@NotNull String payload, @Nullable String owner) throws Exception {

        Class<?> profileClass = Reflect.firstClass("com.mojang.authlib.GameProfile",
                "net.minecraft.util.com.mojang.authlib.GameProfile");

        if (profileClass == null) {
            // Недостижимо в тестах: стабы authlib лежат в test classpath
            throw new IllegalStateException("GameProfile class is unavailable");
        }

        Object gameProfile = profileClass.getConstructor(UUID.class, String.class)
                .newInstance(stableUuid(payload), ownerName(owner));

        putTextureProperty(gameProfile, payload);

        return gameProfile;

    }

    static void putTextureProperty(@NotNull Object gameProfile, @NotNull String payload) throws Exception {

        Object property = newAuthlibProperty(payload);

        if (property == null) {
            throw new IllegalStateException("authlib Property is unavailable");
        }

        Object propertyMap = Reflect.invokeAny(gameProfile, "getProperties");

        if (propertyMap == null) {
            throw new IllegalStateException("GameProfile properties are unavailable");
        }

        propertyMap.getClass().getMethod("put", Object.class, Object.class)
                .invoke(propertyMap, TEXTURE_PROPERTY, property);

    }

    static @Nullable Object newAuthlibProperty(@NotNull String payload) {

        Class<?> propertyClass = Reflect.firstClass("com.mojang.authlib.properties.Property",
                "net.minecraft.util.com.mojang.authlib.properties.Property");

        if (propertyClass == null) {
            return null;
        }

        for (Constructor<?> constructor : propertyClass.getConstructors()) {

            Class<?>[] types = constructor.getParameterTypes();

            try {

                if (types.length == 2 && types[0] == String.class && types[1] == String.class) {
                    return constructor.newInstance(TEXTURE_PROPERTY, payload);
                }

                if (types.length == 3 && types[0] == String.class && types[1] == String.class) {
                    return constructor.newInstance(TEXTURE_PROPERTY, payload, null);
                }

            } catch (Throwable ignored) {
                // Пробуем следующую сигнатуру конструктора
            }

        }

        return null;

    }

    /**
     * Создаёт {@code ResolvableProfile} — набор полей менялся между 1.20.5 и 1.21.x.
     */
    static @Nullable Object newResolvableProfile(@NotNull String payload, @Nullable String owner)
            throws Exception {

        Object gameProfile = newGameProfile(payload, owner);

        Class<?> resolvableClass = Reflect.firstClass("net.minecraft.world.item.component.ResolvableProfile",
                "net.minecraft.server.level.ResolvableProfile");

        if (resolvableClass == null) {
            return null;
        }

        for (Constructor<?> constructor : resolvableClass.getDeclaredConstructors()) {

            Class<?>[] types = constructor.getParameterTypes();
            Object[] arguments = new Object[types.length];

            if (types.length == 0 || !fillArguments(types, arguments, gameProfile)) {
                continue;
            }

            try {

                constructor.setAccessible(true);
                return constructor.newInstance(arguments);

            } catch (Throwable ignored) {
                // Пробуем следующий конструктор
            }

        }

        return null;

    }

    static boolean fillArguments(@NotNull Class<?>[] types, Object @NotNull [] arguments,
                                 @NotNull Object gameProfile) {

        boolean profileUsed = false;

        for (int index = 0; index < types.length; index++) {

            Class<?> type = types[index];

            if (type.isAssignableFrom(gameProfile.getClass())) {

                arguments[index] = gameProfile;
                profileUsed = true;

            } else if (type.getName().endsWith("CompletableFuture")) {

                arguments[index] = CompletableFuture.completedFuture(gameProfile);
                profileUsed = true;

            } else if (type == boolean.class) {

                arguments[index] = Boolean.TRUE;

            } else if (type == int.class) {

                arguments[index] = 0;

            } else {
                return false;
            }

        }

        return profileUsed;

    }
}