package eu.neydev.expbottle.service;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Операции с инвентарём игрока.
 *
 * <p>Работаем только с {@code getStorageContents()}: старый код перебирал
 * {@code getContents()}, куда входят броня и вторая рука, и мог удалить
 * надетый на игрока предмет.</p>
 */
public class InventoryService {

    private final DiagnosticsService diagnosticsService;

    public InventoryService(@NotNull DiagnosticsService diagnosticsService) {
        this.diagnosticsService = diagnosticsService;
    }

    /**
     * Считает предметы в основном инвентаре (36 слотов).
     *
     * @param player   игрок
     * @param material материал
     * @return суммарное количество
     */
    public int count(@NotNull Player player, @NotNull Material material) {
        return count(player.getInventory().getStorageContents(), material);
    }

    private int count(ItemStack @NotNull [] contents, @NotNull Material material) {

        int count = 0;

        for (ItemStack item : contents) {

            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }

        }

        return count;

    }

    public boolean has(@NotNull Player player, @NotNull Material material, int amount) {
        return count(player, material) >= amount;
    }

    /**
     * Удаляет предметы, только если их достаточно.
     *
     * <p>Проверка и списание идут одним вызовом, поэтому частичного списания
     * (и дюпа на ошибках) не возникает. Старый код удалял «первый похожий стек»
     * прямо во время перебора — это могло задеть не тот стек.</p>
     *
     * <p>Слоты меняются точечно через {@code setItem}: перезапись всего массива
     * {@code setStorageContents} задевает и те слоты, которых операция не касалась.</p>
     *
     * @param player   игрок
     * @param material материал
     * @param amount   сколько удалить
     * @return true если предметы удалены
     */
    public boolean remove(@NotNull Player player, @NotNull Material material, int amount) {

        if (amount <= 0) {
            return true;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();

        if (count(contents, material) < amount) {
            return false;
        }

        int left = amount;

        for (int slot = 0; slot < contents.length && left > 0; slot++) {

            ItemStack item = contents[slot];

            if (item == null || item.getType() != material) {
                continue;
            }

            int itemAmount = item.getAmount();

            if (itemAmount > left) {

                ItemStack rest = item.clone();
                rest.setAmount(itemAmount - left);

                inventory.setItem(slot, rest);
                left = 0;

            } else {

                inventory.setItem(slot, null);
                left -= itemAmount;

            }

        }

        return left == 0;

    }

    /**
     * Выдаёт предмет, а при полном инвентаре — выбрасывает под ноги.
     * Предмет не теряется ни при каком раскладе.
     *
     * @param player игрок
     * @param item   предмет
     */
    public void giveOrDrop(@NotNull Player player, @NotNull ItemStack item) {

        Map<Integer, ItemStack> left = player.getInventory().addItem(item);

        if (left.isEmpty()) {
            return;
        }

        for (ItemStack rest : left.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }

        diagnosticsService.debug("Inventory of " + player.getName() + " is full - the item is dropped on the ground");

    }
}