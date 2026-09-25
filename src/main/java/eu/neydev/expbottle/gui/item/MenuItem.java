package eu.neydev.expbottle.gui.item;

import eu.neydev.expbottle.config.type.FillMode;
import eu.neydev.expbottle.gui.action.ClickAction;
import eu.neydev.expbottle.gui.condition.Condition;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Предмет меню — единая декларативная модель для любого слота.
 *
 * <p>Декорация, информационный предмет, кнопка обмена и кнопка закрытия описываются
 * одной и той же схемой; разница только в {@link MenuItemType} и списке действий.</p>
 *
 * <p>Объект неизменяемый: собирается через {@link Builder}, а из конфига его читает
 * {@link MenuItemParser} — модель не знает про YAML.</p>
 */
public final class MenuItem {

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