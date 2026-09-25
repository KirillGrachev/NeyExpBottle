package eu.neydev.expbottle.registry;

import org.jetbrains.annotations.NotNull;

/**
 * Уровень обмена — кнопка типа {@code TIER} в одном из меню.
 *
 * @param id    идентификатор предмета в {@code items} (например, {@code tier_5})
 * @param levels сколько уровней списывается и кладётся в бутылку
 * @param menu  имя меню, в котором объявлена кнопка
 */
public record BottleTier(@NotNull String id, int levels, @NotNull String menu) {
}