package eu.neydev.expbottle;

import be.seeseemelk.mockbukkit.entity.PlayerMock;
import eu.neydev.expbottle.event.BottleThrowHandler;
import eu.neydev.expbottle.model.BottleData;
import eu.neydev.expbottle.util.ExperienceFormula;
import org.bukkit.Material;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Физика бутылки: бросок, разрушение, подделка подписи, выдача опыта и защита от копирования в креативе.
 */
class BottlePhysicsIntegrationTest extends PluginTestHarness {

    @Test
    @DisplayName("Our bottle can be thrown like a vanilla one")
    void thrownBottleIsAllowedToFly() {

        PlayerMock player = operator("Ney");
        ItemStack bottle = plugin.getServices().getBottleFactory().create(10);
        player.getInventory().setItemInMainHand(bottle);

        PlayerInteractEvent event = rightClick(player, bottle);
        assertNotEquals(Event.Result.DENY, event.useItemInHand(),
                "The throw of our bottle must not be cancelled");

    }

    @Test
    @DisplayName("Safe mode: the click uses the bottle at once, no projectile and no orbs")
    void safeModeUsesBottleWithoutProjectile() throws IOException {

        setConfigValue("safe_mode: false", "safe_mode: true");
        plugin.getServices().reload();

        PlayerMock player = operator("Ney");
        player.setLevel(10);
        ItemStack bottle = plugin.getServices().getBottleFactory().create(5);
        player.getInventory().setItemInMainHand(bottle);

        PlayerInteractEvent event = rightClick(player, bottle);

        assertTrue(event.isCancelled(), "The vanilla throw must not happen in safe mode");
        assertEquals(15, player.getLevel(), "The stored levels arrive immediately");
        assertNull(findBottle(player), "The bottle is consumed by the click");

        boolean projectileFound = player.getWorld().getEntities().stream()
                .anyMatch(entity -> entity instanceof ThrownExpBottle);
        assertFalse(projectileFound, "Safe mode must not launch a projectile at all");

    }

    @Test
    @DisplayName("A damaged bottle drops no experience when it breaks")
    void brokenBottleReleasesNothing() {

        int maxLevels = plugin.getConfigManager().getMaxBottleLevels();
        ItemStack bottle = plugin.getServices().getBottleTagService()
                .tag(new ItemStack(Material.EXPERIENCE_BOTTLE), maxLevels + 500);

        BottleThrowHandler handler = new BottleThrowHandler(plugin.getServices());
        assertEquals(0, handler.handleBreak(bottle, null, null),
                "A bottle above the level cap must drop nothing");

    }

    @Test
    @DisplayName("A bottle without the server signature counts as forged and is rejected")
    void forgedBottleIsRejected() {

        ItemStack forged = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = forged.getItemMeta();
        meta.getPersistentDataContainer().set(
                plugin.getServices().getBottleTagService().getLevelsKey(),
                PersistentDataType.INTEGER, 500);
        forged.setItemMeta(meta);

        BottleData data = plugin.getServices().getBottleTagService().read(forged);
        assertTrue(data.bottle());
        assertTrue(data.forged(), "A bottle without a signature must be detected as forged");

        PlayerMock player = operator("Ney");
        player.getInventory().setItemInMainHand(forged);

        PlayerInteractEvent event = rightClick(player, forged);
        assertEquals(Event.Result.DENY, event.useItemInHand(), "A forgery cannot be thrown");
        assertTrue(drainMessages(player).contains("not created by the server"));

        BottleThrowHandler handler = new BottleThrowHandler(plugin.getServices());
        assertEquals(0, handler.handleBreak(forged, player.getLocation(), player),
                "A forgery must not drop experience");

    }

    @Test
    @DisplayName("A valid bottle drops exactly the stored experience")
    void validBottleReleasesStoredExperience() {

        ItemStack bottle = plugin.getServices().getBottleFactory().create(10);
        BottleThrowHandler handler = new BottleThrowHandler(plugin.getServices());

        assertEquals(ExperienceFormula.expFromLevels(10),
                handler.handleBreak(bottle, null, null),
                "The orbs must hold exactly the experience stored in the bottle");
        assertEquals(1, plugin.getServices().getDiagnosticsService().getBottlesUsed());

    }

    @Test
    @DisplayName("A break next to a player grants levels instead of raw experience points")
    void breakNearPlayerGrantsLevels() {

        PlayerMock player = operator("Ney");
        player.setLevel(30);

        ItemStack bottle = plugin.getServices().getBottleFactory().create(5);
        BottleThrowHandler handler = new BottleThrowHandler(plugin.getServices());

        assertEquals(0, handler.handleBreak(bottle, player.getLocation(), null),
                "No orbs drop while a player is nearby");
        assertEquals(35, player.getLevel(), "The five bottle levels were added to thirty");

    }

    @Test
    @DisplayName("A break in an empty place drops orbs so the experience is not lost")
    void breakFarAwayDropsOrbs() {

        PlayerMock player = operator("Ney");
        player.setLevel(30);

        ItemStack bottle = plugin.getServices().getBottleFactory().create(5);
        BottleThrowHandler handler = new BottleThrowHandler(plugin.getServices());

        org.bukkit.Location far = new org.bukkit.Location(player.getWorld(), 1000, 100, 1000);

        assertEquals(ExperienceFormula.expFromLevels(5), handler.handleBreak(bottle, far, null),
                "Far from players the orbs drop for the full bottle value");
        assertEquals(30, player.getLevel(), "Levels are not granted directly");

    }

    @Test
    @DisplayName("Creative cannot clone our bottles")
    void creativeCloneIsCancelled() {

        PlayerMock player = operator("Ney");
        player.performCommand("exp");

        ItemStack bottle = plugin.getServices().getBottleFactory().create(5);

        InventoryCreativeEvent event = new InventoryCreativeEvent(
                player.getOpenInventory(), InventoryType.SlotType.CONTAINER, 10, bottle);
        event.setCurrentItem(bottle);

        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled(), "The creative bottle clone must be cancelled");

    }
}
