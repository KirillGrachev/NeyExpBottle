package eu.neydev.expbottle.util;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилита для обработки цветов в строках.
 *
 * <p>Поддерживаемые форматы:</p>
 * <ul>
 *     <li>{@code &a}, {@code &l} — классические коды;</li>
 *     <li>{@code #RRGGBB} и {@code &#RRGGBB} — HEX (на ядрах до 1.16 заменяется ближайшим legacy-цветом);</li>
 *     <li>{@code <gradient:#RRGGBB:#RRGGBB>текст</gradient>} — плавный переход.</li>
 * </ul>
 *
 * <p>Шаблоны компилируются один раз: в старой реализации {@code Pattern.compile}
 * вызывался на каждое сообщение, то есть на каждый клик по меню.</p>
 */
public final class HexColorUtil {

    /** #RRGGBB или &#RRGGBB — в конфигах встречаются обе формы. */
    private static final Pattern HEX_PATTERN = Pattern.compile("&?#([a-fA-F0-9]{6})");

    /** <gradient:#RRGGBB:#RRGGBB>текст</gradient> */
    private static final Pattern GRADIENT_PATTERN = Pattern.compile(
            "<gradient:#([a-fA-F0-9]{6}):#([a-fA-F0-9]{6})>(.*?)</gradient>",
            Pattern.DOTALL
    );

    private static final char[] LEGACY_CODES = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private static final Color[] LEGACY_COLORS = {
            new Color(0x000000), new Color(0x0000AA), new Color(0x00AA00), new Color(0x00AAAA),
            new Color(0xAA0000), new Color(0xAA00AA), new Color(0xFFAA00), new Color(0xAAAAAA),
            new Color(0x555555), new Color(0x5555FF), new Color(0x55FF55), new Color(0x55FFFF),
            new Color(0xFF5555), new Color(0xFF55FF), new Color(0xFFFF55), new Color(0xFFFFFF)
    };

    private HexColorUtil() {
    }

    /**
     * Преобразует цветовые коды строки в формат, который понимает клиент.
     *
     * @param text исходная строка
     * @return строка с {@code §}-кодами
     */
    public static @NotNull String color(@Nullable String text) {

        if (text == null || text.isEmpty()) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes('&', applyHex(applyGradients(text)));

    }

    /**
     * Преобразует список строк построчно.
     *
     * @param lines исходные строки
     * @return новый список с {@code §}-кодами
     */
    public static @NotNull List<String> color(@Nullable List<String> lines) {

        List<String> result = new ArrayList<>();

        if (lines == null || lines.isEmpty()) {
            return result;
        }

        for (String line : lines) {
            result.add(color(line));
        }

        return result;

    }

    /**
     * Убирает из строки все цветовые коды и собственные теги.
     *
     * @param text исходная строка
     * @return текст без оформления
     */
    public static @NotNull String strip(@Nullable String text) {

        if (text == null || text.isEmpty()) {
            return "";
        }

        String withoutGradients = GRADIENT_PATTERN.matcher(text).replaceAll("$3");
        String withoutHex = HEX_PATTERN.matcher(withoutGradients).replaceAll("");
        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', withoutHex));

        return stripped == null ? "" : stripped;

    }

    /**
     * Раскрывает HEX-коды в {@code &x&R&R&G&G&B&B}.
     */
    private static @NotNull String applyHex(@NotNull String text) {

        Matcher matcher = HEX_PATTERN.matcher(text);

        if (!matcher.find()) {
            return text;
        }

        boolean hexSupported = ServerVersion.isHexSupported();

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        do {

            result.append(text, lastEnd, matcher.start());

            Color color = decode(matcher.group(1));
            result.append(hexSupported ? toHexCode(color) : "&" + nearestLegacyCode(color));

            lastEnd = matcher.end();

        } while (matcher.find());

        result.append(text, lastEnd, text.length());
        return result.toString();

    }

    /**
     * Раскрывает теги {@code <gradient>} в последовательность HEX-кодов.
     */
    private static @NotNull String applyGradients(@NotNull String text) {

        Matcher matcher = GRADIENT_PATTERN.matcher(text);

        if (!matcher.find()) {
            return text;
        }

        boolean hexSupported = ServerVersion.isHexSupported();

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        do {

            result.append(text, lastEnd, matcher.start());

            Color start = decode(matcher.group(1));
            Color end = decode(matcher.group(2));

            result.append(gradient(matcher.group(3), start, end, hexSupported));
            lastEnd = matcher.end();

        } while (matcher.find());

        result.append(text, lastEnd, text.length());
        return result.toString();

    }

    /**
     * Красит каждый символ текста своим цветом перехода.
     */
    private static @NotNull String gradient(@NotNull String text, @NotNull Color start,
                                            @NotNull Color end, boolean hexSupported) {

        if (text.isEmpty()) {
            return "";
        }

        int last = Math.max(1, text.length() - 1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {

            char symbol = text.charAt(i);

            if (symbol == ' ') {
                result.append(symbol);
                continue;
            }

            Color color = interpolate(start, end, (float) i / last);
            result.append(hexSupported ? toHexCode(color) : "&" + nearestLegacyCode(color)).append(symbol);

        }

        return result.toString();

    }

    /**
     * Промежуточный цвет между двумя границами.
     */
    private static @NotNull Color interpolate(@NotNull Color start, @NotNull Color end, float ratio) {

        float safeRatio = Math.min(1.0f, Math.max(0.0f, ratio));

        int red = Math.round(start.getRed() + (end.getRed() - start.getRed()) * safeRatio);
        int green = Math.round(start.getGreen() + (end.getGreen() - start.getGreen()) * safeRatio);
        int blue = Math.round(start.getBlue() + (end.getBlue() - start.getBlue()) * safeRatio);

        return new Color(red, green, blue);

    }

    /**
     * Цвет -> {@code &x&R&R&G&G&B&B}.
     */
    private static @NotNull String toHexCode(@NotNull Color color) {

        String rgb = Integer.toHexString(color.getRGB() & 0xFFFFFF);
        StringBuilder result = new StringBuilder("&x");

        for (int i = rgb.length(); i < 6; i++) {
            result.append("&0");
        }

        for (int i = 0; i < rgb.length(); i++) {
            result.append('&').append(rgb.charAt(i));
        }

        return result.toString();

    }

    /**
     * Ближайший legacy-цвет — запасной вариант для ядер старее 1.16.
     */
    private static char nearestLegacyCode(@NotNull Color color) {

        int bestIndex = 0;
        long bestDistance = Long.MAX_VALUE;

        for (int i = 0; i < LEGACY_COLORS.length; i++) {

            Color legacy = LEGACY_COLORS[i];

            long distance = distance(color, legacy);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }

        }

        return LEGACY_CODES[bestIndex];

    }

    private static long distance(@NotNull Color first, @NotNull Color second) {

        long red = first.getRed() - second.getRed();
        long green = first.getGreen() - second.getGreen();
        long blue = first.getBlue() - second.getBlue();

        return red * red + green * green + blue * blue;

    }

    /**
     * Разбирает шесть HEX-символов в цвет.
     */
    private static @NotNull Color decode(@NotNull String hex) {
        return new Color(Integer.parseInt(hex, 16));
    }
}
