package eu.neydev.expbottle.gui;

import eu.neydev.expbottle.gui.item.MenuItem;
import eu.neydev.expbottle.gui.item.MenuItemType;
import eu.neydev.expbottle.model.ExchangeOutcome;
import eu.neydev.expbottle.service.AmountSelectionService;
import eu.neydev.expbottle.service.ExchangeService;
import eu.neydev.expbottle.service.PlaceholderService;
import eu.neydev.expbottle.service.PluginServices;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Отрисовка меню по его раскладке.
 *
 * <p>Порядок проходов важен: сначала фон на все слоты, затем обычные предметы
 * по приоритету, затем фон «по свободным слотам». Так декорация не перекрывает
 * кнопки, а заполнение пустых слотов не затирает их.</p>
 *
 * <p>Сам предмет собирает {@link MenuItemFactory}; рендерер решает лишь,
 * в какой слот его положить и виден ли он зрителю.</p>
 */
public class MenuRenderer {

    private final PlaceholderService placeholderService;
    private final ExchangeService exchangeService;
    private final AmountSelectionService amountSelectionService;
    private final MenuItemFactory itemFactory;

    public MenuRenderer(@NotNull PlaceholderService placeholderService, @NotNull ExchangeService exchangeService,
                        @NotNull AmountSelectionService amountSelectionService, @NotNull MenuItemFactory itemFactory) {
        this.placeholderService = placeholderService;
        this.exchangeService = exchangeService;
        this.amountSelectionService = amountSelectionService;
        this.itemFactory = itemFactory;
    }

    /**
     * Полная отрисовка меню.
     *
     * @param inventory инвентарь
     * @param player    зритель
     * @param menuName  имя меню (для плейсхолдера {@code {menu}})
     * @param layout    раскладка предметов
     * @param context   плейсхолдеры родительского меню
     */
    public void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull String menuName,
                       @NotNull MenuLayout layout, @NotNull Placeholders context) {

        renderFill(inventory, player, menuName, layout.fullFill(), context);
        renderSlots(inventory, player, menuName, layout.itemsBySlot(), false, context);
        renderEmptyFill(inventory, player, menuName, layout.emptyFill(), context);

    }

    /**
     * Обновление только динамических предметов ({@code refresh: true}).
     *
     * @param inventory инвентарь
     * @param player    зритель
     * @param menuName  имя меню (для плейсхолдера {@code {menu}})
     * @param layout    раскладка предметов
     * @param context   плейсхолдеры родительского меню
     */
    public void renderDynamic(@NotNull Inventory inventory, @NotNull Player player, @NotNull String menuName,
                              @NotNull MenuLayout layout, @NotNull Placeholders context) {
        renderSlots(inventory, player, menuName, layout.itemsBySlot(), true, context);
    }

    private void renderSlots(@NotNull Inventory inventory, @NotNull Player player, @NotNull String menuName,
                             @NotNull Map<Integer, MenuItem> items, boolean onlyRefreshable,
                             @NotNull Placeholders context) {

        for (Map.Entry<Integer, MenuItem> entry : items.entrySet()) {

            MenuItem item = entry.getValue();

            if (onlyRefreshable && !item.isRefresh()) {
                continue;
            }

            renderItem(inventory, player, menuName, item, entry.getKey(), context);

        }

    }

    private void renderFill(@NotNull Inventory inventory, @NotNull Player player, @NotNull String menuName,
                            @NotNull Iterable<MenuItem> items, @NotNull Placeholders context) {

        for (MenuItem item : items) {

            ItemStack stack = itemFactory.build(player, item, placeholdersFor(player, menuName, item, context));

            for (int slot = 0; slot < inventory.getSize(); slot++) {
                inventory.setItem(slot, stack);
            }

        }

    }

    private void renderEmptyFill(@NotNull Inventory inventory, @NotNull Player player, @NotNull String menuName,
                                 @NotNull Iterable<MenuItem> items, @NotNull Placeholders context) {

        for (MenuItem item : items) {

            if (!item.getViewRequirement().evaluate(placeholdersFor(player, menuName, item, context))) {
                continue;
            }

            ItemStack stack = itemFactory.build(player, item, placeholdersFor(player, menuName, item, context));

            for (int slot = 0; slot < inventory.getSize(); slot++) {

                if (inventory.getItem(slot) == null) {
                    inventory.setItem(slot, stack);
                }

            }

        }

    }

    /**
     * Рисует предмет в слоте. Если условие видимости не выполнено — слот очищается,
     * иначе предмет «залип» бы после обновления.
     */
    private void renderItem(@NotNull Inventory inventory, @NotNull Player player, @NotNull String menuName,
                            @NotNull MenuItem item, int slot, @NotNull Placeholders context) {

        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        Placeholders placeholders = placeholdersFor(player, menuName, item, context);

        if (!item.getViewRequirement().evaluate(placeholders)) {
            inventory.setItem(slot, null);
            return;
        }

        inventory.setItem(slot, itemFactory.build(player, item, placeholders));

    }

    private @NotNull Placeholders placeholdersFor(@NotNull Player player, @NotNull String menuName,
                                                  @NotNull MenuItem item, @NotNull Placeholders context) {

        Placeholders placeholders = placeholderService.forMenu(player, menuName);
        placeholders.merge(context);

        if (item.getType() == MenuItemType.TIER) {

            // Доступность считаем на всё выбранное количество бутылок, а не на
            // одну: статус в лоре должен совпадать с исходом нажатия и называть
            // настоящую причину отказа (уровни или пустые пузырьки)
            int selected = amountSelectionService.getSelected(player);
            ExchangeOutcome availability = exchangeService.availability(player, item.getLevels(), selected);
            placeholders.merge(placeholderService.forTier(item.getLevels(), item.getId(), availability));

        }

        return placeholders;

    }
}