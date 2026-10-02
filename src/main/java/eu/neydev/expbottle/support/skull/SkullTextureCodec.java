package eu.neydev.expbottle.support.skull;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Преобразование записи текстуры головы в base64-блок профиля и обратно.
 *
 * <p>Конфигурация принимает текстуру в трёх видах: готовый base64/JSON-блок,
 * полная ссылка на скин или короткий хеш {@code textures.minecraft.net}.
 * Профиль игрока всегда хранит JSON вида {@code {"textures":{"SKIN":{"url":...}}}},
 * поэтому любой ввод приводится к нему. Класс чистый: ни Bukkit, ни отражения.</p>
 */
public final class SkullTextureCodec {

    private static final String TEXTURE_URL_PREFIX = "https://textures.minecraft.net/texture/";
    private static final String URL_KEY = "\"url\"";

    private SkullTextureCodec() {
    }

    /**
     * Превращает любую запись текстуры в base64-блок профиля.
     */
    public static @NotNull String toBase64Payload(@NotNull String texture) {

        if (texture.startsWith("{") || texture.contains("\"textures\"")) {
            return texture;
        }

        if (isReadyPayload(texture)) {
            return texture;
        }

        String url = texture.startsWith("http") ? texture : TEXTURE_URL_PREFIX + texture;

        return Base64.getEncoder().encodeToString(
                ("{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}").getBytes(StandardCharsets.UTF_8));

    }

    /**
     * Готовый base64-блок профиля (формат head-баз): после декода это JSON
     * с полем {@code textures}.
     *
     * <p>Без этой проверки блок заворачивался второй раз как хеш: в ссылку скина
     * попадала сама base64-строка, и клиент не мог загрузить текстуру.</p>
     *
     * @param texture запись текстуры
     * @return true если это уже готовый base64-блок
     */
    private static boolean isReadyPayload(@NotNull String texture) {

        String json = decode(texture);

        return json.startsWith("{") && json.contains("\"textures\"");

    }

    /**
     * Достаёт ссылку на текстуру из base64-блока профиля.
     *
     * <p>Блок профиля может быть как минифицированным ({@code "url":"..."}),
     * так и «красивым» из head-баз ({@code "url" : "..."} с пробелами вокруг
     * двоеточия) — поиск это учитывает. Если блок разобрать не удалось
     * (готовый JSON без url, битая строка), возвращается исходное значение —
     * Paper принимает и его.</p>
     */
    public static @NotNull String skinUrl(@NotNull String payload) {

        String json = payload.startsWith("{") ? payload : decode(payload);
        int index = json.indexOf(URL_KEY);

        if (index < 0) {
            return payload;
        }

        int colon = json.indexOf(':', index + URL_KEY.length());
        int start = colon < 0 ? -1 : json.indexOf('"', colon + 1);

        if (start < 0) {
            return payload;
        }

        start++;
        int end = json.indexOf('"', start);

        return end > start ? json.substring(start, end) : json.substring(start);

    }

    private static @NotNull String decode(@NotNull String payload) {
        try {
            return new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return payload;
        }
    }
}