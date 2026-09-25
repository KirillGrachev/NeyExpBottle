package eu.neydev.expbottle.service;

import eu.neydev.expbottle.PluginTestHarness;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import eu.neydev.expbottle.NeyExpBottle;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Инвентарь под завязку: излишек выдачи падает под ноги, а снятие
 * несуществующих пузырьков честно отвечает false.
 */
class InventoryServiceDropTest extends PluginTestHarness {

    @Test
    @DisplayName("A full inventory drops the overflow at the player's feet")
    void fullInventoryDropsOverflow() {

        PlayerMock player = server.addPlayer("Ney");

        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Material.STONE, 64));
        }

        services.getInventoryService().giveOrDrop(player, new ItemStack(Material.EXPERIENCE_BOTTLE, 4));

        assertFalse(player.getInventory().contains(Material.EXPERIENCE_BOTTLE),
                "The overflow cannot fit and leaves the inventory");

    }

    @Test
    @DisplayName("Removing more than the player has answers false")
    void removeMoreThanPresent() {

        PlayerMock player = server.addPlayer("Ney");
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 2));

        assertFalse(services.getInventoryService().remove(player, Material.GLASS_BOTTLE, 5));

        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 5));
        assertTrue(services.getInventoryService().remove(player, Material.GLASS_BOTTLE, 5));

    }
}
