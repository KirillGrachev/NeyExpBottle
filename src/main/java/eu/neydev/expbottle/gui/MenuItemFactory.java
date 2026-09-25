package eu.neydev.expbottle.gui;

import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.config.type.AmountSettings;
import eu.neydev.expbottle.gui.item.MenuItem;
import eu.neydev.expbottle.gui.item.MenuItemType;
import eu.neydev.expbottle.service.AmountSelectionService;
import eu.neydev.expbottle.service.ExchangeService;
import eu.neydev.expbottle.service.PlaceholderService;
import eu.neydev.expbottle.service.SkullTextureService;
import eu.neydev.expbottle.service.PluginServices;
import eu.neydev.expbottle.util.ItemBuilder;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Собирает {@link ItemStack} из {@link MenuItem}.
 *
 * <p>Вся «кухня» отдельного предмета живёт здесь: текст и лор с плейсхолдерами,
 * блеск по доступности обмена, голова с текстурой, зачарования, флаги и
 * разворачивание токена {@code {amount_options}} в список вариантов количества.
 * {@link MenuRenderer} отвечает только за то, в какие слоты предметы положить.</p>
 */
public class MenuItemFactory {

    /** Строка лора с этим токеном разворачивается в список вариантов количества. */
    private static final String TOKEN_AMOUNT_OPTIONS = "{amount_options}";

    /** Отдельная строка с этим токеном исчезает из лора при выключенном переключателе. */
    private static final String TOKEN_AMOUNT_HINT = "{amount_hint}";
    private static final String TOKEN_LABEL = "{label}";
    private static final String TOKEN_COST = "{cost}";

    private final PluginConfig config;
    private final PlaceholderService placeholderService;
    private final ExchangeService exchangeService;
    private final AmountSelectionService amountSelectionService;
    private final SkullTextureService skullTextureService;

    public MenuItemFactory(@NotNull PluginConfig config, @NotNull PlaceholderService placeholderService,
                           @NotNull ExchangeService exchangeService, @NotNull AmountSelectionService amountSelectionService,
                           @NotNull SkullTextureService skullTextureService) {
        this.config = config;
        this.placeholderService = placeholderService;
        this.exchangeService = exchangeService;
        this.amountSelectionService = amountSelectionService;
        this.skullTextureService = skullTextureService;
    }

    /**
     * Строит предмет для зрителя с уже подготовленными плейсхолдерами.
     */
    public @NotNull ItemStack build(@NotNull Player player, @NotNull MenuItem item,
                                    @NotNull Placeholders placeholders) {

        ItemBuilder builder = new ItemBuilder(item.getMaterial(), item.getAmount());

        // Голову ставим первой: на части ядер текстура применяется только
        // пересборкой предмета, и остальная мета должна лечь уже на него
        builder.setSkull(skullTextureService, item.getSkullOwner(), item.getSkullTexture());

        builder.setName(placeholderService.format(player, item.getName(), placeholders))
                .setLore(placeholderService.formatList(player, expandAmountOptions(player, item, placeholders), placeholders))
                .setGlow(resolveGlow(player, item))
                .setUnbreakable(item.isUnbreakable())
                .setHideEnchantments(item.isHideEnchantments());

        if (item.getCustomModelData() != 0) {
            builder.setCustomModelData(item.getCustomModelData());
        }

        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            builder.addEnchantment(entry.getKey(), entry.getValue());
        }

        if (!item.getItemFlags().isEmpty()) {
            builder.addItemFlags(item.getItemFlags().toArray(new ItemFlag[0]));
        }

        return builder.build();

    }

    /**
     * У кнопок обмена блеск означает «обмен выполним прямо сейчас» — так
     * доступные варианты видны сразу. Блеск смотрит на ту же проверку, что и
     * статус в лоре: уровни и пустые пузырьки на всё выбранное количество
     * бутылок, а не на одну.
     */
    private boolean resolveGlow(@NotNull Player player, @NotNull MenuItem item) {

        if (!item.isGlow()) {
            return false;
        }

        if (item.getType() != MenuItemType.TIER) {
            return true;
        }

        return exchangeService.availability(player, item.getLevels(),
                amountSelectionService.getSelected(player)).isSuccess();

    }

    /**
     * Разворачивает строку лора {@code {amount_options}} в список вариантов
     * количества: по одной строке на вариант из {@code settings.amount.amounts},
     * выбранный подсвечивается шаблоном {@code switcher.active}.
     *
     * <p>Токен работает только на кнопках обмена: стоимость варианта считается
     * от уровней кнопки. В остальных предметах строка остаётся как есть.</p>
     */
    private @NotNull List<String> expandAmountOptions(@NotNull Player player, @NotNull MenuItem item,
                                                      @NotNull Placeholders placeholders) {

        List<String> lore = item.getLore();

        if (item.getType() != MenuItemType.TIER) {
            return lore;
        }

        // Переключатель выключен: витрина не рисует список вариантов и строку
        // про ПКМ, иначе карточка рекламирует действие, которого нет
        if ("false".equals(placeholders.get("amount_cycle"))) {
            return withoutSwitcherLines(lore);
        }

        if (!containsToken(lore)) {
            return lore;
        }

        AmountSettings settings = config.getAmount();
        int selected = amountSelectionService.getSelected(player);
        List<String> expanded = new ArrayList<>(lore.size() + settings.amounts().size());

        for (String line : lore) {

            if (!line.contains(TOKEN_AMOUNT_OPTIONS)) {

                expanded.add(line);
                continue;

            }

            for (String option : settings.amounts()) {

                int amount = AmountSelectionService.parse(option);
                String template = amount == selected
                        ? settings.switcherActive()
                        : settings.switcherInactive();

                expanded.add(template
                        .replace(TOKEN_LABEL, amountSelectionService.label(amount))
                        .replace(TOKEN_COST, costOf(item.getLevels(), amount, settings)));

            }

        }

        return expanded;

    }

    /**
     * Убирает строки переключателя: список вариантов и отдельную подсказку.
     * Строки, где подсказка идёт вместе с другим текстом, остаются: значение
     * {@code {amount_hint}} для выключенного переключателя уже пустое.
     */
    private @NotNull List<String> withoutSwitcherLines(@NotNull List<String> lore) {

        List<String> kept = new ArrayList<>(lore.size());

        for (String line : lore) {

            if (!line.contains(TOKEN_AMOUNT_OPTIONS) && !line.equals(TOKEN_AMOUNT_HINT)) {
                kept.add(line);
            }

        }

        return kept;

    }

    private boolean containsToken(@NotNull List<String> lore) {

        for (String line : lore) {

            if (line.contains(TOKEN_AMOUNT_OPTIONS)) {
                return true;
            }

        }

        return false;

    }

    /**
     * Стоимость варианта: уровни кнопки, умноженные на количество бутылок.
     * Для {@code all} точной цены нет — показываем подпись варианта.
     */
    private @NotNull String costOf(int levels, int amount, @NotNull AmountSettings settings) {

        if (amount == ExchangeService.AMOUNT_ALL) {
            return settings.allLabel();
        }

        return String.valueOf(levels * amount);

    }
}