package eu.neydev.expbottle.service;

import eu.neydev.expbottle.support.skull.CraftProfilePropertiesStrategy;
import eu.neydev.expbottle.support.skull.GameProfileFieldStrategy;
import eu.neydev.expbottle.support.skull.NmsTagStrategy;
import eu.neydev.expbottle.support.skull.PaperProfileStrategy;
import eu.neydev.expbottle.support.skull.ReserializedProfileStrategy;
import eu.neydev.expbottle.support.skull.SkullApplyStrategy;
import eu.neydev.expbottle.support.skull.SkullTextureCodec;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Совместимость текстур голов между ядрами.
 *
 * <p>Способов поставить кастомную текстуру несколько, и каждый работает только
 * на своей группе ядер. Сервис перебирает {@link SkullApplyStrategy} по очереди,
 * запоминает сработавший и больше не повторяет заведомо мёртвые ветки. Сами
 * способы и отражение к NMS живут в пакете {@code support.skull}; здесь только
 * подбор, статистика и однократное предупреждение в консоль.</p>
 *
 * <p>Состояние принадлежит экземпляру сервиса, а не классу: тесты поднимают
 * по сервису на проверке и не сбрасывают глобальные списки между собой,
 * а перезагрузка плагин-сборщиком пересоздаёт сервис вместе с остальными.</p>
 *
 * <p>Предупреждение пишется один раз на текстуру: меню обновляется раз в секунду,
 * и без этого лог заполнялся бы одинаковой строкой.</p>
 */
public class SkullTextureService {

    /** Сколько разных текстур могут дать предупреждение в лог. */
    private static final int MAX_WARNED_TEXTURES = 3;

    /** Способы применения текстуры — от самого надёжного к самому запасному. */
    private static final List<SkullApplyStrategy> STRATEGIES = List.of(
            new PaperProfileStrategy(),
            new CraftProfilePropertiesStrategy(),
            new GameProfileFieldStrategy(),
            new ReserializedProfileStrategy(),
            new NmsTagStrategy());

    private final Logger logger;

    private final Set<String> warned = new LinkedHashSet<>();
    private final Set<String> deadStrategies = new LinkedHashSet<>();

    private final AtomicInteger applied = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();

    private final AtomicReference<String> workingStrategy = new AtomicReference<>("not used yet");

    /** Все способы проверены и ни один не подошёл ядру — перебор больше не нужен. */
    private volatile boolean allStrategiesDead;

    public SkullTextureService(@NotNull Logger logger) {
        this.logger = logger;
    }

    /**
     * Применяет владельца и текстуру к голове.
     *
     * @param item    предмет, которому принадлежит мета
     * @param meta    мета головы
     * @param owner   ник владельца или {@code null}
     * @param texture текстура: base64-блок, ссылка или хеш текстуры
     * @return предмет с применённой текстурой (может отличаться от входного)
     */
    @SuppressWarnings("deprecation")
    public @NotNull ItemStack apply(@NotNull ItemStack item, @NotNull SkullMeta meta,
                                    @Nullable String owner, @Nullable String texture) {

        String ownerName = owner == null ? null : owner.trim();

        if (texture == null || texture.trim().isEmpty()) {

            if (ownerName != null && !ownerName.isEmpty()) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerName));
            }

            return item;

        }

        String payload = SkullTextureCodec.toBase64Payload(texture.trim());

        if (!allStrategiesDead) {

            for (SkullApplyStrategy strategy : STRATEGIES) {

                if (isDead(strategy)) {
                    continue;
                }

                try {

                    ItemStack appliedItem = strategy.apply(meta, item, payload, ownerName);

                    if (appliedItem != null) {
                        rememberSuccess(strategy);
                        return appliedItem;
                    }

                } catch (Throwable ignored) {
                    // Ветка не работает на этом ядре — больше её не пробуем
                }

                markDead(strategy);

            }

        }

        failed.incrementAndGet();
        warnOnce(payload);

        return item;

    }

    public @NotNull String getWorkingStrategy() {
        return workingStrategy.get();
    }

    public int getAppliedCount() {
        return applied.get();
    }

    public int getFailedCount() {
        return failed.get();
    }

    /**
     * Сбрасывает статистику и список мёртвых веток — нужно перезагрузке и тестам.
     */
    public void reset() {

        synchronized (warned) {
            warned.clear();
        }

        synchronized (deadStrategies) {
            deadStrategies.clear();
        }

        applied.set(0);
        failed.set(0);
        workingStrategy.set("not used yet");
        allStrategiesDead = false;

    }

    private boolean isDead(@NotNull SkullApplyStrategy strategy) {

        synchronized (deadStrategies) {
            return deadStrategies.contains(strategy.id());
        }

    }

    private void markDead(@NotNull SkullApplyStrategy strategy) {

        synchronized (deadStrategies) {

            deadStrategies.add(strategy.id());
            allStrategiesDead = deadStrategies.size() >= STRATEGIES.size();

        }

    }

    private void rememberSuccess(@NotNull SkullApplyStrategy strategy) {

        applied.incrementAndGet();
        workingStrategy.set(strategy.id());

    }

    /**
     * Первое предупреждение по текстуре — и тишина, чтобы обновление меню
     * не превращало консоль в простыню.
     */
    private void warnOnce(@NotNull String payload) {

        String key = payload.length() > 32 ? payload.substring(0, 32) : payload;

        synchronized (warned) {

            if (warned.contains(key) || warned.size() >= MAX_WARNED_TEXTURES) {
                return;
            }

            warned.add(key);

        }

        logger.warning("Skull texture was not applied on this core (" + describeServer()
                + "), the head stays default. This line is shown once per texture, "
                + "tried strategies: " + String.join(", ", strategyNames()));

    }

    private @NotNull List<String> strategyNames() {

        return STRATEGIES.stream()
                .map(SkullApplyStrategy::id)
                .toList();

    }

    private @NotNull String describeServer() {

        try {
            return Bukkit.getServer().getClass().getPackage().getName() + ", " + Bukkit.getBukkitVersion();
        } catch (Throwable ignored) {
            // Недостижимо на живом сервере: Bukkit.getServer() есть всегда;
            // ветка страховки на случай вызова без поднятого ядра
            return "unknown core";
        }

    }
}
