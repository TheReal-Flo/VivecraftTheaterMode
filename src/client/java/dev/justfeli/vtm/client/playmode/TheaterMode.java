package dev.justfeli.vtm.client.playmode;

import org.vivecraft.client_vr.VRState;
import org.vivecraft.client_vr.ClientDataHolderVR;

public final class TheaterMode {
    private static int vanillaBypassDepth;
    private static boolean vanillaBypassPreviousVrRunning;

    private TheaterMode() {
    }

    public static boolean isActive() {
        ClientDataHolderVR dataHolder = ClientDataHolderVR.getInstance();
        if (dataHolder.vrSettings == null) {
            return false;
        }

        TheaterModeState.ensureLoaded(dataHolder.vrSettings);
        return TheaterModeState.getMode() == TheaterPlayMode.THEATER;
    }

    public static void beginVanillaBypass() {
        if (!isActive()) {
            return;
        }

        if (vanillaBypassDepth++ == 0) {
            vanillaBypassPreviousVrRunning = VRState.VR_RUNNING;
            VRState.VR_RUNNING = false;
        }
    }

    public static void endVanillaBypass() {
        if (vanillaBypassDepth <= 0) {
            return;
        }

        if (--vanillaBypassDepth == 0) {
            VRState.VR_RUNNING = vanillaBypassPreviousVrRunning;
        }
    }

    public static boolean isVanillaBypassActive() {
        return vanillaBypassDepth > 0;
    }
}
