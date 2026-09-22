package eu.neydev.expbottle.registry;

import eu.neydev.expbottle.gui.action.ClickAction;
import eu.neydev.expbottle.gui.condition.Condition;
import eu.neydev.expbottle.gui.item.MenuItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Описание меню из файла {@code menus/<имя>.yml}.
 *
 * @param name            имя меню (имя файла без расширения)
 * @param title           заголовок окна (сырой шаблон)
 * @param size            размер, кратен девяти
 * @param permission      право для открытия (пустая строка — без проверки)
 * @param openRequirement условие открытия
 * @param denialMessage   сообщение при отказе в открытии
 * @param updateInterval  период автообновления в тиках (0 — выключено)
 * @param openActions     действия при открытии
 * @param closeActions    действия при закрытии
 * @param items           предметы в порядке объявления
 */
public record MenuDefinition(@NotNull String name, @NotNull String title, int size,
                             @NotNull String permission, @NotNull Condition openRequirement,
                             @NotNull String denialMessage, int updateInterval,
                             @NotNull List<ClickAction> openActions,
                             @NotNull List<ClickAction> closeActions,
                             @NotNull List<MenuItem> items) {
}
