package eu.neydev.expbottle.gui.item;

/**
 * Тип предмета меню — определяет поведение по умолчанию.
 */
public enum MenuItemType {

    /** Декорация: клики игнорируются. */
    DECORATION,

    /** Информационный предмет: обновляется при каждом refresh. */
    INFO,

    /** Кнопка обмена опыта на бутылку. Без явных действий выполняет {@code [exchange] + [refresh]}. */
    TIER,

    /** Кнопка закрытия. Без явных действий выполняет {@code [close]}. */
    CLOSE,

    /** Произвольная кнопка: поведение полностью задаётся списком {@code click}. */
    CUSTOM;

    /**
     * Нужно ли перерисовывать предмет при обновлении меню, если {@code refresh} не задан явно.
     *
     * @return true для динамических типов
     */
    public boolean isDynamicByDefault() {
        return this == INFO || this == TIER;
    }
}
