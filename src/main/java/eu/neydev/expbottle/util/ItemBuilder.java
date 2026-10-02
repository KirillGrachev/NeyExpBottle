package eu.neydev.expbottle.util;

import eu.neydev.expbottle.service.SkullTextureService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Сборщик {@link ItemStack} с защитой от {@code null}-меты.
 *
 * <p>Отличия от старой реализации:</p>
 * <ul>
 *     <li>мета может отсутствовать (AIR, некоторые ядра) — методы просто ничего не делают;</li>
 *     <li>блеск («glow») включается нативным {@code setEnchantmentGlintOverride} на 1.20.5+,
 *         а на старых ядрах — фейковым зачарованием;</li>
 *     <li>никаких прямых обращений к {@code Enchantment.DURABILITY}: поле удалено в новых ядрах
 *         и роняет {@code NoSuchFieldError}.</li>
 * </ul>
 */
public class ItemBuilder {

    private static final String GLINT_OVERRIDE_METHOD = "setEnchantmentGlintOverride";
    private static final String[] GLOW_ENCHANTMENTS = {"UNBREAKING", "DURABILITY"};

    private ItemStack item;
    private @Nullable ItemMeta meta;

    public ItemBuilder(@NotNull Material material) {
        this(material, 1);
    }

    public ItemBuilder(@NotNull Material material, int amount) {
        this.item = new ItemStack(material, Math.max(1, amount));
        this.meta = item.getItemMeta();
    }

    public @NotNull ItemBuilder setName(@Nullable String name) {

        if (meta == null || name == null) {
            return this;
        }

        meta.setDisplayName(name);
        return this;

    }

    public @NotNull ItemBuilder setLore(@Nullable List<String> lore) {

        // Недостижимо в тестах: MockBukkit даёт мету каждому материалу,
        // ветка — страховка от ядер, отдающих null-мету
        if (meta == null) {
            return this;
        }

        // null трактуем как «лора нет»: некоторые меты не принимают null

        meta.setLore(lore == null ? List.of() : new ArrayList<>(lore));
        return this;

    }

    public @NotNull ItemBuilder addLore(@Nullable String line) {

        if (meta == null || line == null) {
            return this;
        }

        List<String> lore = meta.getLore();

        if (lore == null) {
            lore = new ArrayList<>();
        }

        lore.add(line);
        meta.setLore(lore);

        return this;

    }

    public @NotNull ItemBuilder addItemFlags(@NotNull ItemFlag @NotNull ... flags) {

        if (meta == null || flags.length == 0) {
            return this;
        }

        meta.addItemFlags(flags);
        return this;

    }

    public @NotNull ItemBuilder setAmount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    /**
     * Владелец или текстура головы. Работает только для предметов-голов.
     *
     * <p>На части ядер текстура ставится только через пересборку предмета,
     * поэтому сборщик подменяет и предмет, и его мету на возвращённые.</p>
     *
     * @param skulls сервис текстур голов (состояние подбора способа живёт в нём)
     * @param owner  ник владельца или {@code null}
     * @param texture base64-текстура, ссылка, хеш или {@code null}
     * @return этот же объект
     */
    public @NotNull ItemBuilder setSkull(@NotNull SkullTextureService skulls, @Nullable String owner,
                                         @Nullable String texture) {

        if (!(meta instanceof SkullMeta skullMeta)) {
            return this;
        }

        ItemStack applied = skulls.apply(item, skullMeta, owner, texture);

        if (applied != item) {

            item = applied;
            meta = item.getItemMeta();

        }

        return this;

    }

    /**
     * Возвращает предмет, который держит сборщик: на части ядер голову
     * удаётся собрать только заменой предмета.
     */
    public @NotNull ItemStack getItem() {
        return item;
    }

