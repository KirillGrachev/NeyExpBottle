package eu.neydev.expbottle.support.skull;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Один способ нанести кастомную текстуру на голову.
 *
 * <p>Реализации рассчитаны на разные группы ядер (Paper, Spigot 1.18+,
 * классика 1.16—1.20.4, ядра 1.20.5+, прямой NBT).
 * {@link eu.neydev.expbottle.support.SkullTextureSupport} перебирает их по
 * очереди и запоминает сработавшую, поэтому способ не должен держать
 * состояние — только применить текстуру или сообщить, что не вышло.</p>
 */
public interface SkullApplyStrategy {

    /**
     * Короткое имя способа для лога и статистики (в нижнем регистре).
     */
    @NotNull String id();

    /**
     * Применяет текстуру к голове.
     *
     * @param meta    мета головы
     * @param item    предмет, которому она принадлежит
     * @param payload base64-блок текстуры
     * @param owner   ник владельца головы или {@code null}
     * @return предмет с текстурой или {@code null}, если способ не подошёл ядру
     * @throws Exception если отражение не сработало — способ помечается мёртвым
     */
    @Nullable ItemStack apply(@NotNull SkullMeta meta, @NotNull ItemStack item, @NotNull String payload,
                              @Nullable String owner) throws Exception;
}