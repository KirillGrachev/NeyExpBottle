package eu.neydev.expbottle.support.skull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Разбор записи текстуры головы: хеш, ссылка и готовый base64-блок
 * приводятся к JSON профиля, а ссылка достаётся обратно.
 */
class SkullTextureCodecTest {

    @Test
    @DisplayName("A texture hash turns into a base64 profile block")
    void buildsPayloadFromTextureHash() {

        String payload = SkullTextureCodec.toBase64Payload("abc123");

        String json = new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);

        assertTrue(json.contains("https://textures.minecraft.net/texture/abc123"));
        assertTrue(json.contains("\"SKIN\""));

    }

    @Test
    @DisplayName("A full url is used without conversion")
    void keepsFullUrl() {

        String payload = SkullTextureCodec.toBase64Payload("https://example.com/skin.png");
        String json = new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);

        assertTrue(json.contains("https://example.com/skin.png"));

    }

    @Test
    @DisplayName("A ready JSON block starting with a brace is not rebuilt")
    void keepsRawJsonBlock() {
        String raw = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/x\"}}}";
        assertEquals(raw, SkullTextureCodec.toBase64Payload(raw));
    }

    @Test
    @DisplayName("A line with the textures key is returned as is even without a leading brace")
    void keepsStringContainingTexturesKey() {
        String raw = "prefix-\"textures\"-suffix";
        assertEquals(raw, SkullTextureCodec.toBase64Payload(raw));
    }

    @Test
    @DisplayName("The texture url is extracted back from the base64 block")
    void extractsSkinUrlFromPayload() {
        String payload = SkullTextureCodec.toBase64Payload("abc123");
        assertEquals("https://textures.minecraft.net/texture/abc123", SkullTextureCodec.skinUrl(payload));
    }

    @Test
    @DisplayName("The url is also extracted from a ready JSON block")
    void extractsSkinUrlFromRawJson() {
        String raw = "{\"textures\":{\"SKIN\":{\"url\":\"https://example.com/skin.png\"}}}";
        assertEquals("https://example.com/skin.png", SkullTextureCodec.skinUrl(raw));
    }

    @Test
    @DisplayName("A broken block without a url is returned as is")
    void returnsPayloadWhenUrlMissing() {
        assertEquals("not-a-payload", SkullTextureCodec.skinUrl("not-a-payload"));
    }

    @Test
    @DisplayName("An unclosed quote after the url does not break the extraction")
    void extractsSkinUrlWithoutClosingQuote() {
        assertEquals("abc", SkullTextureCodec.skinUrl("{\"url\":\"abc"));
    }

    @Test
    @DisplayName("A ready base64 block is not wrapped a second time as a hash")
    void keepsReadyBase64Payload() {

        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/c10a\"}}}";
        String payload = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        assertEquals(payload, SkullTextureCodec.toBase64Payload(payload),
                "A ready base64 block must pass through the codec without a second wrap");
        assertEquals("http://textures.minecraft.net/texture/c10a", SkullTextureCodec.skinUrl(payload),
                "The skin url comes out of a ready block unchanged");

    }

    @Test
    @DisplayName("A pretty-printed profile block from a head database keeps its url reachable")
    void extractsSkinUrlFromPrettyPrintedPayload() {

        String json = "{\n"
                + "  \"profileName\" : \"mattijs\",\n"
                + "  \"textures\" : {\n"
                + "    \"SKIN\" : {\n"
                + "      \"url\" : \"http://textures.minecraft.net/texture/20ea\"\n"
                + "    }\n"
                + "  }\n"
                + "}";

        String payload = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        assertEquals(payload, SkullTextureCodec.toBase64Payload(payload),
                "A ready base64 block must pass through the codec without a second wrap");
        assertEquals("http://textures.minecraft.net/texture/20ea", SkullTextureCodec.skinUrl(payload),
                "The url must be found even with spaces around the colon");

    }

    @Test
    @DisplayName("The profile UUID is derived from the texture and stays the same between redraws")
    void profileUuidIsDerivedFromTexture() {

        String payload = SkullTextureCodec.toBase64Payload("abc123");
        String other = SkullTextureCodec.toBase64Payload("def456");

        assertEquals(GameProfileFactory.stableUuid(payload), GameProfileFactory.stableUuid(payload),
                "The same texture gives the same UUID on every redraw");
        assertNotEquals(GameProfileFactory.stableUuid(payload), GameProfileFactory.stableUuid(other),
                "Different textures give different UUIDs");

    }
}