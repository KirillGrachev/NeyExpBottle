package eu.neydev.expbottle.gui;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Узкий контракт навигации по меню для исполнителя действий.
 *
 * <p>Действия {@code [close]}, {@code [refresh]}, {@code [open]} и {@code [back]}
 * управляют окнами, а исполняет их {@code ActionExecutor}: без этого интерфейса
 * ему пришлось бы держать ссылку на весь контейнер сервисов или породить
 * цикл сборки с {@code MenuService}. Реализацию подшивает композиционный
 * корень после постройки обоих сервисов.</p>
 */
public interface MenuNavigation {

    void close(@NotNull Player player);

    void refresh(@NotNull Player player);

    boolean open(@NotNull Player player, @NotNull String menuName);

    void openParent(@NotNull Player player);
}
