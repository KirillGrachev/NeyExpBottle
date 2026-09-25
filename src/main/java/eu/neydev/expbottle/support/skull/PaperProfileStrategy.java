package eu.neydev.expbottle.support.skull;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Paper 1.18+: штатный профиль игрока с текстурами.
 *
 * <p>Самый надёжный способ на Paper: {@code Bukkit.createProfile} +
 * {@code PlayerProfileTextures.setSkin}. На Spigot и старых ядрах классов
 * Paper нет, и способ сразу помечается мёртвым.</p>
 */
public class PaperProfileStrategy implements SkullApplyStrategy {

    @Override
    public @NotNull String id() {
        return "paper_profile";
    }

    @Override
    public @Nullable ItemStack apply(@NotNull SkullMeta meta, @NotNull ItemStack item, @NotNull String payload,
                                     @Nullable String owner) throws Exception {

        Class<?> profileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
        Class<?> texturesClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfileTextures");

        SkullOwnerWriter.applyOwner(meta, owner);

        Method createProfile = Reflect.requireMethod(Bukkit.class, "createProfile", UUID.class, String.class);
        Object profile = createProfile.invoke(null, GameProfileFactory.stableUuid(payload), GameProfileFactory.ownerName(owner));

        Object textures = texturesClass.getConstructor().newInstance();
        Reflect.requireMethod(texturesClass, "setSkin", String.class).invoke(textures, SkullTextureCodec.skinUrl(payload));
        Reflect.requireMethod(profileClass, "setTextures", texturesClass).invoke(profile, textures);

        return SkullOwnerWriter.setOwnerProfile(meta, profile) ? item : null;

    }
}