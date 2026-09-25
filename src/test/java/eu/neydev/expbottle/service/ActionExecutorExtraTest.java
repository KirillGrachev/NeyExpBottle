package eu.neydev.expbottle.service;

import eu.neydev.expbottle.PluginTestHarness;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.gui.action.ActionType;
import eu.neydev.expbottle.gui.action.ClickAction;
import eu.neydev.expbottle.util.Placeholders;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Действие [exchange] с количеством: токен all, идентификатор кнопки тира
 * и граничные значения количества, а также пустые аргументы звука и консоли.
 */
class ActionExecutorExtraTest extends PluginTestHarness {

    private PlayerMock readyPlayer(String name) {

        PlayerMock player = granted(name);
        player.setLevel(30);
        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 64));
        return player;

    }

    @Test
    @DisplayName("The exchange action accepts the all token and a tier id")
    void exchangeAllAndTierId() {

        PlayerMock player = readyPlayer("Ney");
        ActionExecutor executor = services.getActionExecutor();

        executor.execute(player, new ClickAction(ActionType.EXCHANGE, "1 all"), Placeholders.create());
        assertNotNull(player.nextMessage(), "The all exchange answers with a message");

        executor.execute(player, new ClickAction(ActionType.EXCHANGE, "tier_5 1"), Placeholders.create());
        assertNotNull(player.nextMessage(), "The tier id resolves to its levels");

    }

    @Test
    @DisplayName("A broken amount token answers with the invalid amount message")
    void exchangeInvalidAmount() {

        PlayerMock player = readyPlayer("Ney");
        ActionExecutor executor = services.getActionExecutor();

        for (String amount : List.of("0", "abc", "577")) {

            executor.execute(player, new ClickAction(ActionType.EXCHANGE, "1 " + amount), Placeholders.create());

            String message = drainMessages(player);
            assertFalse(message.isEmpty(), "amount " + amount + " must answer");
            assertTrue(message.contains("invalid value"), "amount " + amount + ": " + message);

        }

    }

    @Test
    @DisplayName("Empty sound and console arguments are silent no-ops")
    void emptyArguments() {

        PlayerMock player = granted("Ney");
        ActionExecutor executor = services.getActionExecutor();

        executor.execute(player, new ClickAction(ActionType.SOUND, ""), Placeholders.create());
        executor.execute(player, new ClickAction(ActionType.CONSOLE, ""), Placeholders.create());
        executor.execute(player, new ClickAction(ActionType.CONSOLE, "/neyexpbottle-definitely-unknown"), Placeholders.create());

    }
}
