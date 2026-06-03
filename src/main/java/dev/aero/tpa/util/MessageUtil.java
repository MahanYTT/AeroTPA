package dev.aero.tpa.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Utility for parsing color codes and placeholders
public final class MessageUtil {

    // Regex to match hex codes
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private MessageUtil() {
    }

    // Translates hex codes and standard color codes
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) return "";

        // Convert hex codes to legacy format
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);

        // Translate standard color codes
        return sb.toString().replace('&', '§');
    }

    // Convert string to Adventure Component
    public static Component toComponent(String message) {
        return LegacyComponentSerializer.legacySection().deserialize(colorize(message));
    }

    // Replace placeholders in string
    public static String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (message == null) return "";
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    // Get a fully formatted message component
    public static Component formatMessage(String prefix, String rawMessage, Map<String, String> placeholders) {
        String msg = prefix + replacePlaceholders(rawMessage, placeholders);
        return toComponent(msg);
    }

    // Get a formatted message component without placeholders
    public static Component formatMessage(String prefix, String rawMessage) {
        return toComponent(prefix + rawMessage);
    }
}