    /**
     * Зачарование предмета. Уровень и само зачарование зависят от ядра —
     * имя разрешает {@link EnchantmentUtil}.
     *
     * @param enchantment зачарование
     * @param level       уровень
     * @return этот же объект
     */
    public @NotNull ItemBuilder addEnchantment(@NotNull Enchantment enchantment, int level) {

        if (meta == null) {
            return this;
        }

        meta.addEnchant(enchantment, Math.max(1, level), true);
        return this;

    }

    /**
     * Скрывает информацию о чарах: блеск остаётся, списка зачарований нет.
     *
     * @param hide скрывать ли
     * @return этот же объект
     */
    public @NotNull ItemBuilder setHideEnchantments(boolean hide) {

        if (meta == null || !hide) {
            return this;
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;

    }

    public @NotNull ItemBuilder setUnbreakable(boolean unbreakable) {

        if (meta == null) {
            return this;
        }

        meta.setUnbreakable(unbreakable);
        return this;

    }

    public @NotNull ItemBuilder setCustomModelData(int data) {

        if (meta == null) {
            return this;
        }

        meta.setCustomModelData(data);
        return this;

    }

    /**
     * Включает блеск зачарования без видимого зачарования.
     *
     * @param glow нужен ли блеск
     * @return этот же объект
     */
    public @NotNull ItemBuilder setGlow(boolean glow) {

        if (meta == null || !glow) {
            return this;
        }

        if (applyGlintOverride()) {
            return this;
        }

        applyFakeEnchantment();
        return this;

    }

    public @NotNull ItemStack build() {

        if (meta != null) {
            item.setItemMeta(meta);
        }

        return item;

    }

    /**
     * Нативный блеск появился в 1.20.5.
     * Ищем метод отражением, чтобы jar продолжал работать на ядрах без него.
     */
    private boolean applyGlintOverride() {

        // На API 1.16.5 метода ещё нет: ветка успеха недостижима в тестах,
        // но обязательна на ядрах 1.20.5+
        Method method = findMetaMethod(GLINT_OVERRIDE_METHOD);

        if (method == null) {
            return false;
        }

        try {

            method.invoke(meta, Boolean.TRUE);
            return true;

        } catch (Throwable ignored) {
            return false;
        }

    }

    /**
     * Ищет метод меты, которого нет в compile-time API.
     *
     * <p>Имя приходит параметром, потому что литерал в {@code getMethod}
     * непосредственно у {@link ItemMeta} IDE считает ошибкой: метод добавлен
     * ядрами позже той версии API, против которой собран jar.</p>
     *
     * @param name имя метода меты
     * @return метод или {@code null}, если ядро его не содержит
     */
    private static @Nullable Method findMetaMethod(@NotNull String name) {
        try {
            return ItemMeta.class.getMethod(name, Boolean.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    /**
     * Запасной вариант для старых ядер: невидимое зачарование + скрытый флаг.
     * Блеск — косметика, поэтому любая ошибка молча игнорируется.
     */
    private void applyFakeEnchantment() {

        try {

            Enchantment enchantment = resolveGlowEnchantment();

            if (enchantment == null) {
                return;
            }

            meta.addEnchant(enchantment, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        } catch (Throwable ignored) {
            // Предмет остаётся без блеска — на функционал это не влияет
        }

    }

    /**
     * В новых ядрах зачарование называется UNBREAKING, в старых — DURABILITY.
     * {@code Enchantment.getByName} ищем так же защищённо: в 1.21+ тип стал интерфейсом.
     */
    private @Nullable Enchantment resolveGlowEnchantment() {

        for (String name : GLOW_ENCHANTMENTS) {

            Enchantment enchantment = findEnchantment(name);

            if (enchantment != null) {
                return enchantment;
            }

        }

        return null;

    }

    @SuppressWarnings("deprecation")
    private @Nullable Enchantment findEnchantment(@NotNull String name) {

        try {

            // getByName помечен deprecated, но только он работает одновременно
            // на старых ядрах (перечисление) и на новых (реестр).
            return Enchantment.getByName(name);

        } catch (Throwable ignored) {
            return null;
        }

    }
}