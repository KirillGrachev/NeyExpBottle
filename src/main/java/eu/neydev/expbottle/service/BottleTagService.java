package eu.neydev.expbottle.service;

import eu.neydev.expbottle.model.BottleData;
import eu.neydev.expbottle.util.SignatureUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Чтение и запись метки бутылки в {@code PersistentDataContainer}.
 *
 * <p>Ключи создаются один раз в конструкторе: старый код собирал
 * {@code new NamespacedKey(plugin, ...)} на каждую проверку предмета.</p>
 *
 * <p>Помимо уровней хранится подпись ключом сервера (HMAC): предмет без неё или
 * с чужой считается подделкой, поэтому бутылку нельзя выдать себе через
 * {@code /give} с руками в NBT.</p>

 */
public class BottleTagService {

    private static final String KEY_MARKER = "exp_bottle";
    private static final String KEY_LEVELS = "bottle_levels";
    private static final String KEY_SIGNATURE = "bottle_signature";
    private static final String KEY_SPENT = "bottle_spent";

    private final NamespacedKey markerKey;
    private final NamespacedKey levelsKey;
    private final NamespacedKey signatureKey;
    private final NamespacedKey spentKey;

    private final byte[] secret;

    public BottleTagService(@NotNull Plugin plugin) {

        this.markerKey = new NamespacedKey(plugin, KEY_MARKER);
        this.levelsKey = new NamespacedKey(plugin, KEY_LEVELS);
        this.signatureKey = new NamespacedKey(plugin, KEY_SIGNATURE);
        this.spentKey = new NamespacedKey(plugin, KEY_SPENT);

        this.secret = SignatureUtil.loadOrCreateSecret(plugin.getDataFolder(), plugin.getLogger());

    }

    /**
     * Читает метку предмета и проверяет подпись.
     *
     * @param item предмет (может быть {@code null})
     * @return данные бутылки или {@link BottleData#empty()}
     */
    public @NotNull BottleData read(@Nullable ItemStack item) {

        if (item == null || !item.hasItemMeta()) {
            return BottleData.empty();
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return BottleData.empty();
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();

        Integer levels = container.get(levelsKey, PersistentDataType.INTEGER);

        if (levels != null) {

            String signature = container.getOrDefault(signatureKey, PersistentDataType.STRING, "");
            boolean forged = !SignatureUtil.verify(secret, levels, signature);

            return new BottleData(true, levels, forged);

        }

        return BottleData.empty();

    }

    public boolean isBottle(@Nullable ItemStack item) {
        return read(item).bottle();
    }

    public int readLevels(@Nullable ItemStack item) {
        return read(item).levels();
    }

    /**
     * Помечает предмет снаряда как уже использованный.
     *
     * <p>Метка бутылки (уровни и подпись) стирается, поэтому повторная награда
     * при поломке невозможна даже для снаряда safe-режима, который летит
     * только ради анимации броска.</p>
     *
     * @param item предмет снаряда
     * @return тот же предмет с меткой использованного
     */
    public @NotNull ItemStack markSpent(@NotNull ItemStack item) {

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.remove(markerKey);
        container.remove(levelsKey);
        container.remove(signatureKey);
        container.set(spentKey, PersistentDataType.INTEGER, 1);

        item.setItemMeta(meta);
        return item;

    }

    /**
     * Проверяет, помечен ли предмет как использованный снаряд safe-режима.
     *
     * @param item предмет (может быть {@code null})
     * @return true если награда за этот предмет уже выдана
     */
    public boolean isSpent(@Nullable ItemStack item) {

        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        return meta != null && meta.getPersistentDataContainer().has(spentKey, PersistentDataType.INTEGER);

    }

    /**
     * Помечает предмет как бутылку опыта и подписывает её.
     *
     * @param item   предмет
     * @param levels количество уровней внутри
     * @return тот же предмет с меткой
     */
    public @NotNull ItemStack tag(@NotNull ItemStack item, int levels) {

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(markerKey, PersistentDataType.INTEGER, 1);
        container.set(levelsKey, PersistentDataType.INTEGER, levels);
        container.set(signatureKey, PersistentDataType.STRING, SignatureUtil.sign(secret, levels));

        item.setItemMeta(meta);
        return item;

    }

    public @NotNull NamespacedKey getMarkerKey() {
        return markerKey;
    }

    public @NotNull NamespacedKey getLevelsKey() {
        return levelsKey;
    }

    public @NotNull NamespacedKey getSignatureKey() {
        return signatureKey;
    }

}