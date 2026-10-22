package net.welcome_message;

import java.io.IOException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WelcomeMessage implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("welcome_message");
    private final WelcomeConfig config = new WelcomeConfig(
            FabricLoader.getInstance().getConfigDir().resolve("welcome-message.json"));

    @Override
    public void onInitialize() {
        try {
            config.createIfMissing();
            config.reload();
        } catch (IOException exception) {
            LOGGER.warn("[Welcome Message] Welcome messages are disabled until the configuration is fixed and reloaded: {}",
                    exception.getMessage());
        }

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendWelcome(handler.player));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                reload(null);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("welcomemessage")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("reload").executes(context -> reload(context.getSource())))
                        .then(Commands.literal("preview").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            sendWelcome(player);
                            return 1;
                        }))));
        String version = FabricLoader.getInstance().getModContainer("welcome_message")
                .orElseThrow().getMetadata().getVersion().getFriendlyString();
        LOGGER.info("[Welcome Message] Mod initialized. Version: {}", version);
    }

    private void sendWelcome(ServerPlayer player) {
        WelcomeConfig.Snapshot snapshot = config.current();
        if (snapshot.enabled()) {
            String playerName = player.getGameProfile().name();
            for (MessageTemplate line : snapshot.lines()) {
                player.sendSystemMessage(line.render(playerName));
            }
        }
    }

    private int reload(CommandSourceStack source) {
        try {
            config.reload();
            LOGGER.debug("[Welcome Message] Configuration reloaded ({} lines).", config.current().lines().size());
            if (source != null) {
                source.sendSuccess(() -> Component.literal("[Welcome Message] Configuration reloaded."), false);
            }
            return 1;
        } catch (IOException exception) {
            String message = "[Welcome Message] Reload failed; the previous configuration remains active. "
                    + exception.getMessage();
            LOGGER.warn("{}", message);
            if (source != null) {
                source.sendFailure(Component.literal(message));
            }
            return 0;
        }
    }
}
