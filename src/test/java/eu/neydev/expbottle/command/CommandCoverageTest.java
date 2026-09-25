package eu.neydev.expbottle.command;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.gui.Menu;
import eu.neydev.expbottle.gui.MenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Покрытие команд: все ветки /exp и /neyexpbottle вместе с tab-complete.
 */
class CommandCoverageTest {

    private ServerMock server;
    private NeyExpBottle plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(NeyExpBottle.class);
    }

    @AfterEach
    void tearDown() {
        server.getScheduler().cancelTasks(plugin);
        MockBukkit.unmock();
    }

    private PlayerMock granted(String name) {

        PlayerMock player = server.addPlayer(name);

        for (String permission : List.of("expbottle.use", "expbottle.exchange", "expbottle.admin")) {
            player.addAttachment(plugin, permission, true);
        }

        return player;

    }

    private String drain(PlayerMock player) {

        StringBuilder builder = new StringBuilder();
        String message;

        while ((message = player.nextMessage()) != null) {
            builder.append(message.toLowerCase(Locale.ROOT)).append(' ');
        }

        return builder.toString();

    }

    private Menu findMenu(PlayerMock player) {

        InventoryView view = player.getOpenInventory();

        if (view == null || view.getTopInventory() == null) {
            return null;
        }

        return view.getTopInventory().getHolder() instanceof MenuHolder holder ? holder.getMenu() : null;

    }

    @Test
    @DisplayName("/exp: console, permission denial, all open forms")
    void expCommandBranches() {

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "exp");

        PlayerMock guest = server.addPlayer("Guest");
        guest.addAttachment(plugin, "expbottle.use", false);
        guest.performCommand("exp");
        assertTrue(drain(guest).contains("permission"));
        assertNull(findMenu(guest));

        PlayerMock player = granted("Ney");

        player.performCommand("exp");
        assertEquals("exchange", findMenu(player).getName());
        player.closeInventory();

        player.performCommand("exp open exchange");
        assertEquals("exchange", findMenu(player).getName());
        player.closeInventory();

        player.performCommand("exp exchange");
        assertTrue(drain(player).contains("usage"));

    }

    @Test
    @DisplayName("/exp exchange: success, a broken argument and going over the cap")
    void expExchangeBranches() {

        PlayerMock player = granted("Ney");
        player.setLevel(20);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 2));

        player.performCommand("exp exchange 5");
        assertEquals(15, player.getLevel());

        player.performCommand("exp exchange not_a_number");
        assertTrue(drain(player).contains("tier"));

        player.performCommand("exp exchange 999999");
        assertTrue(drain(player).contains("invalid") || drain(player).contains("value"));

    }

    @Test
    @DisplayName("/exp exchange with an amount and with all")
    void expExchangeWithAmount() {

        PlayerMock player = granted("Ney");
        player.setLevel(40);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 5));

        player.performCommand("exp exchange 5 3");
        assertEquals(25, player.getLevel(), "Three bottles of 5 levels");

        player.performCommand("exp exchange 5 all");
        assertEquals(15, player.getLevel(), "all stopped at the two empty bottles left");

        player.performCommand("exp exchange 5 0");
        assertTrue(drain(player).contains("invalid") || drain(player).contains("value"));

    }

    @Test
    @DisplayName("/exp tab-complete suggests subcommands, menus and button ids")
    void expTabComplete() {

        ExpCommand command = new ExpCommand(plugin);
        Command stub = new Command("exp") {
            @Override
            public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                return false;
            }
        };

        PlayerMock player = granted("Ney");

        List<String> first = command.onTabComplete(player, stub, "exp", new String[]{""});
        assertTrue(first.contains("exchange"));
        assertTrue(first.contains("open"));
        assertTrue(first.contains("exchange-menu".replace("-menu", "")), "The exchange menu is in the suggestions");

        List<String> second = command.onTabComplete(player, stub, "exp", new String[]{"exchange", ""});
        assertTrue(second.contains("tier_5"));

        List<String> menus = command.onTabComplete(player, stub, "exp", new String[]{"open", "ex"});
        assertTrue(menus.contains("exchange"));

        assertTrue(command.onTabComplete(player, stub, "exp", new String[]{"open", "exchange", "x"}).isEmpty());

        PlayerMock guest = server.addPlayer("Guest");
        guest.addAttachment(plugin, "expbottle.use", false);
        assertTrue(command.onTabComplete(guest, stub, "exp", new String[]{""}).isEmpty());

    }

    @Test
    @DisplayName("/neyexpbottle: help, reload, give, info and menu")
    void adminCommandBranches() {

        PlayerMock admin = granted("Admin");

        admin.performCommand("neyexpbottle");
        assertTrue(drain(admin).contains("commands"));

        admin.performCommand("neyexpbottle help");
        assertTrue(drain(admin).contains("commands"));

        admin.performCommand("neyexpbottle reload");
        assertTrue(drain(admin).contains("reloaded"));

        admin.performCommand("neyexpbottle give");
        assertTrue(drain(admin).contains("usage"));

        admin.performCommand("neyexpbottle give NoSuchPlayer tier_5");
        assertTrue(drain(admin).contains("not online"));

        admin.performCommand("neyexpbottle give Admin not_a_button");
        assertTrue(drain(admin).contains("tier") || drain(admin).contains("exist"));

        admin.performCommand("neyexpbottle give Admin 5 99");
        assertTrue(drain(admin).contains("invalid") || drain(admin).contains("value"));

        admin.performCommand("neyexpbottle give Admin tier_5 2");
        assertEquals(2, countBottles(admin), "Two bottles were given");

        admin.performCommand("neyexpbottle info");
        assertTrue(drain(admin).contains("diagnostics"));

        admin.performCommand("neyexpbottle info NoSuchPlayer");
        assertTrue(drain(admin).contains("not online"));

        admin.performCommand("neyexpbottle info Admin");
        assertTrue(drain(admin).contains("diagnostics"));

        admin.performCommand("neyexpbottle menu");
        assertTrue(drain(admin).contains("usage"));

        admin.performCommand("neyexpbottle menu exchange NoSuchPlayer");
        assertTrue(drain(admin).contains("not online"));

        admin.performCommand("neyexpbottle menu exchange Admin");
        assertNotNull(findMenu(admin), "The menu was opened by the admin command");

        admin.performCommand("neyexpbottle something_unknown");
        assertTrue(drain(admin).contains("commands"), "An unknown subcommand shows the help");

    }

    @Test
    @DisplayName("/neyexpbottle without permissions denies everything")
    void adminCommandWithoutPermission() {

        PlayerMock guest = server.addPlayer("Guest");
        guest.addAttachment(plugin, "expbottle.admin", false);

        guest.performCommand("neyexpbottle reload");
        guest.performCommand("neyexpbottle give Guest tier_5");
        guest.performCommand("neyexpbottle info");
        guest.performCommand("neyexpbottle menu exchange");

        String messages = drain(guest);
        assertFalse(messages.contains("reloaded"), "A reload without the permission is impossible");
        assertTrue(messages.contains("permission"));

    }

    @Test
    @DisplayName("/neyexpbottle tab-complete across all branches")
    void adminTabComplete() {

        AdminCommand command = new AdminCommand(plugin);
        Command stub = new Command("neyexpbottle") {
            @Override
            public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                return false;
            }
        };

        PlayerMock admin = granted("Admin");

        assertTrue(command.onTabComplete(admin, stub, "n", new String[]{""}).contains("reload"));
        assertTrue(command.onTabComplete(admin, stub, "n", new String[]{"menu", ""}).contains("exchange"));
        assertTrue(command.onTabComplete(admin, stub, "n", new String[]{"give", ""}).contains(admin.getName()));
        assertTrue(command.onTabComplete(admin, stub, "n", new String[]{"info", ""}).contains(admin.getName()));
        assertTrue(command.onTabComplete(admin, stub, "n", new String[]{"give", "Admin", ""}).contains("tier_5"));
        assertTrue(command.onTabComplete(admin, stub, "n", new String[]{"menu", "exchange", ""}).contains(admin.getName()));
        assertTrue(command.onTabComplete(admin, stub, "n", new String[]{"reload", "x"}).isEmpty());

        PlayerMock guest = server.addPlayer("Guest");
        guest.addAttachment(plugin, "expbottle.admin", false);
        assertTrue(command.onTabComplete(guest, stub, "n", new String[]{""}).isEmpty());

    }

    private int countBottles(PlayerMock player) {

        int total = 0;

        for (ItemStack item : player.getInventory().getContents()) {

            if (item != null && plugin.getServices().getBottleTagService().isBottle(item)) {
                total += item.getAmount();
            }

        }

        return total;

    }
}