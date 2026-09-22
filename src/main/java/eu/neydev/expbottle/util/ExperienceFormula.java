package eu.neydev.expbottle.util;

/**
 * Математика ванильного опыта Minecraft.
 *
 * <p>Все методы — чистые функции без обращений к Bukkit, поэтому покрыты юнит-тестами.
 * Вместо перебора уровней (O(n) на каждый рендер меню, как в старой реализации)
 * используются закрытые формулы — O(1).</p>
 */
public final class ExperienceFormula {

    /**
     * Технический предел: суммарный опыт выше этого уровня не помещается в int.
     */
    public static final int MAX_LEVEL = 21_000;

    private static final int FIRST_TIER_LIMIT = 15;
    private static final int SECOND_TIER_LIMIT = 30;

    private static final int SECOND_TIER_EXPERIENCE = 352;    // totalAtLevel(16)
    private static final int THIRD_TIER_EXPERIENCE = 1628;     // totalAtLevel(32)

    private ExperienceFormula() {
    }

    /**
     * Сколько опыта нужно, чтобы перейти с уровня {@code level} на следующий.
     *
     * @param level текущий уровень
     * @return стоимость следующего уровня
     */
    public static int toNextLevel(int level) {

        int safeLevel = clampLevel(level);
        long required;

        if (safeLevel <= FIRST_TIER_LIMIT) {
            required = 2L * safeLevel + 7L;
        } else if (safeLevel <= SECOND_TIER_LIMIT) {
            required = 5L * safeLevel - 38L;
        } else {
            required = 9L * safeLevel - 158L;
        }

        return clampToInt(required);

    }

    /**
     * Суммарный опыт, накопленный к началу уровня {@code level}.
     *
     * @param level уровень
     * @return опыт на «нулевом» прогрессе этого уровня
     */
    public static int totalAtLevel(int level) {

        int safeLevel = clampLevel(level);
        long total;

        if (safeLevel <= 16) {
            total = (long) safeLevel * safeLevel + 6L * safeLevel;
        } else if (safeLevel <= 31) {
            total = (5L * safeLevel * safeLevel - 81L * safeLevel + 720L) / 2L;
        } else {
            total = (9L * safeLevel * safeLevel - 325L * safeLevel + 4440L) / 2L;
        }

        return clampToInt(total);

    }

    /**
     * Суммарный опыт игрока по его уровню и заполненности полосы.
     *
     * @param level    уровень
     * @param progress заполненность полосы (0.0 — 1.0)
     * @return суммарный опыт
     */
    public static int totalOf(int level, float progress) {

        int safeLevel = clampLevel(level);
        float safeProgress = Math.min(1.0f, Math.max(0.0f, progress));

        long total = (long) totalAtLevel(safeLevel)
                + Math.round(safeProgress * toNextLevel(safeLevel));

        return clampToInt(total);

    }

    /**
     * Уровень, соответствующий суммарному опыту.
     *
     * @param totalExperience суммарный опыт
     * @return уровень игрока
     */
    public static int levelOf(int totalExperience) {

        long experience = Math.max(0L, (long) totalExperience);
        int level = clampLevel(approximateLevel(experience));

        // Коррекция погрешности floating-point: обычно ноль итераций, максимум пара
        while (level < MAX_LEVEL && totalAtLevel(level + 1) <= experience) {
            level++;
        }

        while (level > 0 && totalAtLevel(level) > experience) {
            level--;
        }

        return level;

    }

    /**
     * Опыт, накопленный внутри текущего уровня.
     *
     * @param totalExperience суммарный опыт
     * @return опыт сверх начала уровня
     */
    public static int expIntoLevel(int totalExperience) {

        int safeExperience = Math.max(0, totalExperience);
        return Math.max(0, safeExperience - totalAtLevel(levelOf(safeExperience)));

    }

    /**
     * Заполненность полосы опыта (0.0 — 1.0).
     *
     * @param totalExperience суммарный опыт
     * @return прогресс для {@code Player#setExp}
     */
    public static float progressOf(int totalExperience) {

        int safeExperience = Math.max(0, totalExperience);
        int required = toNextLevel(levelOf(safeExperience));

        if (required <= 0) {
            return 0.0f;
        }

        float progress = (float) expIntoLevel(safeExperience) / required;
        return Math.min(1.0f, Math.max(0.0f, progress));

    }

    /**
     * Сколько опыта «стоит» {@code levels} уровней, отсчитывая от нулевого.
     *
     * @param levels количество уровней
     * @return эквивалент в опыте
     */
    public static int expFromLevels(int levels) {
        return totalAtLevel(levels);
    }

    /**
     * Сколько полных уровней помещается в опыт.
     *
     * @param totalExperience суммарный опыт
     * @return количество уровней
     */
    public static int levelsFromExp(int totalExperience) {
        return levelOf(totalExperience);
    }

    /**
     * Приблизительный уровень по закрытым формулам — основа для {@link #levelOf(int)}.
     */
    private static int approximateLevel(long experience) {

        if (experience >= THIRD_TIER_EXPERIENCE) {
            return (int) ((Math.sqrt(72.0 * experience - 54215.0) + 325.0) / 18.0);
        }

        if (experience >= SECOND_TIER_EXPERIENCE) {
            return (int) (Math.sqrt(40.0 * experience - 7839.0) / 10.0 + 8.1);
        }

        return (int) (Math.sqrt(experience + 9.0) - 3.0);

    }

    private static int clampLevel(int level) {
        return Math.min(MAX_LEVEL, Math.max(0, level));
    }

    private static int clampToInt(long value) {

        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        if (value < 0L) {
            return 0;
        }

        return (int) value;

    }
}
