package dev.justfeli.vtm.client.playmode;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resource.language.I18n;
import org.vivecraft.client_vr.settings.VRSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public final class TheaterModeState {
    private static final String MODE_KEY = "playMode";
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("vivecraft_theater_mode.properties");

    private static TheaterPlayMode currentMode = TheaterPlayMode.STANDING;
    private static TheaterPlayMode pendingMode;
    private static boolean loaded;

    private TheaterModeState() {
    }

    public static TheaterPlayMode getMode() {
        return currentMode != null ? currentMode : TheaterPlayMode.STANDING;
    }

    public static void setMode(TheaterPlayMode mode) {
        currentMode = Objects.requireNonNull(mode, "mode");
    }

    public static void queuePendingMode(TheaterPlayMode mode) {
        pendingMode = Objects.requireNonNull(mode, "mode");
    }

    public static TheaterPlayMode consumePendingMode() {
        TheaterPlayMode nextMode = pendingMode != null ? pendingMode : TheaterPlayMode.SEATED;
        pendingMode = null;
        return nextMode;
    }

    public static void clearPendingMode() {
        pendingMode = null;
    }

    public static void loadOrInfer(VRSettings vrSettings) {
        if (!loaded) {
            currentMode = null;
            readFromDisk().ifPresent(theaterPlayMode -> currentMode = theaterPlayMode);
            if (currentMode == null) {
                currentMode = inferMode(vrSettings);
            }
            loaded = true;
        }

        syncIntoVivecraft(vrSettings);
    }

    public static void syncIntoVivecraft(VRSettings vrSettings) {
        vrSettings.seated = currentMode.isSeatedLike();
    }

    public static void ensureLoaded(VRSettings vrSettings) {
        if (!loaded) {
            loadOrInfer(vrSettings);
        } else {
            syncIntoVivecraft(vrSettings);
        }
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty(MODE_KEY, getMode().name());

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream outputStream = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(outputStream, "Vivecraft Theater Mode");
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to save Vivecraft Theater Mode config", exception);
        }
    }

    public static String getButtonDisplayString() {
        return I18n.translate("vivecraft.options.PLAY_MODE_SEATED") + ": " + getModeDisplayString();
    }

    public static String getModeDisplayString() {
        return I18n.translate(getMode().translationKey());
    }

    private static TheaterPlayMode inferMode(VRSettings vrSettings) {
        return vrSettings.seated ? TheaterPlayMode.SEATED : TheaterPlayMode.STANDING;
    }

    private static java.util.Optional<TheaterPlayMode> readFromDisk() {
        if (!Files.exists(CONFIG_PATH)) {
            return java.util.Optional.empty();
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(CONFIG_PATH)) {
            properties.load(inputStream);
            TheaterPlayMode mode = TheaterPlayMode.fromString(properties.getProperty(MODE_KEY));
            return java.util.Optional.of(mode);
        } catch (IOException exception) {
            return java.util.Optional.empty();
        }
    }
}
