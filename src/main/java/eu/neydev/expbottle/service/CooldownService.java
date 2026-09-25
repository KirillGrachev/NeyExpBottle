package eu.neydev.expbottle.service;

import eu.neydev.expbottle.config.PluginConfig;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кулдаун на обмен.
 *
 * <p>Закрывает сценарий, когда игрок «прокликивает» меню быстрее, чем сервер
 * успевает обработать клик, и получает несколько бутылок за один опыт.</p>
 */
public class CooldownService {

    private final Map<UUID, Long> deadlines = new ConcurrentHashMap<>();

    private final PluginConfig config;
    private final PermissionService permissionService;

    public CooldownService(@NotNull PluginConfig config, @NotNull PermissionService permissionService) {
        this.config = config;
        this.permissionService = permissionService;
    }

    public boolean isOnCooldown(@NotNull Player player) {
        return getRemainingMillis(player) > 0L;
    }

    /**
     * Сколько миллисекунд осталось ждать.
     *
     * @param player игрок
     * @return остаток кулдауна, 0 если его нет
     */
    public long getRemainingMillis(@NotNull Player player) {

        if (!config.isCooldownEnabled() || permissionService.hasBypassCooldown(player)) {
            return 0L;
        }

        Long deadline = deadlines.get(player.getUniqueId());

        if (deadline == null) {
            return 0L;
        }

        long remaining = deadline - System.currentTimeMillis();

        if (remaining <= 0L) {
            deadlines.remove(player.getUniqueId());
            return 0L;
        }

        return remaining;

    }

    /**
     * Округлённый остаток кулдауна в секундах — для плейсхолдера {@code {cooldown}}.
     *
     * @param player игрок
     * @return секунды
     */
    public long getRemainingSeconds(@NotNull Player player) {
        return (long) Math.ceil(getRemainingMillis(player) / 1000.0D);
    }

    public void start(@NotNull Player player) {

        if (!config.isCooldownEnabled() || permissionService.hasBypassCooldown(player)) {
            return;
        }

        deadlines.put(player.getUniqueId(), System.currentTimeMillis() + config.getCooldownMillis());

    }

    public void clear(@NotNull Player player) {
        deadlines.remove(player.getUniqueId());
    }

    public void clear() {
        deadlines.clear();
    }

    /**
     * Удаляет протухшие записи, чтобы карта не росла бесконечно.
     *
     * @return сколько записей удалено
     */
    public int purgeExpired() {

        long now = System.currentTimeMillis();
        int removed = 0;

        Iterator<Map.Entry<UUID, Long>> iterator = deadlines.entrySet().iterator();

        while (iterator.hasNext()) {

            if (iterator.next().getValue() <= now) {
                iterator.remove();
                removed++;
            }

        }

        return removed;

    }

    public int size() {
        return deadlines.size();
    }
}