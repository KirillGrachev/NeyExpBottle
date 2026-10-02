package eu.neydev.expbottle.gui.item;

import eu.neydev.expbottle.config.type.FillMode;
import eu.neydev.expbottle.gui.action.ClickAction;
import eu.neydev.expbottle.gui.condition.Condition;
import eu.neydev.expbottle.util.EnchantmentUtil;
import eu.neydev.expbottle.util.ValueResolver;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Разбор предмета меню из YAML-секции.
 *
 * <p>Модель {@link MenuItem} остаётся чистым неизменяемым объектом, а всё чтение
 * конфига — слоты с диапазонами, зачарования, флаги, условия — вынесено сюда.
 * Так предмет отвечает за данные, а парсер за их извлечение из {@code items.*}.</p>
 */
public final class MenuItemParser {

    private static final String TOKEN_ALL = "all";
    private static final String TOKEN_EMPTY = "empty";

    private MenuItemParser() {
    }

    /**
     * Читает предмет из секции конфига.
     *
     * @param id       идентификатор предмета (ключ в {@code items})
     * @param section  секция предмета
     * @param logger   логгер для предупреждений
     * @param menuSize размер меню — для проверки слотов
     * @return предмет
     */
    public static @NotNull MenuItem parse(@NotNull String id, @NotNull ConfigurationSection section,
                                         @NotNull Logger logger, int menuSize) {

        MenuItemType type = ValueResolver.enumValue(
                section.getString("type"), MenuItemType.class, MenuItemType.CUSTOM, logger);

        MenuItem.Builder builder = MenuItem.builder(id)
                .type(type)
                .material(ValueResolver.material(
                        section.getString("material"), Material.STONE, logger))
                .amount(Math.max(1, section.getInt("amount", 1)))
                .name(section.getString("name", ""))
                .lore(section.getStringList("lore"))
                .glow(section.getBoolean("glow", false))
                .customModelData(section.getInt("custom_model_data", 0))
                .unbreakable(section.getBoolean("unbreakable", false))
                .itemFlags(readItemFlags(section.getStringList("item_flags"), logger))
                .priority(section.getInt("priority", 1))
                .refresh(section.getBoolean("refresh", type.isDynamicByDefault()))
                .levels(section.getInt("levels", 0))
                .denialMessage(section.getString("denial_message", ""))
                .clickActions(ClickAction.parseList(
                        section.getStringList("click"), logger, "items." + id + ".click"))
                .skullOwner(section.getString("skull_owner"))
                .skullTexture(section.getString("skull_texture"))
                .enchantments(readEnchantments(
                        section.getStringList("enchantments"), logger, "items." + id + ".enchantments"))
                .hideEnchantments(section.getBoolean("hide_enchantments", false));

        applySlots(builder, section, logger, menuSize, id);

        builder.viewRequirement(readCondition(section.getString("view_requirement"), logger,
                "items." + id + ".view_requirement"));
        builder.clickRequirement(readCondition(section.getString("click_requirement"), logger,
                "items." + id + ".click_requirement"));

        return builder.build();

    }

    private static void applySlots(@NotNull MenuItem.Builder builder, @NotNull ConfigurationSection section,
                                   @NotNull Logger logger, int menuSize, @NotNull String id) {

        List<String> rawSlots = new ArrayList<>(section.getStringList("slots"));

        if (rawSlots.isEmpty() && section.isSet("slot")) {
            rawSlots.add(String.valueOf(section.getInt("slot")));
        }

        if (rawSlots.isEmpty()) {
            logger.warning("items." + id + ": neither slot nor slots is set - the item will not render");
            return;
        }

        FillMode fillMode = FillMode.SLOTS;

        for (String token : rawSlots) {

            String normalized = token.trim().toLowerCase(Locale.ROOT);

            if (TOKEN_ALL.equals(normalized)) {
                fillMode = FillMode.ALL;
            } else if (TOKEN_EMPTY.equals(normalized)) {
                fillMode = FillMode.EMPTY;
            }

        }

        builder.fillMode(fillMode).slots(parseSlots(rawSlots, logger, menuSize, id));

    }

