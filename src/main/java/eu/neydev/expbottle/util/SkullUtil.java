package eu.neydev.expbottle.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Головы игроков в предметах меню.
 *
 * <p>Поддерживаются два способа, как в DeluxeMenus:</p>
 * <ul>
 *     <li>{@code skull_owner: Ник} — голова конкретного игрока;</li>
 *     <li>{@code skull_texture: base64} — произвольная текстура из bases64-блока
 *         или ссылки на неё.</li>
 * </ul>
 *
 * <p>Текстура ставится через {@code PlayerProfile} (1.18+), а на старых ядрах —
 * отражением в профиль скалл-меты. Любая ошибка отката не ломает предмет:
 * голова просто останется стандартной.</p>
 */
public final class SkullUtil {

    private static final String TEXTURE_PROPERTY = "textures";

    private SkullUtil() {
    }

    /**
     * Применяет владельца и текстуру к мете головы.
     *
     * @param meta    мета предмета (ожидается SkullMeta)
     * @param owner   ник владельца или {@code null}
     * @param texture base64-текстура, ссылка или {@code null}
     * @param logger  логгер для предупреждений
     */
    @SuppressWarnings("deprecation")
    public static void apply(@NotNull SkullMeta meta, @Nullable String owner,
                             @Nullable String texture, @NotNull Logger logger) {

        if (owner != null && !owner.trim().isEmpty()) {

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner.trim());
            meta.setOwningPlayer(offlinePlayer);

        }

        if (texture == null || texture.trim().isEmpty()) {
            return;
        }

        String base64 = toBase64Payload(texture.trim());

        if (applyByProfile(meta, base64)) {
            return;
        }

        if (applyByReflection(meta, base64)) {
            return;
        }

        logger.warning("Failed to apply skull_texture on this core - the head stays default");

    }

    /**
     * Ссылку на текстуру превращаем в готовый base64-блок профиля.
     */
    private static @NotNull String toBase64Payload(@NotNull String texture) {

        boolean alreadyPayload = texture.startsWith("{") || texture.contains("\"textures\"");

        if (alreadyPayload) {
            return texture;
        }

        String url = texture.startsWith("http") ? texture : "https://textures.minecraft.net/texture/" + texture;
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";

        return Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));

    }

    /**
     * Штатный путь 1.18+: PlayerProfile с свойством textures.
     */
    private static boolean applyByProfile(@NotNull SkullMeta meta, @NotNull String base64) {

        try {

            Method createProfile = Bukkit.class.getMethod("createPlayerProfile", UUID.class);
            Object profile = createProfile.invoke(null, UUID.randomUUID());

            Class<?> profilePropertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
            Object property = profilePropertyClass
                    .getConstructor(String.class, String.class)
                    .newInstance(TEXTURE_PROPERTY, base64);

            Method setProperties = profile.getClass().getMethod("setProperties", java.util.Collection.class);
            setProperties.invoke(profile, java.util.List.of(property));

            Method setOwnerProfile = SkullMeta.class.getMethod("setOwnerProfile",
                    Class.forName("org.bukkit.profile.PlayerProfile"));
            setOwnerProfile.invoke(meta, profile);

            return true;

        } catch (Throwable ignored) {
            return false;
        }

    }

    /**
     * Запасной путь для ядер 1.16—1.17: GameProfile из authlib через поле меты.
     */
    private static boolean applyByReflection(@NotNull SkullMeta meta, @NotNull String base64) {

        try {

            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");

            Object profile = gameProfileClass
                    .getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), null);

            Object property = propertyClass
                    .getConstructor(String.class, String.class)
                    .newInstance(TEXTURE_PROPERTY, base64);

            Method getProperties = gameProfileClass.getMethod("getProperties");
            Object propertyMap = getProperties.invoke(profile);

            Method put = propertyMap.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(propertyMap, TEXTURE_PROPERTY, property);

            java.lang.reflect.Field profileField = findProfileField(meta.getClass());

            if (profileField == null) {
                return false;
            }

            profileField.setAccessible(true);
            profileField.set(meta, profile);

            return true;

        } catch (Throwable ignored) {
            return false;
        }

    }

    private static @Nullable java.lang.reflect.Field findProfileField(@NotNull Class<?> type) {

        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {

            for (java.lang.reflect.Field field : current.getDeclaredFields()) {

                if (field.getType().getSimpleName().equals("GameProfile")) {
                    return field;
                }

            }

        }

        return null;

    }
}
