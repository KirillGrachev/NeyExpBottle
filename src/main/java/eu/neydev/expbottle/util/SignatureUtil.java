package eu.neydev.expbottle.util;

import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Подпись бутылок ключом сервера.
 *
 * <p>Каждая бутылка несёт HMAC от количества уровней. Предмет, созданный вручную
 * через {@code /give} или отредактированный по NBT, подписи не имеет и
 * отклоняется как подделка.</p>
 *
 * <p>Ключ живёт в {@code plugins/NeyExpBottle/secret.key} и генерируется один раз:
 * перенос мира на другой сервер без этого файла обнулит подписи, поэтому файл
 * нельзя удалять и нельзя показывать игрокам.</p>
 */
public final class SignatureUtil {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SECRET_FILE = "secret.key";
    private static final int SECRET_LENGTH = 32;
    private static final int SIGNATURE_CHARS = 16;

    private SignatureUtil() {
    }

    /**
     * Читает ключ сервера, при первом запуске создаёт его.
     *
     * @param dataFolder папка плагина
     * @param logger     логгер для предупреждений
     * @return ключ или пустой массив, если ключ недоступен
     */
    public static byte @NotNull [] loadOrCreateSecret(@NotNull File dataFolder, @NotNull Logger logger) {

        File file = new File(dataFolder, SECRET_FILE);

        if (file.exists()) {

            try {
                return Files.readAllBytes(file.toPath());
            } catch (IOException exception) {
                logger.warning("Failed to read " + SECRET_FILE
                        + ": bottle signatures are disabled - " + exception.getMessage());
                return new byte[0];
            }

        }

        byte[] secret = new byte[SECRET_LENGTH];
        new SecureRandom().nextBytes(secret);

        try {

            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                logger.warning("Failed to create the plugin folder - the key is not saved");
                return secret;
            }

            Files.write(file.toPath(), secret);

        } catch (IOException exception) {
            logger.warning("Failed to save " + SECRET_FILE + ": " + exception.getMessage());
        }

        return secret;

    }

    /**
     * Подписывает количество уровней.
     *
     * @param secret ключ сервера
     * @param levels уровни бутылки
     * @return подпись (16 hex-символов) или пустая строка без ключа
     */
    public static @NotNull String sign(byte @NotNull [] secret, int levels) {

        if (secret.length == 0) {
            return "";
        }

        try {

            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));

            byte[] digest = mac.doFinal(String.valueOf(levels).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return toHex(digest).substring(0, SIGNATURE_CHARS);

        } catch (Exception exception) {
            return "";
        }

    }

    /**
     * Проверяет подпись.
     *
     * @param secret    ключ сервера
     * @param levels    уровни бутылки
     * @param signature подпись из предмета
     * @return true если предмет подписан этим сервером
     */
    public static boolean verify(byte @NotNull [] secret, int levels, @NotNull String signature) {

        if (secret.length == 0 || signature.isEmpty()) {
            return false;
        }

        String expected = sign(secret, levels);
        return !expected.isEmpty() && expected.equalsIgnoreCase(signature);

    }

    private static @NotNull String toHex(byte @NotNull [] bytes) {

        StringBuilder builder = new StringBuilder(bytes.length * 2);

        for (byte value : bytes) {
            builder.append(String.format(Locale.ROOT, "%02x", value));
        }

        return builder.toString();

    }
}
