package net.welcome_message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public final class WelcomeMessageVerification {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void verifyText() {
        Component message = MessageTemplate.parse("&6Welcome, {player}! &b[Website](https://example.com/?a=1&b=2) after")
                .render("SwordfishBE");
        require(message.getString().equals("Welcome, SwordfishBE! Website after"), "Visible text and player placeholder");
        Component link = message.getSiblings().get(1);
        require(link.getStyle().getClickEvent() instanceof ClickEvent.OpenUrl, "URL click action");
        ClickEvent.OpenUrl click = (ClickEvent.OpenUrl) link.getStyle().getClickEvent();
        require(click.uri().toString().equals("https://example.com/?a=1&b=2"), "URL ampersands stay unchanged");
        require(link.getStyle().isUnderlined() && link.getStyle().getHoverEvent() != null, "Link visibility and hover");
        Component suffix = message.getSiblings().get(2);
        require(suffix.getStyle().getClickEvent() == null && !suffix.getStyle().isUnderlined(), "Link style stays inside link");
        require(link.getStyle().getColor().equals(Style.EMPTY.withColor(ChatFormatting.AQUA).getColor()), "Link color");

        Component styled = MessageTemplate.parse("&lBold &aGreen &rPlain &&a").render("Player");
        require(styled.getSiblings().getFirst().getStyle().isBold(), "Bold formatting");
        require(!styled.getSiblings().get(1).getStyle().isBold(), "Color resets bold");
        require(styled.getString().equals("Bold Green Plain &a"), "Reset and literal ampersand");
        require(MessageTemplate.parse("").render("Player").getString().isEmpty(), "Blank lines");
        require(MessageTemplate.parse("Welcome, {player}!").render("&c[Test]").getString()
                .equals("Welcome, &c[Test]!"), "Player name remains literal");
        Component multiple = MessageTemplate.parse("[One](https://example.com) / [Two](http://example.org)").render("Player");
        require(multiple.getSiblings().stream().filter(part -> part.getStyle().getClickEvent() != null).count() == 2,
                "Multiple independent links");
        Component placeholder = MessageTemplate.parse("[{player}](https://example.com/)").render("Player");
        require(placeholder.getString().equals("Player"), "Player placeholder inside label");
    }

    @Test
    void verifyConfiguration() throws IOException {
        Path directory = Files.createTempDirectory("welcome-message-verification-");
        Path path = directory.resolve("welcome-message.json");
        try {
            WelcomeConfig config = new WelcomeConfig(path);
            config.createIfMissing();
            config.reload();
            require(config.current().enabled() && config.current().lines().size() == 6, "Default welcome has more than two lines");
            String original = Files.readString(path);
            config.createIfMissing();
            require(Files.readString(path).equals(original), "Startup preserves existing file");
            List<String> invalid = List.of(
                    "{broken",
                    "{\"enabled\":true,\"lines\":[\"One\",]}",
                    "{\"enabled\":true,\"lines\":[]} trailing",
                    "{\"enabled\":\"true\",\"lines\":[]}",
                    "{\"enabled\":true,\"lines\":[null]}",
                    "{\"enabled\":true,\"lines\":[42]}",
                    "{\"enabled\":true,\"lines\":[],\"typo\":true}",
                    "{\"enabled\":true,\"lines\":[\"[Bad](file:///etc/passwd)\"]}",
                    "{\"enabled\":true,\"lines\":[\"[Bad](https://user:secret@example.com)\"]}",
                    "{\"enabled\":true,\"lines\":[\"[Bad](relative/path)\"]}",
                    "{\"enabled\":true,\"lines\":[\"[](https://example.com)\"]}");
            WelcomeConfig.Snapshot previous = config.current();
            for (String content : invalid) {
                Files.writeString(path, content);
                expectReloadFailure(config);
                require(config.current() == previous, "Invalid reload preserves active configuration");
                require(Files.readString(path).equals(content), "Invalid file is never reset");
            }
            Files.delete(path);
            expectReloadFailure(config);
            require(!Files.exists(path) && config.current() == previous, "Missing file on reload keeps active configuration");
            Files.writeString(path, "{\"enabled\":true,\"lines\":[\"Changed {player}\",\"Second\",\"Third\"]}");
            config.reload();
            require(config.current().lines().size() == 3
                    && config.current().lines().getFirst().render("Alex").getString().equals("Changed Alex"), "Valid live update");
            Files.writeString(path, "{\"enabled\":false,\"lines\":[]}");
            config.reload();
            require(!config.current().enabled() && config.current().lines().isEmpty(), "Disable welcome");
            Files.writeString(path, "null");
            WelcomeConfig initialInvalid = new WelcomeConfig(path);
            expectReloadFailure(initialInvalid);
            require(!initialInvalid.current().enabled(), "Initial invalid file disables welcome");
        } finally {
            Files.deleteIfExists(path);
            Files.delete(directory);
        }
    }

    private static void expectReloadFailure(WelcomeConfig config) {
        try {
            config.reload();
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError("Expected reload to fail");
    }

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
