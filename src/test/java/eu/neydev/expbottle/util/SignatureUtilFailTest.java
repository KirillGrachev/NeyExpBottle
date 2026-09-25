package eu.neydev.expbottle.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Отказы ключа подписи: нечитаемый файл, несоздаваемая папка и петля
 * симлинков на месте файла ключа.
 */
class SignatureUtilFailTest {

    private static final Logger LOGGER = Logger.getLogger("signature-fail-test");

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("An unreadable secret file disables signatures")
    void unreadableSecret() throws IOException {

        // Каталог на месте файла ключа: чтение бросает IOException
        File secret = new File(tempDir.toFile(), "secret.key");
        assertTrue(secret.mkdirs());

        byte[] secretBytes = SignatureUtil.loadOrCreateSecret(tempDir.toFile(), LOGGER);

        assertEquals(0, secretBytes.length);
        assertEquals("", SignatureUtil.sign(secretBytes, 5));

    }

    @Test
    @DisplayName("A folder that cannot be created keeps the key in memory only")
    void uncreatableFolder() throws IOException {

        File parentAsFile = new File(tempDir.toFile(), "parent-as-file");
        assertTrue(parentAsFile.createNewFile(), "The parent must stay a file");

        File dataFolder = new File(parentAsFile, "data");

        byte[] secret = SignatureUtil.loadOrCreateSecret(dataFolder, LOGGER);

        assertEquals(32, secret.length, "The key lives in memory even when it cannot be saved");

    }

    @Test
    @DisplayName("A symlink loop on the secret path is survived silently")
    void symlinkLoop() throws IOException {

        Path loop = tempDir.resolve("secret.key");
        Files.createSymbolicLink(loop, loop);

        byte[] secret = SignatureUtil.loadOrCreateSecret(tempDir.toFile(), LOGGER);

        assertEquals(32, secret.length, "The key is generated even when it cannot be written");
        assertEquals(16, SignatureUtil.sign(secret, 5).length());

    }
}
