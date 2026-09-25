package eu.neydev.expbottle.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка формул ванильного опыта.
 *
 * <p>Эталон — перебор, как это делает сам Minecraft: каждый следующий уровень
 * стоит {@code 2L+7}, {@code 5L-38} или {@code 9L-158} опыта в зависимости
 * от диапазона.</p>
 */
class ExperienceFormulaTest {

    /** Переборная реализация «в лоб» — эталон для сравнения. */
    private static int bruteTotalAtLevel(int level) {

        int total = 0;

        for (int current = 0; current < level; current++) {
            total += bruteToNextLevel(current);
        }

        return total;

    }

    private static int bruteToNextLevel(int level) {

        if (level <= 15) {
            return 2 * level + 7;
        }

        if (level <= 30) {
            return 5 * level - 38;
        }

        return 9 * level - 158;

    }

    @Test
    @DisplayName("toNextLevel matches the vanilla values")
    void toNextLevelMatchesVanilla() {

        assertEquals(7, ExperienceFormula.toNextLevel(0));
        assertEquals(9, ExperienceFormula.toNextLevel(1));
        assertEquals(37, ExperienceFormula.toNextLevel(15));
        assertEquals(42, ExperienceFormula.toNextLevel(16));
        assertEquals(112, ExperienceFormula.toNextLevel(30));
        assertEquals(121, ExperienceFormula.toNextLevel(31));
        assertEquals(130, ExperienceFormula.toNextLevel(32));

    }

    @Test
    @DisplayName("toNextLevel matches a brute force over the whole supported range")
    void toNextLevelMatchesBruteForce() {

        for (int level = 0; level <= 5000; level++) {
            assertEquals(bruteToNextLevel(level), ExperienceFormula.toNextLevel(level),
                    "Mismatch at level " + level);
        }

    }

    @Test
    @DisplayName("totalAtLevel matches a brute force")
    void totalAtLevelMatchesBruteForce() {

        assertEquals(0, ExperienceFormula.totalAtLevel(0));
        assertEquals(7, ExperienceFormula.totalAtLevel(1));
        assertEquals(352, ExperienceFormula.totalAtLevel(16));
        assertEquals(394, ExperienceFormula.totalAtLevel(17));
        assertEquals(1507, ExperienceFormula.totalAtLevel(31));
        assertEquals(1628, ExperienceFormula.totalAtLevel(32));

        for (int level = 0; level <= 3000; level++) {
            assertEquals(bruteTotalAtLevel(level), ExperienceFormula.totalAtLevel(level),
                    "Mismatch at level " + level);
        }

    }

    @Test
    @DisplayName("levelOf returns the exact level for any experience")
    void levelOfMatchesBruteForce() {

        int level = 0;
        int experience = 0;

        // Проходим перебором весь диапазон до 2 миллионов опыта
        while (experience <= 2_000_000) {

            assertEquals(level, ExperienceFormula.levelOf(experience),
                    "Mismatch at experience " + experience);

            experience += bruteToNextLevel(level);
            level++;

        }

    }

    @Test
    @DisplayName("levelOf is correct at the level boundaries")
    void levelOfHandlesBoundaries() {

        for (int level = 0; level <= 1000; level++) {

            int start = ExperienceFormula.totalAtLevel(level);
            int next = start + ExperienceFormula.toNextLevel(level);

            assertEquals(level, ExperienceFormula.levelOf(start), "Start of level " + level);
            assertEquals(level, ExperienceFormula.levelOf(next - 1), "End of level " + level);
            assertEquals(level + 1, ExperienceFormula.levelOf(next), "Transition to level " + (level + 1));

        }

    }

    @Test
    @DisplayName("Negative experience does not break the formulas")
    void handlesNegativeExperience() {

        assertEquals(0, ExperienceFormula.levelOf(-100));
        assertEquals(0, ExperienceFormula.expIntoLevel(-100));
        assertEquals(0.0f, ExperienceFormula.progressOf(-100));
        assertEquals(0, ExperienceFormula.totalAtLevel(-5));

    }

    @Test
    @DisplayName("progressOf is always within 0.0 - 1.0")
    void progressIsAlwaysInRange() {

        for (int experience = 0; experience <= 500_000; experience += 37) {

            float progress = ExperienceFormula.progressOf(experience);

            assertTrue(progress >= 0.0f && progress <= 1.0f,
                    "Progress out of range at experience " + experience + ": " + progress);

        }

    }

    @Test
    @DisplayName("At zero progress the experience bar is empty")
    void progressIsZeroAtLevelStart() {

        for (int level = 0; level <= 500; level++) {
            assertEquals(0.0f, ExperienceFormula.progressOf(ExperienceFormula.totalAtLevel(level)));
        }

    }

    @Test
    @DisplayName("totalOf(level, progress) is consistent with levelOf and progressOf")
    void totalOfIsReversible() {

        for (int level = 0; level <= 500; level++) {

            // Значения до 0.75 включительно гарантированно не округляются
            // до следующего уровня: стоимость уровня всегда не меньше 7 опыта
            for (float progress : new float[]{0.0f, 0.25f, 0.5f, 0.75f}) {

                int total = ExperienceFormula.totalOf(level, progress);

                assertEquals(level, ExperienceFormula.levelOf(total),
                        "The level was not restored: level=" + level + ", progress=" + progress);

                // Опыт хранится целым числом, поэтому прогресс квантуется
                // шагом в один опыт: round(progress * toNextLevel) / toNextLevel
                int required = ExperienceFormula.toNextLevel(level);
                float quantized = (float) Math.round(progress * required) / required;

                assertEquals(quantized, ExperienceFormula.progressOf(total), 0.0001f,
                        "Progress was not restored: level=" + level + ", progress=" + progress);

            }

        }

    }

    @Test
    @DisplayName("Progress that rounds to a whole level raises the level")
    void progressRoundsUpToNextLevel() {

        // На нулевом уровне всего 7 опыта, поэтому 0.99 * 7 = 6.93 -> 7.
        // Это честное округление целого опыта, а не потеря данных.
        assertEquals(1, ExperienceFormula.levelOf(ExperienceFormula.totalOf(0, 0.99f)));
        assertEquals(0.0f, ExperienceFormula.progressOf(ExperienceFormula.totalOf(0, 0.99f)));

    }

    @Test
    @DisplayName("expFromLevels counts the cost of levels from zero")
    void expFromLevelsMatchesBruteForce() {

        for (int levels = 0; levels <= 500; levels++) {
            assertEquals(bruteTotalAtLevel(levels), ExperienceFormula.expFromLevels(levels));
        }

    }

    @Test
    @DisplayName("The formulas are monotonic and do not overflow")
    void formulasAreMonotonicAndSafe() {

        for (int level = 0; level < ExperienceFormula.MAX_LEVEL; level++) {

            assertTrue(ExperienceFormula.totalAtLevel(level + 1) > ExperienceFormula.totalAtLevel(level),
                    "Monotonicity is violated at level " + level);

        }

        assertTrue(ExperienceFormula.totalAtLevel(ExperienceFormula.MAX_LEVEL) > 0);
        assertTrue(ExperienceFormula.toNextLevel(Integer.MAX_VALUE) > 0);
        assertEquals(ExperienceFormula.MAX_LEVEL, ExperienceFormula.levelOf(Integer.MAX_VALUE));

    }
}