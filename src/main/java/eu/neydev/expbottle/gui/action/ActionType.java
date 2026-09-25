package eu.neydev.expbottle.gui.action;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * Типы действий, которые можно повесить на клик, открытие или закрытие меню.
 *
 * <p>В конфиге записываются как {@code [тег] аргумент}, например
 * {@code [message] &cНедостаточно опыта}.</p>
 */
public enum ActionType {

    /** Отправить игроку сообщение. */
    MESSAGE("message"),

    /** Закрыть меню. */
    CLOSE("close"),

    /** Выполнить команду от имени консоли. */
    CONSOLE("console"),

    /** Выполнить команду от имени игрока. */
    PLAYER("player"),

    /** Проиграть звук: {@code ИМЯ[:громкость[:высота]]}. */
    SOUND("sound"),

    /** Перерисовать динамические предметы меню. */
    REFRESH("refresh"),

    /** Открыть другое меню по имени. */
    OPEN("open"),

    /** Отправить текст в чат от имени игрока. */
    CHAT("chat"),

    /** Обменять уровни на бутылку: {@code [exchange] 5} или {@code [exchange] tier_5}. */
    EXCHANGE("exchange"),

    /** Вернуться в родительское меню (то, из которого открыли текущее). */
    BACK("back"),

    /** Отправить сообщение всем игрокам сервера. */
    BROADCAST("broadcast"),

    /** Заглушка: ничего не делать. */
    NONE("none");

    private final String tag;

    ActionType(@NotNull String tag) {
        this.tag = tag;
    }

    public @NotNull String getTag() {
        return tag;
    }

    /**
     * Ищет тип действия по тегу из конфига.
     *
     * @param tag текст в квадратных скобках
     * @return тип действия или {@code null}
     */
    public static @Nullable ActionType fromTag(@Nullable String tag) {

        if (tag == null || tag.trim().isEmpty()) {
            return null;
        }

        String normalized = tag.trim().toLowerCase(Locale.ROOT);

        for (ActionType type : values()) {

            if (type.tag.equals(normalized)) {
                return type;
            }

        }

        return null;

    }

    public static @NotNull Optional<ActionType> find(@Nullable String tag) {
        return Optional.ofNullable(fromTag(tag));
    }
}