package dev.justfeli.vtm.client.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Detects whether Controlify is currently driving input with a controller (as opposed to merely
 * being installed). Used so the VR GUI cursor is only suppressed while the controller is in control.
 *
 * <p>The mod does not depend on Controlify, so this is done via reflection and fails safe: any
 * problem resolving or calling the API just reports "not controlling" (so the normal cursor stays).
 * It uses {@code ControlifyApi.currentInputMode()} (enum {@code InputMode.CONTROLLER}), falling back
 * to {@code getCurrentController()} being present.
 */
public final class ControlifyCompat {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("controlify");

    private static boolean resolved;
    private static Method getApi;
    private static Method currentInputMode;
    private static Method getCurrentController;

    private ControlifyCompat() {
    }

    public static boolean isControllerActive() {
        if (!LOADED) {
            return false;
        }
        try {
            resolve();
            if (getApi == null) {
                return false;
            }
            Object api = getApi.invoke(null);
            if (api == null) {
                return false;
            }
            if (currentInputMode != null) {
                Object mode = currentInputMode.invoke(api);
                if (mode instanceof Enum<?> enumMode) {
                    return enumMode.name().toUpperCase().contains("CONTROLLER");
                }
            }
            if (getCurrentController != null) {
                Object result = getCurrentController.invoke(api);
                if (result instanceof Optional<?> optional) {
                    return optional.isPresent();
                }
            }
        } catch (Throwable ignored) {
            // fail safe - treat as not controlling
        }
        return false;
    }

    private static synchronized void resolve() throws ReflectiveOperationException {
        if (resolved) {
            return;
        }
        resolved = true;
        Class<?> apiClass = Class.forName("dev.isxander.controlify.api.ControlifyApi");
        getApi = apiClass.getMethod("get");
        try {
            currentInputMode = apiClass.getMethod("currentInputMode");
        } catch (NoSuchMethodException ignored) {
            currentInputMode = null;
        }
        try {
            getCurrentController = apiClass.getMethod("getCurrentController");
        } catch (NoSuchMethodException ignored) {
            getCurrentController = null;
        }
    }
}
