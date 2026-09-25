package be.seeseemelk.mockbukkit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * Заглушка craft-класса предмета: {@code Reflect.resolveCraftClass} находит её
 * в пакете сервера MockBukkit, как на настоящем ядре нашла бы
 * {@code CraftItemStack}.
 *
 * <p>Копия NMS хранится по материалу: первый запрос предмета создаёт тег
 * пустым (ветка {@code tag == null}), повторный запрос того же материала
 * возвращает копию с уже записанным тегом (ветка {@code tag != null}).
 * Камень копироваться отказывается — так огневых тестов проверяет гибель
 * ветки {@code nms_tag} и путь «все способы мертвы».</p>
 */
public final class CraftItemStack {

    private static final Map<Material, NmsStackStub> COPIES = new EnumMap<>(Material.class);

    private CraftItemStack() {
    }

    public static NmsStackStub asNMSCopy(ItemStack item) {

        if (item.getType() == Material.STONE) {
            throw new UnsupportedOperationException("stub: the core refuses to copy this item");
        }

        return COPIES.computeIfAbsent(item.getType(), type -> new NmsStackStub());

    }

    public static ItemStack asBukkitCopy(NmsStackStub stack) {
        return new ItemStack(Material.PLAYER_HEAD);
    }

    /**
     * НМС-копия предмета с тегом: {@code getTag}/{@code setTag} — сигнатуры,
     * которые перебирают {@code NmsTagStrategy} и {@code Reflect.invokeSetter}.
     */
    public static final class NmsStackStub {

        private net.minecraft.nbt.CompoundTag tag;

        public net.minecraft.nbt.CompoundTag getTag() {
            return tag;
        }

        public void setTag(net.minecraft.nbt.CompoundTag tag) {
            this.tag = tag;
        }
    }
}
