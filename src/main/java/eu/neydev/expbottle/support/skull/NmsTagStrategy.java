package eu.neydev.expbottle.support.skull;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Последний рубеж: пишем тег {@code SkullOwner} в NMS-копию предмета.
 *
 * <p>Работает даже там, где мета не отдаёт профиль: предмет копируется в NMS,
 * в его NBT-тег напрямую пишется структура {@code SkullOwner} с текстурой,
 * затем копия зеркалится обратно в Bukkit-предмет. Именно поэтому способ
 * возвращает новый предмет, а не правит мету.</p>
 */
public class NmsTagStrategy implements SkullApplyStrategy {

    @Override
    public @NotNull String id() {
        return "nms_tag";
    }

    @Override
    public @Nullable ItemStack apply(@NotNull SkullMeta meta, @NotNull ItemStack item, @NotNull String payload,
                                     @Nullable String owner) throws Exception {

        Class<?> craftItemStack = Reflect.resolveCraftClass("CraftItemStack");
        Class<?> tagClass = Reflect.firstClass("net.minecraft.nbt.CompoundTag",
                "net.minecraft.server.NBTTagCompound");

        if (craftItemStack == null || tagClass == null) {
            return null;
        }

        Object nmsStack = craftItemStack.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);

        Object tag = Reflect.invokeAny(nmsStack, "getTag", "s", "v", "u");

        if (tag == null) {

            tag = tagClass.getConstructor().newInstance();
            Reflect.invokeSetter(nmsStack, tagClass, new String[] {"setTag", "c", "a"}, tag);

        }

        // Владелец пишется сразу в тег: обратная запись меты
        // пересобрала бы предмет и потеряла профиль
        if (!SkullOwnerWriter.writeSkullOwner(tag, tagClass, payload, GameProfileFactory.ownerName(owner))) {
            return null;
        }

        return SkullOwnerWriter.mirrorBack(craftItemStack, nmsStack);

    }
}