    /**
     * Разбирает слоты: одиночные значения и диапазоны вида {@code 10-16}.
     * Служебные токены {@code all}/{@code empty} пропускаются.
     */
    private static @NotNull List<Integer> parseSlots(@NotNull List<String> rawSlots, @NotNull Logger logger,
                                                     int menuSize, @NotNull String id) {

        Set<Integer> slots = new LinkedHashSet<>();

        for (String token : rawSlots) {

            String normalized = token.trim();

            if (normalized.isEmpty() || isFillToken(normalized)) {
                continue;
            }

            int separator = normalized.indexOf('-');

            if (separator > 0) {
                addRange(slots, normalized.substring(0, separator), normalized.substring(separator + 1),
                        logger, menuSize, id);
                continue;
            }

            Integer slot = parseSlot(normalized, logger, menuSize, id);

            if (slot != null) {
                slots.add(slot);
            }

        }

        return new ArrayList<>(slots);

    }

    private static void addRange(@NotNull Set<Integer> slots, @NotNull String from, @NotNull String to,
                                 @NotNull Logger logger, int menuSize, @NotNull String id) {

        Integer start = parseSlot(from, logger, menuSize, id);
        Integer end = parseSlot(to, logger, menuSize, id);

        if (start == null || end == null) {
            return;
        }

        int low = Math.min(start, end);
        int high = Math.max(start, end);

        for (int slot = low; slot <= high; slot++) {
            slots.add(slot);
        }

    }

    private static @Nullable Integer parseSlot(@NotNull String raw, @NotNull Logger logger,
                                               int menuSize, @NotNull String id) {

        try {

            int slot = Integer.parseInt(raw.trim());

            if (slot < 0 || slot >= menuSize) {
                logger.warning("items." + id + ": slot " + slot + " out of range 0-" + (menuSize - 1));
                return null;
            }

            return slot;

        } catch (NumberFormatException exception) {
            logger.warning("items." + id + ": invalid slot '" + raw + "'");
            return null;
        }

    }

    /**
     * Разбирает список зачарований вида {@code NAME:уровень}.
     * Имена приводятся к текущему ядру через {@link EnchantmentUtil}.
     */
    private static @NotNull Map<Enchantment, Integer> readEnchantments(@Nullable List<String> raw,
                                                                        @NotNull Logger logger,
                                                                        @NotNull String context) {

        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();

        if (raw == null || raw.isEmpty()) {
            return enchantments;
        }

        for (String entry : raw) {

            int separator = entry.lastIndexOf(':');
            String name = separator > 0 ? entry.substring(0, separator) : entry;
            String level = separator > 0 ? entry.substring(separator + 1) : "";

            Enchantment enchantment = EnchantmentUtil.resolve(name);

            if (enchantment == null) {
                logger.warning(context + ": unknown enchantment '" + name + "'");
                continue;
            }

            int parsedLevel = 1;

            try {
                parsedLevel = Math.max(1, Integer.parseInt(level.trim()));
            } catch (NumberFormatException ignored) {
                // уровень не указан — берём первый
            }

            enchantments.put(enchantment, parsedLevel);

        }

        return enchantments;

    }

    private static boolean isFillToken(@NotNull String token) {
        String normalized = token.toLowerCase(Locale.ROOT);
        return TOKEN_ALL.equals(normalized) || TOKEN_EMPTY.equals(normalized);
    }

    private static @NotNull List<ItemFlag> readItemFlags(@Nullable List<String> raw, @NotNull Logger logger) {

        List<ItemFlag> flags = new ArrayList<>();

        if (raw == null || raw.isEmpty()) {
            return flags;
        }

        for (String value : raw) {
            flags.add(ValueResolver.enumValue(value, ItemFlag.class, ItemFlag.HIDE_ATTRIBUTES, logger));
        }

        return flags;

    }

    private static @NotNull Condition readCondition(@Nullable String raw, @NotNull Logger logger,
                                                    @NotNull String context) {
        try {
            return Condition.parse(raw);
        } catch (IllegalArgumentException exception) {
            logger.warning(context + ": " + exception.getMessage() + " - condition ignored");
            return Condition.alwaysTrue();
        }
    }
}