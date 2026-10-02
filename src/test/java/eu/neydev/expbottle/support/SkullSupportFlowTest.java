package eu.neydev.expbottle.support;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Подбор способа на ядре, где жив последний рубеж (NMS-тег): сработавшая ветка
 * запоминается, мёртвые больше не дёргаются, а предмет возвращается зеркалом.
 */
class SkullSupportFlowTest {

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
        logger = Logger.getLogger("skull-flow-" + System.nanoTime());
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
    @DisplayName("The nms tag strategy applies and becomes the working one")
    void appliesViaNmsTag() {

        ItemStack item = head();

        ItemStack result = service.apply(item, meta(), "Ney", "http://textures/flow.png");

        assertNotSame(item, result, "the tag path returns the mirrored item");
        assertEquals(Material.PLAYER_HEAD, result.getType());
        assertEquals("nms_tag", service.getWorkingStrategy());
        assertEquals(1, service.getAppliedCount());
        assertEquals(0, service.getFailedCount());
        assertTrue(logged.isEmpty());

    }

    @Test
    @DisplayName("A second refresh skips the dead strategies and applies again")
    void secondRefreshSkipsDead() {

        ItemStack item = head();

        service.apply(item, meta(), null, "http://textures/flow-2.png");
        service.apply(item, meta(), null, "http://textures/flow-3.png");

        assertEquals(2, service.getAppliedCount());
        assertEquals("nms_tag", service.getWorkingStrategy());

    }

    @Test
    @DisplayName("An uncopyable item kills every strategy and warns once")
    void uncopyableItemKillsEveryStrategy() {

        ItemStack item = new ItemStack(Material.STONE);

        service.apply(item, meta(), null, "http://textures/dead.png");
        service.apply(item, meta(), null, "http://textures/dead.png");

        assertEquals(2, service.getFailedCount());
        assertEquals(0, service.getAppliedCount());
        assertEquals("not used yet", service.getWorkingStrategy());
        assertEquals(1, logged.size(), "the warning is shown once per texture");
        assertTrue(logged.get(0).contains("unknown core") || logged.get(0).contains("mockbukkit"));

    }

    private ItemStack head() {
        return new ItemStack(Material.PLAYER_HEAD);
    }

    private SkullMeta meta() {
        return (SkullMeta) head().getItemMeta();
    }
}
