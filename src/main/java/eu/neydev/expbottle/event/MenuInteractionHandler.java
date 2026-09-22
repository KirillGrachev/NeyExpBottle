package eu.neydev.expbottle.event;

import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.gui.Menu;
import eu.neydev.expbottle.gui.MenuHolder;
import eu.neydev.expbottle.gui.action.ActionType;
import eu.neydev.expbottle.gui.action.ClickAction;
import eu.neydev.expbottle.gui.item.MenuItem;
import eu.neydev.expbottle.gui.item.MenuItemType;
import eu.neydev.expbottle.service.ActionExecutor;
import eu.neydev.expbottle.service.DiagnosticsService;
import eu.neydev.expbottle.service.ExperienceService;
import eu.neydev.expbottle.service.MessageService;
import eu.neydev.expbottle.service.PlaceholderService;
import eu.neydev.expbottle.service.PluginServices;
import eu.neydev.expbottle.service.SoundService;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Логика взаимодействия с меню: клик, перетаскивание, закрытие.
 *
 * <p>Сам обработчик тонкий: он находит предмет в слоте, проверяет условие клика
 * и передаёт действия в {@link ActionExecutor}. Поведение кнопки целиком
 * описывается в конфиге.</p>
 */
public class MenuInteractionHandler {

    private final PluginConfig config;
    private final ActionExecutor actionExecutor;
    private final MessageService messageService;
    private final SoundService soundService;
    private final PlaceholderService placeholderService;
    private final ExperienceService experienceService;
    private final DiagnosticsService diagnosticsService;

    public MenuInteractionHandler(@NotNull PluginServices services) {

        this.config = services.getConfigManager();
        this.actionExecutor = services.getActionExecutor();
        this.messageService = services.getMessageService();
        this.soundService = services.getSoundService();
        this.placeholderService = services.getPlaceholderService();
        this.experienceService = services.getExperienceService();
        this.diagnosticsService = services.getDiagnosticsService();

    }

    /**
     * Обрабатывает клик в меню.
     *
     * @param event событие клика
     */
    public void onMenuClick(@NotNull InventoryClickEvent event) {

        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }

        // Меню ничего не отдаёт наружу. Отменяем и shift-клики из собственного
        // инвентаря: иначе предметы меню можно было бы перетащить к себе.
        // Не настраивается: это базовая безопасность плагина.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clicked = event.getClickedInventory();

        // Сравниваем по holder'у, а не по ссылке: так проверка не зависит от того,
        // как конкретное ядро заворачивает InventoryView.
        if (clicked == null || !(clicked.getHolder() instanceof MenuHolder)) {
            return;
        }

        Menu menu = holder.getMenu();
        MenuItem item = menu.getItem(event.getRawSlot());

        if (item == null) {
            return;
        }

        if (!isInteractive(item, event.getClick())) {

            diagnosticsService.debug("Click " + event.getClick() + " on '" + item.getId() + "' ignored");
            return;

        }

        Placeholders placeholders = placeholdersFor(player, item);

        if (!item.getClickRequirement().evaluate(placeholders)) {

            handleDenial(player, item, placeholders);
            return;

        }

        actionExecutor.execute(player, resolveActions(item), placeholders);

    }

    /**
     * Запрещает перетаскивание предметов в меню.
     *
     * @param event событие перетаскивания
     */
    public void onMenuDrag(@NotNull InventoryDragEvent event) {

        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder)) {
            return;
        }

        // Перетаскивание в меню отменяется всегда: это базовая безопасность
        event.setCancelled(true);

    }

    /**
     * Закрывает сессию меню и выполняет действия закрытия.
     *
     * @param event событие закрытия
     */
    public void onMenuClose(@NotNull InventoryCloseEvent event) {

        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }

        Menu menu = holder.getMenu();
        menu.onClose();

        if (menu.getPlayer().getUniqueId().equals(event.getPlayer().getUniqueId())
                && !menu.getDefinition().closeActions().isEmpty()) {

            actionExecutor.execute(menu.getPlayer(), menu.getDefinition().closeActions(),
                    placeholderService.forPlayer(menu.getPlayer()));

        }

        diagnosticsService.debug("Menu '" + menu.getName() + "' closed: " + event.getPlayer().getName());

    }

    /**
     * Действия по умолчанию для типов, которым они положены.
     * Явный список {@code click} в конфиге всегда имеет приоритет.
     */
    private @NotNull List<ClickAction> resolveActions(@NotNull MenuItem item) {

        if (!item.getClickActions().isEmpty()) {
            return item.getClickActions();
        }

        return switch (item.getType()) {

            case TIER -> List.of(
                    new ClickAction(ActionType.EXCHANGE, String.valueOf(item.getLevels())),
                    new ClickAction(ActionType.REFRESH, "")
            );

            case CLOSE -> List.of(new ClickAction(ActionType.CLOSE, ""));

            case DECORATION, INFO, CUSTOM -> List.of();

        };

    }

    /**
     * Плейсхолдеры для предмета: общие плюс специфичные для кнопки обмена.
     */
    private @NotNull Placeholders placeholdersFor(@NotNull Player player, @NotNull MenuItem item) {

        Placeholders placeholders = placeholderService.forPlayer(player);

        if (item.getType() == MenuItemType.TIER) {

            boolean available = experienceService.hasLevels(player, item.getLevels());
            placeholders.merge(placeholderService.forTier(item.getLevels(), item.getId(), available));

        }

        return placeholders;

    }

    private void handleDenial(@NotNull Player player, @NotNull MenuItem item,
                              @NotNull Placeholders placeholders) {

        if (!item.getDenialMessage().isEmpty()) {
            messageService.sendText(player, placeholderService.format(player, item.getDenialMessage(), placeholders));
        } else {
            messageService.send(player, MessageKey.NO_PERMISSION, placeholders);
        }

        soundService.play(player, config.getFailSound());
        diagnosticsService.debug("Click requirement not met for '" + item.getId() + "': " + player.getName());

    }

    /**
     * Декорация и информация не реагируют на клики; обмен — только на явный клик
     * без shift, чтобы случайное перемещение предмета не тратило опыт.
     */
    private boolean isInteractive(@NotNull MenuItem item, @NotNull ClickType click) {

        if (item.getType() == MenuItemType.DECORATION || item.getType() == MenuItemType.INFO) {
            return false;
        }

        return (click.isLeftClick() || click.isRightClick()) && !click.isShiftClick();

    }
}
