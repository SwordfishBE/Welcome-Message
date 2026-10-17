package net.welcome_message;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

final class MessageTemplate {
    private final List<Part> parts;

    private MessageTemplate(List<Part> parts) {
        this.parts = List.copyOf(parts);
    }

    static MessageTemplate parse(String text) {
        List<Part> parts = new ArrayList<>();
        StringBuilder pending = new StringBuilder();
        Style style = Style.EMPTY;
        int position = 0;
        while (position < text.length()) {
            char character = text.charAt(position);
            if (character == '&' && position + 1 < text.length()) {
                char code = text.charAt(position + 1);
                if (code == '&') {
                    pending.append('&');
                    position += 2;
                    continue;
                }
                ChatFormatting formatting = ChatFormatting.getByCode(Character.toLowerCase(code));
                if (formatting != null) {
                    flush(parts, pending, style);
                    style = style.applyLegacyFormat(formatting);
                    position += 2;
                    continue;
                }
            }
            if (character == '[') {
                int labelEnd = text.indexOf("](", position + 1);
                int urlEnd = labelEnd < 0 ? -1 : text.indexOf(')', labelEnd + 2);
                if (urlEnd >= 0) {
                    flush(parts, pending, style);
                    String label = text.substring(position + 1, labelEnd);
                    if (label.isEmpty()) {
                        throw new IllegalArgumentException("Link labels must not be empty.");
                    }
                    URI uri = validateUrl(text.substring(labelEnd + 2, urlEnd));
                    Style linkStyle = style.withUnderlined(true)
                            .withClickEvent(new ClickEvent.OpenUrl(uri))
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal(uri.toString())));
                    parts.add(new Part(label, linkStyle));
                    position = urlEnd + 1;
                    continue;
                }
            }
            pending.append(character);
            position++;
        }
        flush(parts, pending, style);
        return new MessageTemplate(parts);
    }

    MutableComponent render(String playerName) {
        MutableComponent message = Component.empty();
        for (Part part : parts) {
            message.append(Component.literal(part.text().replace("{player}", playerName)).setStyle(part.style()));
        }
        return message;
    }

    private static URI validateUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid link URL: " + url, exception);
        }
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Links must be absolute http:// or https:// URLs without credentials: " + url);
        }
        return uri;
    }

    private static void flush(List<Part> parts, StringBuilder pending, Style style) {
        if (!pending.isEmpty()) {
            parts.add(new Part(pending.toString(), style));
            pending.setLength(0);
        }
    }

    private record Part(String text, Style style) {}
}
