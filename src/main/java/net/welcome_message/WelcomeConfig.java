package net.welcome_message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class WelcomeConfig {
    private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private final Path path;
    private Snapshot current = new Snapshot(false, List.of());

    WelcomeConfig(Path path) {
        this.path = path;
    }

    void createIfMissing() throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Files.createDirectories(path.getParent());
        try (InputStream defaults = WelcomeConfig.class.getResourceAsStream("/default-welcome-message.json")) {
            if (defaults == null) {
                throw new IOException("Bundled default configuration is missing.");
            }
            Files.write(path, defaults.readAllBytes(), StandardOpenOption.CREATE_NEW);
        }
    }

    void reload() throws IOException {
        Snapshot candidate;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            candidate = parse(reader);
        } catch (JsonParseException | IllegalArgumentException exception) {
            throw new IOException("Invalid welcome-message.json: " + exception.getMessage(), exception);
        }
        current = candidate;
    }

    Snapshot current() {
        return current;
    }

    private static Snapshot parse(Reader reader) {
        JsonObject root = GSON.fromJson(reader, JsonObject.class);
        if (root == null) {
            throw new IllegalArgumentException("Expected an object with enabled and lines.");
        }
        for (String key : root.keySet()) {
            if (!Set.of("enabled", "lines").contains(key)) {
                throw new IllegalArgumentException("Unknown setting: " + key);
            }
        }
        JsonElement enabled = root.get("enabled");
        if (enabled == null || !enabled.isJsonPrimitive() || !enabled.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("enabled must be true or false.");
        }
        JsonElement lines = root.get("lines");
        if (lines == null || !lines.isJsonArray()) {
            throw new IllegalArgumentException("lines must be an array of text strings.");
        }
        List<MessageTemplate> templates = new ArrayList<>();
        for (JsonElement line : lines.getAsJsonArray()) {
            int lineNumber = templates.size() + 1;
            if (!line.isJsonPrimitive() || !line.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Line " + lineNumber + " must be a text string.");
            }
            try {
                templates.add(MessageTemplate.parse(line.getAsString()));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Line " + lineNumber + ": " + exception.getMessage(), exception);
            }
        }
        return new Snapshot(enabled.getAsBoolean(), List.copyOf(templates));
    }

    record Snapshot(boolean enabled, List<MessageTemplate> lines) {}
}
