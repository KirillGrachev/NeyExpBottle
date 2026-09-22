package eu.neydev.expbottle.model;

/**
 * Исход обмена опыта на бутылку.
 *
 * <p>Сервис обмена не отправляет сообщения и не играет звуки — он только
 * сообщает причину. Текст и звук выбирает вызывающий слой, поэтому одну и ту же
 * логику переиспользуют меню, команды и другие плагины.</p>
 */
public enum ExchangeOutcome {

    /** Обмен выполнен. */
    SUCCESS,

    /** Плагин выключен в config.yml. */
    PLUGIN_DISABLED,

    /** Некорректное количество уровней. */
    INVALID_AMOUNT,

    /** Не хватает прав. */
    NO_PERMISSION,

    /** Игрок ещё в кулдауне. */
    ON_COOLDOWN,

    /** Не хватает уровней. */
    NOT_ENOUGH_LEVELS,

    /** Не хватает пустых пузырьков. */
    NOT_ENOUGH_BOTTLES,

    /** Обмен отменён внешним плагином через BottleExchangeEvent. */
    CANCELLED;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
