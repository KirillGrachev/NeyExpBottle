package eu.neydev.expbottle.support.skull;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Ядра 1.20.5+: профиль лежит в {@code ResolvableProfile} внутри меты.
 *
 * <p>С 1.20.5 Mojang заменили {@code GameProfile} на {@code ResolvableProfile};
 * набор его полей менялся вплоть до 1.21.x, поэтому профиль собирается
 * перебором конструкторов в {@link GameProfileFactory}.</p>
 */
public class ReserializedProfileStrategy implements SkullApplyStrategy {

    @Override
    public @NotNull String id() {
        return "reserialized_profile";
    }

    @Override
    public @Nullable ItemStack apply(@NotNull SkullMeta meta, @NotNull ItemStack item, @NotNull String payload,
                                     @Nullable String owner) throws Exception {

        Field field = Reflect.findFieldOfType(meta.getClass(), "ResolvableProfile");

        if (field == null) {
            return null;
        }

        Object profile = GameProfileFactory.newResolvableProfile(payload, owner);

        if (profile == null) {
            throw new IllegalStateException("ResolvableProfile cannot be created");
        }

        field.setAccessible(true);
        field.set(meta, profile);

        return item;

    }
}