package eu.neydev.expbottle.model;

import org.jetbrains.annotations.NotNull;

/**
 * Результат чтения метки бутылки из {@code PersistentDataContainer}.
 *
 * @param bottle является ли предмет бутылкой опыта
 * @param levels количество уровней внутри
 * @param forged подпись отсутствует или не сходится: предмет подделан
 */
public record BottleData(boolean bottle, int levels, boolean forged) {

    private static final BottleData EMPTY = new BottleData(false, 0, false);

    public static @NotNull BottleData empty() {
        return EMPTY;
    }

    /**
     * Бутылка считается повреждённой, если её метка не проходит валидацию.
     * Так отсекаются предметы с отредактированным NBT.
     *
     * @param maxLevels предел уровней из конфига
     * @return true если бутылку нельзя выдавать игроку
     */
    public boolean isBroken(int maxLevels) {
        return bottle && (levels <= 0 || levels > maxLevels);
    }
}