package eu.neydev.expbottle.support;

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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Поведение подбора без поднятого сервера: все ветки гибнут на отражении,
 * а предупреждение честно называет ядро неизвестным.
 */
class SkullSupportNoCoreTest {

    private SkullTextureService service;

    @BeforeEach
    void freshService() {
        service = new SkullTextureService(logger);
    }

    private Logger logger;
    private List<String> logged;

    @BeforeEach
    void setUp() {


        logged = new ArrayList<>();
        logger = Logger.getLogger("skull-nocore-" + System.nanoTime());
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
    }

    @Test
    @DisplayName("Without a core every strategy dies and the warning names the core unknown")
    void warnsUnknownCore() {

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        // Фабрика предметов живёт на сервере, а сервер не поднят: мета берётся прямо у мока
        SkullMeta meta = new be.seeseemelk.mockbukkit.inventory.meta.SkullMetaMock();

        ItemStack result = service.apply(item, meta, null, "http://textures/nocore.png");

        assertSame(item, result);
        assertEquals(1, service.getFailedCount());
        assertEquals(1, logged.size());
        assertTrue(logged.get(0).contains("unknown core"));

    }
}
