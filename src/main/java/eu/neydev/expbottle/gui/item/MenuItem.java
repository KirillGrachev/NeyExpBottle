package eu.neydev.expbottle.gui.item;

import eu.neydev.expbottle.config.type.FillMode;
import eu.neydev.expbottle.gui.action.ClickAction;
import eu.neydev.expbottle.gui.condition.Condition;
import eu.neydev.expbottle.util.ValueResolver;
import org.bukkit.Material;
import eu.neydev.expbottle.util.EnchantmentUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Предмет меню — единая декларативная модель для любого слота.
 *
 * <p>Декорация, информационный предмет, кнопка обмена и кнопка закрытия описываются
 * одной и той же схемой; разница только в {@link MenuItemType} и списке действий.</p>
 *
 * <p>Объект неизменяемый: собирается через {@link Builder} или читается из конфига
 * методом {@link #from(String, ConfigurationSection, Logger, int)}.</p>
 */
public final class MenuItem {

    private static final String TOKEN_ALL = "all";
    private static final String TOKEN_EMPTY = "empty";

    private final String id;
    private final MenuItemType type;
    private final Material material;
    private final int amount;
    private final FillMode fillMode;
    private final List<Integer> slots;
    private final String name;
    private final List<String> lore;
    private final boolean glow;
    private final int customModelData;
    private final boolean unbreakable;
    private final List<ItemFlag> itemFlags;
    private final int priority;
    private final boolean refresh;
    private final int levels;
    private final Condition viewRequirement;
    private final Condition clickRequirement;
    private final String denialMessage;
    private final List<ClickAction> clickActions;
    private final String skullOwner;
    private final String skullTexture;
    private final Map<Enchantment, Integer> enchantments;
    private final boolean hideEnchantments;

    private MenuItem(@NotNull Builder builder) {

        this.id = builder.id;
        this.type = builder.type;
        this.material = builder.material;
        this.amount = builder.amount;
        this.fillMode = builder.fillMode;
        this.slots = Collections.unmodifiableList(new ArrayList<>(builder.slots));
        this.name = builder.name;
        this.lore = Collections.unmodifiableList(new ArrayList<>(builder.lore));
        this.glow = builder.glow;
        this.customModelData = builder.customModelData;
        this.unbreakable = builder.unbreakable;
        this.itemFlags = Collections.unmodifiableList(new ArrayList<>(builder.itemFlags));
        this.priority = builder.priority;
        this.refresh = builder.refresh;
        this.levels = builder.levels;
        this.viewRequirement = builder.viewRequirement;
        this.clickRequirement = builder.clickRequirement;
        this.denialMessage = builder.denialMessage;
        this.clickActions = Collections.unmodifiableList(new ArrayList<>(builder.clickActions));
        this.skullOwner = builder.skullOwner;
        this.skullTexture = builder.skullTexture;
        this.enchantments = Collections.unmodifiableMap(new LinkedHashMap<>(builder.enchantments));
        this.hideEnchantments = builder.hideEnchantments;

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
    public static @NotNull MenuItem from(@NotNull String id, @NotNull ConfigurationSection section,
                                         @NotNull Logger logger, int menuSize) {

        MenuItemType type = ValueResolver.enumValue(
                section.getString("type"), MenuItemType.class, MenuItemType.CUSTOM, logger);

        Builder builder = builder(id)
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

    private static void applySlots(@NotNull Builder builder, @NotNull ConfigurationSection section,
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

    public static @NotNull Builder builder(@NotNull String id) {
        return new Builder(id);
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull MenuItemType getType() {
        return type;
    }

    public @NotNull Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public @NotNull FillMode getFillMode() {
        return fillMode;
    }

    public @NotNull List<Integer> getSlots() {
        return slots;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull List<String> getLore() {
        return lore;
    }

    public boolean isGlow() {
        return glow;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public @NotNull List<ItemFlag> getItemFlags() {
        return itemFlags;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isRefresh() {
        return refresh;
    }

    public int getLevels() {
        return levels;
    }

    public @NotNull Condition getViewRequirement() {
        return viewRequirement;
    }

    public @NotNull Condition getClickRequirement() {
        return clickRequirement;
    }

    public @NotNull String getDenialMessage() {
        return denialMessage;
    }

    public @NotNull List<ClickAction> getClickActions() {
        return clickActions;
    }

    /**
     * Предмет заполняет слоты целиком или по остаточному принципу.
     *
     * @return true для режимов ALL и EMPTY
     */
    public @Nullable String getSkullOwner() {
        return skullOwner;
    }

    public @Nullable String getSkullTexture() {
        return skullTexture;
    }

    public @NotNull Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public boolean isHideEnchantments() {
        return hideEnchantments;
    }

    public boolean isFill() {
        return fillMode != FillMode.SLOTS;
    }

    @Override
    public @NotNull String toString() {
        return "MenuItem[id=" + id + ", type=" + type + ", slots=" + slots + ", levels=" + levels + "]";
    }

    /**
     * Сборщик предмета меню.
     */
    public static final class Builder {

        private final String id;

        private MenuItemType type = MenuItemType.CUSTOM;
        private Material material = Material.STONE;
        private int amount = 1;
        private FillMode fillMode = FillMode.SLOTS;
        private List<Integer> slots = new ArrayList<>();
        private String name = "";
        private List<String> lore = new ArrayList<>();
        private boolean glow = false;
        private int customModelData = 0;
        private boolean unbreakable = false;
        private List<ItemFlag> itemFlags = new ArrayList<>();
        private int priority = 1;
        private boolean refresh = false;
        private int levels = 0;
        private Condition viewRequirement = Condition.alwaysTrue();
        private Condition clickRequirement = Condition.alwaysTrue();
        private String denialMessage = "";
        private List<ClickAction> clickActions = new ArrayList<>();
        private String skullOwner;
        private String skullTexture;
        private Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        private boolean hideEnchantments = false;

        private Builder(@NotNull String id) {
            this.id = id;
        }

        public @NotNull Builder type(@NotNull MenuItemType type) {
            this.type = type;
            return this;
        }

        public @NotNull Builder material(@NotNull Material material) {
            this.material = material;
            return this;
        }

        public @NotNull Builder amount(int amount) {
            this.amount = Math.max(1, amount);
            return this;
        }

        public @NotNull Builder fillMode(@NotNull FillMode fillMode) {
            this.fillMode = fillMode;
            return this;
        }

        public @NotNull Builder slots(@NotNull List<Integer> slots) {
            this.slots = new ArrayList<>(slots);
            return this;
        }

        public @NotNull Builder name(@Nullable String name) {
            this.name = name == null ? "" : name;
            return this;
        }

        public @NotNull Builder lore(@Nullable List<String> lore) {
            this.lore = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
            return this;
        }

        public @NotNull Builder glow(boolean glow) {
            this.glow = glow;
            return this;
        }

        public @NotNull Builder customModelData(int customModelData) {
            this.customModelData = customModelData;
            return this;
        }

        public @NotNull Builder unbreakable(boolean unbreakable) {
            this.unbreakable = unbreakable;
            return this;
        }

        public @NotNull Builder itemFlags(@Nullable List<ItemFlag> itemFlags) {
            this.itemFlags = itemFlags == null ? new ArrayList<>() : new ArrayList<>(itemFlags);
            return this;
        }

        public @NotNull Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public @NotNull Builder refresh(boolean refresh) {
            this.refresh = refresh;
            return this;
        }

        public @NotNull Builder levels(int levels) {
            this.levels = levels;
            return this;
        }

        public @NotNull Builder viewRequirement(@NotNull Condition viewRequirement) {
            this.viewRequirement = viewRequirement;
            return this;
        }

        public @NotNull Builder clickRequirement(@NotNull Condition clickRequirement) {
            this.clickRequirement = clickRequirement;
            return this;
        }

        public @NotNull Builder denialMessage(@Nullable String denialMessage) {
            this.denialMessage = denialMessage == null ? "" : denialMessage;
            return this;
        }

        public @NotNull Builder clickActions(@Nullable List<ClickAction> clickActions) {
            this.clickActions = clickActions == null ? new ArrayList<>() : new ArrayList<>(clickActions);
            return this;
        }

        public @NotNull Builder skullOwner(@Nullable String skullOwner) {
            this.skullOwner = skullOwner;
            return this;
        }

        public @NotNull Builder skullTexture(@Nullable String skullTexture) {
            this.skullTexture = skullTexture;
            return this;
        }

        public @NotNull Builder enchantments(@Nullable Map<Enchantment, Integer> enchantments) {
            this.enchantments = enchantments == null ? new LinkedHashMap<>() : new LinkedHashMap<>(enchantments);
            return this;
        }

        public @NotNull Builder hideEnchantments(boolean hideEnchantments) {
            this.hideEnchantments = hideEnchantments;
            return this;
        }

        public @NotNull MenuItem build() {
            return new MenuItem(this);
        }
    }
}
