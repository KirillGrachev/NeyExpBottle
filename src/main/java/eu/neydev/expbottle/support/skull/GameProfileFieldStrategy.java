package eu.neydev.expbottle.support.skull;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Классика 1.16—1.20.4: поле профиля лежит прямо в скалл-мете.
 *
 * <p>Ищем в мете поле типа {@code GameProfile} и пишем в него собранный
 * authlib-профиль с текстурой. Если поля нет (новое ядро или мок) — способ
 * не подходит.</p>
 */
public class GameProfileFieldStrategy implements SkullApplyStrategy {

    @Override
    public @NotNull String id() {
        return "game_profile_field";
    }

    @Override
    public @Nullable ItemStack apply(@NotNull SkullMeta meta, @NotNull ItemStack item, @NotNull String payload,
                                     @Nullable String owner) throws Exception {

        Field field = Reflect.findFieldOfType(meta.getClass(), "GameProfile");

        if (field == null) {
            return null;
        }

        SkullOwnerWriter.applyOwner(meta, owner);

        field.setAccessible(true);
        field.set(meta, GameProfileFactory.newGameProfile(payload, owner));

        return item;

    }
}