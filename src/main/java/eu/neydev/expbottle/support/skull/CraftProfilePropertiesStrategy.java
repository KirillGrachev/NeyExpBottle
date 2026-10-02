package eu.neydev.expbottle.support.skull;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Spigot 1.18+: {@code Bukkit.createPlayerProfile}, свойства профиля
 * дописываются в authlib-профиль напрямую.
 *
 * <p>Владелец ставится штатным методом меты до обращения к {@code createPlayerProfile},
 * поэтому даже на ядре без этого метода голова получает хотя бы ник.</p>
 */
public class CraftProfilePropertiesStrategy implements SkullApplyStrategy {

    @Override
    public @NotNull String id() {
        return "craft_profile_properties";
    }

    @Override
    public @Nullable ItemStack apply(@NotNull SkullMeta meta, @NotNull ItemStack item, @NotNull String payload,
                                     @Nullable String owner) throws Exception {

        SkullOwnerWriter.applyOwner(meta, owner);

        Method createProfile = Reflect.requireMethod(Bukkit.class, "createPlayerProfile", UUID.class, String.class);
        Object profile = createProfile.invoke(null, GameProfileFactory.stableUuid(payload), GameProfileFactory.ownerName(owner));

        Object gameProfile = Reflect.findFieldValue(profile, "GameProfile");

        if (gameProfile == null) {
            throw new IllegalStateException("GameProfile is not reachable");
        }

        GameProfileFactory.putTextureProperty(gameProfile, payload);
        return SkullOwnerWriter.setOwnerProfile(meta, profile) ? item : null;

    }
}