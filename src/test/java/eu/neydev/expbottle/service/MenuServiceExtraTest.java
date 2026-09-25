package eu.neydev.expbottle.service;

import eu.neydev.expbottle.PluginTestHarness;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.util.Placeholders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Отказы открытия меню: выключенный плагин, пустое и неизвестное имя,
 * отсутствующая папка меню, возврат родителю без открытого окна
 * и условие открытия с дефолтным текстом отказа.
 */
class MenuServiceExtraTest extends PluginTestHarness {

    @Test
    @DisplayName("Without any loadable menu the default open names the missing menu")
    void openWithoutMenus() throws IOException {

        File folder = new File(plugin.getDataFolder(), "menus");

        for (File file : folder.listFiles()) {
            assertTrue(file.delete());
        }

        // Перезагрузка вернёт встроенное меню: ломаем его yaml, чтобы реестр остался пустым
        services.reload();
        Files.writeString(new File(folder, "exchange.yml").toPath(), "menu: [broken\n", StandardCharsets.UTF_8);
        services.reload();

        PlayerMock player = granted("Ney");

        assertFalse(services.getMenuService().open(player));
        assertNotNull(player.nextMessage());

    }

    @Test
    @DisplayName("A blank or unknown menu name is a polite refusal")
    void openBlankAndUnknown() {

        PlayerMock player = granted("Ney");
        MenuService menuService = services.getMenuService();

        assertFalse(menuService.open(player, "   "));
        assertNotNull(player.nextMessage());

        assertFalse(menuService.open(player, "no-such-menu"));
        assertNotNull(player.nextMessage());

        assertFalse(menuService.openChild(player, "no-such-menu", null, Placeholders.create()));
        assertNotNull(player.nextMessage());

    }

    @Test
    @DisplayName("A disabled plugin refuses both open forms")
    void openDisabled() throws IOException {

        setConfig("enabled: true # Global plugin switch", "enabled: false # Global plugin switch");
        PlayerMock player = granted("Ney");
        MenuService menuService = services.getMenuService();

        assertFalse(menuService.open(player, "exchange"));
        assertNotNull(player.nextMessage());

        assertFalse(menuService.openChild(player, "exchange", null, Placeholders.create()));
        assertNotNull(player.nextMessage());

    }

    @Test
    @DisplayName("openParent without an open window is a silent no-op")
    void openParentWithoutWindow() {
        services.getMenuService().openParent(granted("Ney"));
    }

    @Test
    @DisplayName("A failed open requirement without a denial text uses the default denial")
    void openDeniedDefaultMessage() throws IOException {

        File folder = new File(plugin.getDataFolder(), "menus");
        Files.writeString(new File(folder, "denied.yml").toPath(), """
                menu:
                  title: "Denied"
                  size: 9
                  open_requirement: "{player_level} >= 999999"
                items:
                  deco:
                    type: DECORATION
                    slot: 4
                    material: STONE
                """, StandardCharsets.UTF_8);

        services.reload();
        PlayerMock player = granted("Ney");

        assertFalse(services.getMenuService().open(player, "denied"));

        String message = player.nextMessage();
        assertNotNull(message);
        assertFalse(message.contains("999999"), "The default denial text is used");

    }
}
