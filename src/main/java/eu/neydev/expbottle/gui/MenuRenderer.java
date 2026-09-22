package eu.neydev.expbottle.gui;

import eu.neydev.expbottle.config.PluginConfig;
import org.bukkit.enchantments.Enchantment;
import eu.neydev.expbottle.gui.item.MenuItem;
import eu.neydev.expbottle.gui.item.MenuItemType;
import eu.neydev.expbottle.service.ExperienceService;
import eu.neydev.expbottle.service.PlaceholderService;
import eu.neydev.expbottle.service.PluginServices;
import eu.neydev.expbottle.util.ItemBuilder;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Отрисовка меню по его раскладке.
 *
 * <p>Порядок проходов важен: сначала фон на все слоты, затем обычные предметы
 * по приоритету, затем фон «по свободным слотам». Так декорация не перекрывает
 * кнопки, а заполнение пустых слотов не затирает их.</p>
 */
public class MenuRenderer {

    private final PluginConfig config;
    private final PlaceholderService placeholderService;
    private final ExperienceService experienceService;
    private final java.util.logging.Logger logger;

    public MenuRenderer(@NotNull PluginServices services) {
        this.config = services.getConfigManager();
        this.placeholderService = services.getPlaceholderService();
        this.experienceService = services.getExperienceService();
        this.logger = services.getPlugin().getLogger();
    }

    /**
     * Полная отрисовка меню.
     *
     * @param inventory инвентарь
     * @param player    зритель
     * @param layout    раскладка предметов
     */
    public void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull MenuLayout layout) {

        renderFill(inventory, player, layout.fullFill());
        renderSlots(inventory, player, layout.itemsBySlot(), false);
        renderEmptyFill(inventory, player, layout.emptyFill());

    }

    /**
     * Обновление только динамических предметов ({@code refresh: true}).
     *
     * @param inventory инвентарь
     * @param player    зритель
     * @param layout    раскладка предметов
     */
    public void renderDynamic(@NotNull Inventory inventory, @NotNull Player player,
                              @NotNull MenuLayout layout) {
        renderSlots(inventory, player, layout.itemsBySlot(), true);
    }

    private void renderSlots(@NotNull Inventory inventory, @NotNull Player player,
                             @NotNull Map<Integer, MenuItem> items, boolean onlyRefreshable) {

        for (Map.Entry<Integer, MenuItem> entry : items.entrySet()) {

            MenuItem item = entry.getValue();

            if (onlyRefreshable && !item.isRefresh()) {
                continue;
            }

            renderItem(inventory, player, item, entry.getKey());

        }

    }

    private void renderFill(@NotNull Inventory inventory, @NotNull Player player,
                            @NotNull Iterable<MenuItem> items) {

        for (MenuItem item : items) {

            ItemStack stack = buildItem(player, item, placeholdersFor(player, item));

            for (int slot = 0; slot < inventory.getSize(); slot++) {
                inventory.setItem(slot, stack);
            }

        }

    }

    private void renderEmptyFill(@NotNull Inventory inventory, @NotNull Player player,
                                 @NotNull Iterable<MenuItem> items) {

        for (MenuItem item : items) {

            if (!item.getViewRequirement().evaluate(placeholdersFor(player, item))) {
                continue;
            }

            ItemStack stack = buildItem(player, item, placeholdersFor(player, item));

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
    private void renderItem(@NotNull Inventory inventory, @NotNull Player player,
                            @NotNull MenuItem item, int slot) {

        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        Placeholders placeholders = placeholdersFor(player, item);

        if (!item.getViewRequirement().evaluate(placeholders)) {
            inventory.setItem(slot, null);
            return;
        }

        inventory.setItem(slot, buildItem(player, item, placeholders));

    }

    private @NotNull ItemStack buildItem(@NotNull Player player, @NotNull MenuItem item,
                                         @NotNull Placeholders placeholders) {

        ItemBuilder builder = new ItemBuilder(item.getMaterial(), item.getAmount())
                .setName(placeholderService.format(player, item.getName(), placeholders))
                .setLore(placeholderService.formatList(player, item.getLore(), placeholders))
                .setGlow(resolveGlow(player, item))
                .setUnbreakable(item.isUnbreakable());

        if (item.getCustomModelData() != 0) {
            builder.setCustomModelData(item.getCustomModelData());
        }

        builder.setSkull(item.getSkullOwner(), item.getSkullTexture(), logger);
        builder.setHideEnchantments(item.isHideEnchantments());

        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            builder.addEnchantment(entry.getKey(), entry.getValue());
        }

        if (!item.getItemFlags().isEmpty()) {
            builder.addItemFlags(item.getItemFlags().toArray(new ItemFlag[0]));
        }

        return builder.build();

    }

    /**
     * У кнопок обмена блеск означает «хватает опыта» — так доступные варианты видны сразу.
     */
    private boolean resolveGlow(@NotNull Player player, @NotNull MenuItem item) {

        if (!item.isGlow()) {
            return false;
        }

        if (item.getType() != MenuItemType.TIER) {
            return true;
        }

        return experienceService.hasLevels(player, item.getLevels());

    }

    private @NotNull Placeholders placeholdersFor(@NotNull Player player, @NotNull MenuItem item) {

        Placeholders placeholders = placeholderService.forPlayer(player);

        if (item.getType() == MenuItemType.TIER) {

            boolean available = experienceService.hasLevels(player, item.getLevels());
            placeholders.merge(placeholderService.forTier(item.getLevels(), item.getId(), available));

        }

        return placeholders;

    }
}
