package eu.neydev.expbottle.config.type;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Настройки выбора количества бутылок (секция {@code settings.amount}).
 *
 * <p>Количество переключается правым кликом по кнопке обмена прямо на кнопке:
 * отдельного меню количества нет.</p>
 *
 * @param amounts           варианты количества; токен {@code all} — всё, что игрок может себе позволить
 * @param allLabel          подпись варианта {@code all} в переключателе и подсказках
 * @param switcherActive    шаблон строки переключателя для выбранного варианта
 * @param switcherInactive  шаблон строки переключателя для остальных вариантов
 * @param hint              подсказка правой кнопки под переключателем
 * @param cycleSound        звук переключения количества
 * @param cycleOnRightClick глобальный тумблер переключения количества правым кликом;
 *                          меню может переопределить его своим {@code cycle_amount}
 */
public record AmountSettings(@NotNull List<String> amounts, @NotNull String allLabel,
                             @NotNull String switcherActive, @NotNull String switcherInactive,
                             @NotNull String hint, @NotNull SoundSettings cycleSound,
                             boolean cycleOnRightClick) {
}