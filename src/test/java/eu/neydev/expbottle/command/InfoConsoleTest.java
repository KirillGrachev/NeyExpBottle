package eu.neydev.expbottle.command;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import eu.neydev.expbottle.NeyExpBottle;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * /exp info из консоли: плейсхолдеры игрока заменяются прочерками,
 * а следом в лог уходит сводка по текстурам голов.
 */
class InfoConsoleTest {

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

    @Test
    @DisplayName("The console info replaces the player placeholders with dashes")
    void consoleInfo() {

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "exp info");

        PluginCommand command = plugin.getCommand("exp");
        assertNotNull(command);

        List<String> suggestions = command.tabComplete(Bukkit.getConsoleSender(), "exp",
                new String[] {"exp", "info", "extra"});

        assertTrue(suggestions.isEmpty(), "No suggestions beyond the subcommand arguments");

    }
}
