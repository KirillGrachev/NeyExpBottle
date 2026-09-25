package eu.neydev.expbottle.model;

import org.jetbrains.annotations.NotNull;

/**
 * Результат обмена.
 *
 * @param outcome     исход операции
 * @param levels      сколько уровней потрачено суммарно
 * @param usedBottles сколько пустых пузырьков потрачено
 * @param amount      сколько бутылок создано
 * @param remaining   сколько уровней осталось у игрока после обмена (в виде «уровень + прогресс»)
 */
public record ExchangeResult(@NotNull ExchangeOutcome outcome, int levels,
                             int usedBottles, int amount, double remaining) {

    public static @NotNull ExchangeResult success(int levels, int usedBottles, int amount, double remaining) {
        return new ExchangeResult(ExchangeOutcome.SUCCESS, levels, usedBottles, amount, remaining);
    }

    public static @NotNull ExchangeResult of(@NotNull ExchangeOutcome outcome) {
        return new ExchangeResult(outcome, 0, 0, 0, 0.0);
    }

    public static @NotNull ExchangeResult of(@NotNull ExchangeOutcome outcome, int levels) {
        return new ExchangeResult(outcome, levels, 0, 0, 0.0);
    }

    public boolean isSuccess() {
        return outcome.isSuccess();
    }
}