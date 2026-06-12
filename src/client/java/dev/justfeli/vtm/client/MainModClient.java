package dev.justfeli.vtm.client;

import dev.justfeli.vtm.client.environment.TheaterEnvironmentManager;
import dev.justfeli.vtm.client.network.TheaterServerVrState;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

public class MainModClient implements ClientModInitializer {
    private static final Identifier THEATER_ENVIRONMENT_RELOAD_ID =
        Identifier.of("vivecraft_theater_mode", "theater_environment_reload");

    @Override
    public void onInitializeClient() {
        // Keep the server's view of our VR state in sync with Theater mode, so the server handles a
        // Theater player like a non-VR player (notably for item-drop direction). See TheaterServerVrState.
        ClientTickEvents.END_CLIENT_TICK.register(client -> TheaterServerVrState.tick());

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return THEATER_ENVIRONMENT_RELOAD_ID;
            }

            @Override
            public void reload(ResourceManager resourceManager) {
                TheaterEnvironmentManager.reload(resourceManager);
            }
        });
    }
}
