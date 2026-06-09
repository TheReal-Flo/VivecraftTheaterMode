package dev.justfeli.vtm.client.network;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import org.vivecraft.client.network.ClientNetworking;
import org.vivecraft.client_vr.VRState;
import org.vivecraft.common.network.packet.c2s.VRActivePayloadC2S;

/**
 * Reports the player to the server as "not in VR" while Theater mode is active.
 *
 * <p>Vivecraft's server-side logic branches on whether the player is a VR player. The most relevant case
 * is the dropped-item direction: {@code ServerPlayerMixin.vivecraft$dropVive} overrides it to the VR
 * controller/HMD aim ({@code getAimDir}) instead of the vanilla player look. In Theater mode the player
 * interacts from the flat view, so the server should treat them as a regular player; then drops (and any
 * other VR-aim server behaviour) fall back to vanilla player-rotation logic, which already tracks the
 * Theater camera (the player's yaw is driven by the Theater view).
 *
 * <p>The server learns our VR state only from the connect handshake and from {@link VRActivePayloadC2S}
 * toggles, so this is purely client-driven (a dedicated server won't have this mod installed). We send a
 * toggle whenever the effective VR state — {@code VR_RUNNING && !TheaterMode} — could have changed: on a
 * change of the play mode, or of Vivecraft's own VR on/off state. Vivecraft only sends its own toggle on
 * VR on/off transitions, so re-asserting whenever {@code VR_RUNNING} changes also covers the case where
 * it would otherwise flip us back to VR while Theater is active. On servers without Vivecraft,
 * {@link ClientNetworking#sendServerPacket} is a no-op (and those have no drop override to begin with).
 */
public final class TheaterServerVrState {
    private static Boolean lastVrRunning;
    private static Boolean lastTheater;

    private TheaterServerVrState() {
    }

    public static void tick() {
        if (!VRState.VR_INITIALIZED || MinecraftClient.getInstance().getNetworkHandler() == null) {
            lastVrRunning = null;
            lastTheater = null;
            return;
        }

        boolean vrRunning = VRState.VR_RUNNING;
        boolean theater = TheaterMode.isActive();
        if (lastVrRunning != null && lastVrRunning == vrRunning && lastTheater == theater) {
            return;
        }

        lastVrRunning = vrRunning;
        lastTheater = theater;
        ClientNetworking.sendServerPacket(new VRActivePayloadC2S(vrRunning && !theater));
    }
}
