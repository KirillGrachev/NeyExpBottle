package eu.neydev.expbottle.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Держатель меню.
 *
 * <p>Меню живёт внутри самого инвентаря, а не в {@code Map<UUID, Menu>}: клик
 * сам приносит нужное меню в {@code getHolder()}, поэтому утечек и устаревших
 * окон после перезагрузки конфига не бывает.</p>
 */
public class MenuHolder implements InventoryHolder {

    private Menu menu;
    private Inventory inventory;

    /**
     * Ссылки проставляются сразу после создания инвентаря:
     * Bukkit требует holder ещё до того, как окно существует.
     *
     * @param menu      меню
     * @param inventory созданный инвентарь
     */
    public void initialize(@NotNull Menu menu, @NotNull Inventory inventory) {
        this.menu = menu;
        this.inventory = inventory;
    }

    public @NotNull Menu getMenu() {
        return menu;
    }

    @Override
    public @Nullable Inventory getInventory() {
        return inventory;
    }
}