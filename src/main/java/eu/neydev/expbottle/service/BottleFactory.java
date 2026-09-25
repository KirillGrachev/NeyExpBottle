package eu.neydev.expbottle.service;

import eu.neydev.expbottle.config.PluginConfig;
import eu.neydev.expbottle.registry.BottleTier;
import eu.neydev.expbottle.util.HexColorUtil;
import eu.neydev.expbottle.util.ItemBuilder;
import eu.neydev.expbottle.util.Placeholders;
import eu.neydev.expbottle.util.ValueResolver;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Создание предмета-бутылки.
 *
 * <p>Отвечает только за внешний вид и метку: опыт игрока и проверки
 * находятся в {@link ExchangeService}.</p>
 */
public class BottleFactory {

    private final PluginConfig config;
    private final BottleTagService tagService;
    private final PlaceholderService placeholderService;

    public BottleFactory(@NotNull PluginConfig config, @NotNull BottleTagService tagService,
                         @NotNull PlaceholderService placeholderService) {
        this.config = config;
        this.tagService = tagService;
        this.placeholderService = placeholderService;
    }

    /**
     * Собирает бутылку на {@code levels} уровней.
     *
     * @param levels количество уровней
     * @return готовый предмет с меткой
     */
    public @NotNull ItemStack create(int levels) {

        int safeLevels = clampLevels(levels);
        Placeholders placeholders = placeholderService.forLevels(safeLevels);

        String name = HexColorUtil.color(placeholders.apply(config.getBottleName()));
        List<String> lore = HexColorUtil.color(placeholders.apply(config.getBottleLore()));

        if (config.isInstructionEnabled()) {
            lore.add("");
            lore.add(HexColorUtil.color(placeholders.apply(config.getInstructionText())));
        }

        ItemStack item = new ItemBuilder(config.getBottleMaterial())
                .setName(name)
                .setLore(lore)
                .setGlow(config.isBottleGlowEnabled())
                .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                .build();

        return tagService.tag(item, safeLevels);

    }

    /**
     * Собирает бутылку по уровню обмена из меню.
     *
     * @param tier уровень обмена
     * @return готовый предмет
     */
    public @NotNull ItemStack create(@NotNull BottleTier tier) {
        return create(tier.levels());
    }

    /**
     * Ограничивает количество уровней значением из конфига.
     *
     * @param levels запрошенные уровни
     * @return допустимое количество
     */
    public int clampLevels(int levels) {
        return ValueResolver.clamp(levels, 1, config.getMaxBottleLevels());
    }
}