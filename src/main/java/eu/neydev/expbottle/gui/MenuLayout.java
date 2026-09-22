package eu.neydev.expbottle.gui;

import eu.neydev.expbottle.config.type.FillMode;
import eu.neydev.expbottle.gui.item.MenuItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Раскладка меню: предметы, разложенные по способу заполнения.
 *
 * <p>Собирается один раз при создании меню, поэтому reload конфига не меняет
 * открытое окно под курсором игрока.</p>
 *
 * @param fullFill    предметы, заполняющие все слоты (рисуются первыми)
 * @param itemsBySlot предметы в конкретных слотах (поздние перекрывают ранние)
 * @param emptyFill   предметы, заполняющие только свободные слоты (рисуются последними)
 */
public record MenuLayout(@NotNull List<MenuItem> fullFill,
                         @NotNull Map<Integer, MenuItem> itemsBySlot,
                         @NotNull List<MenuItem> emptyFill) {

    /**
     * Раскладывает список предметов по слотам.
     *
     * @param items предметы (уже отсортированы по приоритету)
     * @return раскладка
     */
    public static @NotNull MenuLayout of(@NotNull List<MenuItem> items) {

        List<MenuItem> fullFill = new ArrayList<>();
        List<MenuItem> emptyFill = new ArrayList<>();
        Map<Integer, MenuItem> bySlot = new LinkedHashMap<>();

        for (MenuItem item : items) {

            if (item.getFillMode() == FillMode.ALL) {
                fullFill.add(item);
                continue;
            }

            if (item.getFillMode() == FillMode.EMPTY) {
                emptyFill.add(item);
                continue;
            }

            for (int slot : item.getSlots()) {
                bySlot.put(slot, item);
            }

        }

        return new MenuLayout(
                Collections.unmodifiableList(fullFill),
                Collections.unmodifiableMap(bySlot),
                Collections.unmodifiableList(emptyFill)
        );

    }

    /**
     * Предмет в указанном слоте.
     *
     * @param slot слот
     * @return предмет или {@code null}
     */
    public @Nullable MenuItem getItem(int slot) {
        return itemsBySlot.get(slot);
    }
}
