package eu.neydev.expbottle.service;

import eu.neydev.expbottle.util.ExperienceFormula;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Работа с опытом игрока.
 *
 * <p>Единица расчёта — уровни (как и задумано в конфиге), но любое изменение
 * синхронизирует все три поля игрока: {@code level}, {@code exp} и
 * {@code totalExperience}. Старый код менял только первые два, из-за чего
 * суммарный опыт расходился с реальным (это видно при смерти и другим плагинам).</p>
 */
public class ExperienceService {

    /**
     * Допуск для сравнения дробных уровней: {@code float} в полосе опыта
     * хранит значение с погрешностью.
     */
    private static final double EPSILON = 0.0001D;

    /**
     * Суммарный опыт игрока.
     *
     * @param player игрок
     * @return опыт с учётом заполненности полосы
     */
    public int getTotalExperience(@NotNull Player player) {
        return ExperienceFormula.totalOf(player.getLevel(), clampProgress(player.getExp()));
    }

    /**
     * Точное количество уровней вместе с прогрессом (например, {@code 12.37}).
     *
     * @param player игрок
     * @return уровни с дробной частью
     */
    public double getExactLevels(@NotNull Player player) {
        return player.getLevel() + clampProgress(player.getExp());
    }

    /**
     * Заполненность полосы опыта в процентах.
     *
     * @param player игрок
     * @return целое число от 0 до 100
     */
    public int getProgressPercent(@NotNull Player player) {
        return (int) Math.round(clampProgress(player.getExp()) * 100.0D);
    }

    /**
     * Хватает ли игроку уровней.
     *
     * @param player игрок
     * @param levels сколько уровней нужно
     * @return true если хватает
     */
    public boolean hasLevels(@NotNull Player player, int levels) {

        if (levels <= 0) {
            return true;
        }

        return getExactLevels(player) + EPSILON >= levels;

    }

    /**
     * Прибавляет уровни, сохраняя дробный прогресс.
     *
     * @param player игрок
     * @param levels сколько уровней прибавить
     */
    public void addLevels(@NotNull Player player, int levels) {
        setExactLevels(player, getExactLevels(player) + levels);
    }

    /**
     * Списывает уровни, если их достаточно.
     *
     * @param player игрок
     * @param levels сколько уровней списать
     * @return true если списание выполнено
     */
    public boolean removeLevels(@NotNull Player player, int levels) {

        if (levels <= 0) {
            return true;
        }

        if (!hasLevels(player, levels)) {
            return false;
        }

        setExactLevels(player, Math.max(0.0D, getExactLevels(player) - levels));
        return true;

    }

    /**
     * Устанавливает точное количество уровней (целая часть — уровень, дробная — полоса).
     *
     * @param player игрок
     * @param levels уровни вместе с прогрессом
     */
    public void setExactLevels(@NotNull Player player, double levels) {

        double safeLevels = Math.max(0.0D, levels);

        int level = (int) Math.floor(safeLevels);
        float progress = clampProgress((float) (safeLevels - level));

        player.setLevel(level);
        player.setExp(progress);
        player.setTotalExperience(ExperienceFormula.totalOf(level, progress));

    }

    private float clampProgress(float progress) {

        if (Float.isNaN(progress)) {
            return 0.0f;
        }

        return Math.min(1.0f, Math.max(0.0f, progress));

    }
}
