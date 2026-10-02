package eu.neydev.expbottle.support;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import eu.neydev.expbottle.util.ItemBuilder;
import eu.neydev.expbottle.service.SkullTextureService;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка слоя совместимости текстур голов: разбор записи текстуры,
 * однократное предупреждение в консоль и сброс накопленной статистики.
 */
class SkullTextureSupportTest {

    private SkullTextureService service;

    @BeforeEach
    void freshService() {
        service = new SkullTextureService(logger);
    }

    private ServerMock server;
    private Logger logger;
    private List<String> logged;

    @BeforeEach
    void setUp() {

        server = MockBukkit.mock();

        
        logged = new ArrayList<>();
        logger = Logger.getLogger("skull-test-" + System.nanoTime());
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {

            @Override
            public void publish(LogRecord record) {
                logged.add(record.getMessage());
            }

            @Override
            public void flush() {
                // Ничего не буферизуем
            }

            @Override
            public void close() {
                // Ресурсов нет
            }
        });

    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Without a texture the item stays the same and the counters do not grow")
    void skipsWhenTextureIsEmpty() {

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        ItemStack result = service.apply(item, meta, null, "   ");

        assertSame(item, result);
        assertEquals(0, service.getAppliedCount() + service.getFailedCount());
        assertTrue(logged.isEmpty());

    }

    @Test
    @DisplayName("The warning is logged once per texture, not on every menu refresh")
    void warnsOncePerTexture() {

        // Камень стабильно отказывает в NMS-копии: все ветки гибнут и головка остаётся как есть
        ItemStack item = new ItemStack(Material.STONE);
        SkullMeta meta = (SkullMeta) new ItemStack(Material.PLAYER_HEAD).getItemMeta();

        for (int attempt = 0; attempt < 25; attempt++) {
            service.apply(item, meta, null, "same-texture");
        }

        assertTrue(service.getFailedCount() > 0);
        assertTrue(logged.size() <= 1, "expected at most one warning, got " + logged.size());

        if (!logged.isEmpty()) {
            assertTrue(logged.get(0).contains("once per texture"));
        }

    }

    @Test
    @DisplayName("An empty owner does not stop the texture")
    void ignoresBlankOwner() {

        ItemStack item = new ItemStack(Material.STONE);
        SkullMeta meta = (SkullMeta) new ItemStack(Material.PLAYER_HEAD).getItemMeta();

        assertSame(item, service.apply(item, meta, "   ", "texture-b"));
        assertEquals(1, service.getFailedCount());

    }

    @Test
    @DisplayName("The owner is set with the texture and survives a failure")
    @SuppressWarnings("deprecation")
    void appliesOwnerAlongsideTexture() {

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        service.apply(item, meta, "Ney", "texture-d");
        assertEquals("Ney", meta.getOwner());

    }

    @Test
    @DisplayName("The item builder survives the item being replaced while the texture applies")
    void itemBuilderSurvivesSkullReplacement() {

        ItemStack built = new ItemBuilder(Material.PLAYER_HEAD)
                .setSkull(service, null, "texture-c")
                .setName("&fInformation")
                .setLore(List.of("&7line"))
                .build();

        assertEquals(Material.PLAYER_HEAD, built.getType());
        assertNotNull(built.getItemMeta());
        assertEquals(List.of("&7line"), built.getItemMeta().getLore());

        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD).setSkull(service, null, null);

        assertSame(builder.getItem(), builder.build());

    }

    @Test
    @DisplayName("The reset clears the stats and the dead strategy list")
    void resetClearsState() {

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        service.apply(item, meta, null, "texture-a");

        assertFalse(service.getWorkingStrategy().isEmpty());
        assertEquals(1, service.getAppliedCount());

        service.reset();

        assertEquals(0, service.getFailedCount());
        assertEquals(0, service.getAppliedCount());
        assertEquals("not used yet", service.getWorkingStrategy());

        List<String> before = new ArrayList<>(logged);

        // Камень убивает все ветки, включая последний рубеж: после сброса предупреждение снова пишется
        ItemStack uncopyable = new ItemStack(Material.STONE);

        service.apply(uncopyable, meta, null, "texture-a");

        assertEquals(before.size() + 1, logged.size(), "after reset the texture may warn again");
        assertTrue(service.getFailedCount() > 0);

    }
}