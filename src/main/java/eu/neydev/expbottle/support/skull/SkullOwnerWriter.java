package eu.neydev.expbottle.support.skull;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Перенос владельца и текстуры на саму голову.
 *
 * <p>Два уровня: штатные методы меты ({@code setOwningPlayer}, {@code setOwnerProfile})
 * и запасной путь — прямая запись NBT-тега {@code SkullOwner} в NMS-копию предмета
 * с последующим зеркальным отражением обратно в Bukkit. Второй путь возвращает новый
 * предмет, потому что тег читается ядром при создании меты.</p>
 */
final class SkullOwnerWriter {

    private SkullOwnerWriter() {
    }

    /**
     * Ставит владельца штатным способом меты — работает на любом ядре.
     */
    @SuppressWarnings("deprecation")
    static void applyOwner(@NotNull SkullMeta meta, @Nullable String owner) {

        if (owner != null && !owner.isEmpty()) {

            try {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            } catch (Throwable ignored) {
                // Владелец не критичен: текстура важнее
            }

        }

    }

    /**
     * Ставит профиль в мету: на части ядер метод ничего не возвращает,
     * поэтому «успех» означает отсутствие исключения.
     */
    static boolean setOwnerProfile(@NotNull SkullMeta meta, @NotNull Object profile) throws Exception {

        Method setOwnerProfile = Reflect.requireMethod(SkullMeta.class, "setOwnerProfile",
                Class.forName("org.bukkit.profile.PlayerProfile"));

        Object result = setOwnerProfile.invoke(meta, profile);
        return !(result instanceof Boolean value) || value;

    }

    /**
     * Прописывает в тег предмета структуру {@code SkullOwner} с текстурой.
     */
    static boolean writeSkullOwner(@NotNull Object tag, @NotNull Class<?> tagClass, @NotNull String payload,
                                   @NotNull String owner) {

        try {

            Constructor<?> constructor = tagClass.getConstructor();

            Object ownerTag = constructor.newInstance();
            Object properties = constructor.newInstance();
            Object textures = constructor.newInstance();
            Object texture = constructor.newInstance();

            String[] stringMethods = {"setString", "a", "putString"};
            String[] tagMethods = {"set", "a", "put"};

            boolean filled = Reflect.invokeAny(ownerTag, String.class, stringMethods, "Id", GameProfileFactory.stableUuid(payload).toString())
                    && Reflect.invokeAny(ownerTag, String.class, stringMethods, "Name", owner)
                    && Reflect.invokeAny(texture, String.class, stringMethods, "Value", payload);

            if (!filled) {
                return false;
            }

            return Reflect.invokeAny(textures, tagClass, tagMethods, GameProfileFactory.TEXTURE_PROPERTY, texture)
                    && Reflect.invokeAny(properties, tagClass, tagMethods, GameProfileFactory.TEXTURE_PROPERTY, textures)
                    && Reflect.invokeAny(ownerTag, tagClass, tagMethods, "Properties", properties)
                    && Reflect.invokeAny(tag, tagClass, tagMethods, "SkullOwner", ownerTag);

        } catch (Throwable ignored) {
            return false;
        }

    }

    /**
     * Превращает NMS-копию обратно в предмет Bukkit.
     *
     * <p>Возвращается именно новый предмет: тег читается ядром при создании
     * меты, а обратная запись меты профиль уже не содержит.</p>
     */
    static @Nullable ItemStack mirrorBack(@NotNull Class<?> craftItemStack, @NotNull Object nmsStack) {

        for (String name : new String[] {"asBukkitCopy", "asCraftMirror"}) {

            try {

                Method method = craftItemStack.getMethod(name, nmsStack.getClass());

                if (method.invoke(null, nmsStack) instanceof ItemStack mirrored) {
                    return mirrored;
                }

            } catch (NoSuchMethodException ignored) {
                // Ищем следующий способ обратного преобразования
            } catch (Throwable ignored) {
                return null;
            }

        }

        return null;

    }
}