package dev.justfeli.vtm.client;

import dev.justfeli.vtm.client.network.TheaterServerVrState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class MainModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Keep the server's view of our VR state in sync with Theater mode, so the server handles a
        // Theater player like a non-VR player (notably for item-drop direction). See TheaterServerVrState.
        ClientTickEvents.END_CLIENT_TICK.register(client -> TheaterServerVrState.tick());
    }
}
