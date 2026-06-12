package dev.justfeli.vtm.client.environment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.extensions.GameRendererExtension;
import org.vivecraft.client_vr.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.client_vr.render.helpers.RenderHelper;
import org.vivecraft.client_vr.render.helpers.VREffectsHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class TheaterEnvironmentManager {
    private static final Gson GSON = new GsonBuilder().create();
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final ClientDataHolderVR DATA_HOLDER = ClientDataHolderVR.getInstance();
    private static final Identifier DEFAULT_SCENE_ID = Identifier.of("vivecraft_theater_mode", "theater/environment.json");
    private static final Identifier WHITE_TEXTURE_ID = Identifier.of("minecraft", "textures/misc/white.png");

    private static volatile LoadedScene loadedScene;
    private static volatile boolean sceneLoaded;

    private TheaterEnvironmentManager() {
    }

    public static void reload(ResourceManager resourceManager) {
        synchronized (TheaterEnvironmentManager.class) {
            if (resourceManager == null) {
                loadedScene = null;
                sceneLoaded = false;
                return;
            }

            try {
                loadedScene = loadScene(resourceManager);
                sceneLoaded = true;
            } catch (MissingSceneException exception) {
                loadedScene = null;
                sceneLoaded = true;
            } catch (Exception exception) {
                loadedScene = null;
                sceneLoaded = true;
                System.err.println("Failed to load theater environment: " + exception.getMessage());
                exception.printStackTrace(System.err);
            }
        }
    }

    public static void renderEnvironment(float partialTick) {
        ensureLoaded();
        if (DATA_HOLDER.vrPlayer == null || DATA_HOLDER.vr == null) {
            return;
        }

        loadedSceneOrFallback().render(partialTick);
    }

    public static ScenePlacement getGuiPlacement() {
        ensureLoaded();
        if (loadedScene == null) {
            return null;
        }
        return loadedScene.guiPlacement;
    }

    public static boolean hasLoadedScene() {
        ensureLoaded();
        return loadedScene != null;
    }

    private static LoadedScene loadedSceneOrFallback() {
        if (loadedScene != null) {
            return loadedScene;
        }
        return new LoadedScene(
            DEFAULT_SCENE_ID,
            new Matrix4f(),
            new ScenePlacement(new Vector3f(0.0F, 1.4F, -2.5F), new Matrix4f(), 1.0F),
            List.of()
        );
    }

    private static synchronized void ensureLoaded() {
        if (sceneLoaded) {
            return;
        }

        ResourceManager resourceManager = MC.getResourceManager();
        if (resourceManager != null) {
            reload(resourceManager);
        }
    }

    private static LoadedScene loadScene(ResourceManager resourceManager) throws IOException {
        TheaterEnvironmentConfig config;
        try (InputStream inputStream = resourceManager.open(DEFAULT_SCENE_ID)) {
            config = readConfig(inputStream);
        }

        if (config == null) {
            throw new IOException("Theater environment config is empty: " + DEFAULT_SCENE_ID);
        }

        return buildScene(config, resourceManager, DEFAULT_SCENE_ID);
    }

    private static LoadedScene buildScene(
        TheaterEnvironmentConfig config,
        ResourceManager resourceManager,
        Identifier configId) throws IOException
    {
        TheaterEnvironmentConfig sceneConfig = config == null ? new TheaterEnvironmentConfig() : config;
        Vector3f playerPosition = toVector3f(sceneConfig.playerPosition, 0.0F, 1.6F, 0.0F);
        Vector3f playerRotation = toVector3f(sceneConfig.playerRotation, 0.0F, 0.0F, 0.0F);
        TheaterEnvironmentConfig.ScreenConfig screenConfig = sceneConfig.screen == null
            ? new TheaterEnvironmentConfig.ScreenConfig()
            : sceneConfig.screen;

        // playerPosition marks the viewer's eye point in scene coordinates (y = eye height, 1.6 by
        // default), playerRotation their facing. Rendering happens in room coordinates with the
        // viewer's feet at the origin, so everything in the scene (objects and the GUI placement)
        // is transformed by the inverse of the player pose: sceneToRoom = R(playerRotation)^-1 *
        // T(-feet). With the defaults this is the identity.
        Matrix4f inverseRotation = new Matrix4f()
            .rotateZ(-playerRotation.z * MathHelper.RADIANS_PER_DEGREE)
            .rotateY(-playerRotation.y * MathHelper.RADIANS_PER_DEGREE)
            .rotateX(-playerRotation.x * MathHelper.RADIANS_PER_DEGREE);
        Matrix4f sceneToRoom = new Matrix4f(inverseRotation)
            .translate(-playerPosition.x, -(playerPosition.y - 1.6F), -playerPosition.z);

        Vector3f screenPosition = toVector3f(screenConfig.position, 0.0F, 1.4F, -2.5F);
        ScenePlacement guiPlacement = new ScenePlacement(
            sceneToRoom.transformPosition(screenPosition, new Vector3f()),
            new Matrix4f(inverseRotation)
                .mul(buildRotation(toVector3f(screenConfig.rotation, 0.0F, 0.0F, 0.0F))),
            computeGuiScale(screenConfig.size)
        );

        List<LoadedObject> objects = new ArrayList<>();
        if (sceneConfig.objects != null) {
            for (TheaterEnvironmentConfig.ObjectConfig objectConfig : sceneConfig.objects) {
                if (objectConfig == null || objectConfig.model == null || objectConfig.model.isBlank()) {
                    continue;
                }

                Identifier modelId = resolveResourceIdentifier(configId, objectConfig.model);
                TheaterObjModel model = TheaterObjModel.parse(readString(resourceManager, modelId));

                Identifier textureId = WHITE_TEXTURE_ID;
                if (objectConfig.texture != null && !objectConfig.texture.isBlank()) {
                    Identifier candidateTexture = resolveResourceIdentifier(configId, objectConfig.texture);
                    textureId = resourceExists(resourceManager, candidateTexture) ? candidateTexture : WHITE_TEXTURE_ID;
                }

                objects.add(new LoadedObject(
                    model,
                    textureId,
                    objectConfig.translucent,
                    toVector3f(objectConfig.position, 0.0F, 0.0F, 0.0F),
                    toVector3f(objectConfig.rotation, 0.0F, 0.0F, 0.0F),
                    toVector3f(objectConfig.scale, 1.0F, 1.0F, 1.0F)
                ));
            }
        }

        return new LoadedScene(configId, sceneToRoom, guiPlacement, objects);
    }

    private static TheaterEnvironmentConfig readConfig(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, TheaterEnvironmentConfig.class);
        }
    }

    private static String readString(ResourceManager resourceManager, Identifier identifier) throws IOException {
        try (InputStream inputStream = resourceManager.open(identifier)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static boolean resourceExists(ResourceManager resourceManager, Identifier identifier) {
        try (InputStream ignored = resourceManager.open(identifier)) {
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static Identifier resolveResourceIdentifier(Identifier baseId, String reference) {
        if (reference.indexOf(':') >= 0) {
            Identifier parsed = Identifier.tryParse(reference);
            if (parsed != null) {
                return parsed;
            }
        }

        String namespace = baseId.getNamespace();
        String basePath = baseId.getPath();
        int lastSlash = basePath.lastIndexOf('/');
        String prefix = lastSlash >= 0 ? basePath.substring(0, lastSlash + 1) : "";
        return Identifier.of(namespace, normalizeResourcePath(prefix + reference));
    }

    private static float computeGuiScale(float[] size) {
        float width = size != null && size.length > 0 ? size[0] : 1.8F;
        float height = size != null && size.length > 1 ? size[1] : 1.0F;
        // Vivecraft's GUI quad is 1.5m wide at GUI_SCALE 1.0, with the height following the GUI
        // framebuffer aspect ratio (see GuiHandler.getTexCoordsForCursor).
        float quadWidth = 1.5F;
        float quadHeight = quadWidth * GuiHandler.GUI_HEIGHT / GuiHandler.GUI_WIDTH;
        float widthScale = width / quadWidth;
        float heightScale = height / quadHeight;
        return Math.max(0.0001F, Math.max(widthScale, heightScale));
    }

    private static Vector3f toVector3f(float[] values, float defaultX, float defaultY, float defaultZ) {
        if (values == null || values.length == 0) {
            return new Vector3f(defaultX, defaultY, defaultZ);
        }
        float x = values.length > 0 ? values[0] : defaultX;
        float y = values.length > 1 ? values[1] : defaultY;
        float z = values.length > 2 ? values[2] : defaultZ;
        return new Vector3f(x, y, z);
    }

    private static Matrix4f buildRotation(Vector3f degrees) {
        Matrix4f rotation = new Matrix4f();
        rotation.rotateX(degrees.x * MathHelper.RADIANS_PER_DEGREE);
        rotation.rotateY(degrees.y * MathHelper.RADIANS_PER_DEGREE);
        rotation.rotateZ(degrees.z * MathHelper.RADIANS_PER_DEGREE);
        return rotation;
    }

    private static String normalizeResourcePath(String path) {
        String[] parts = path.replace('\\', '/').split("/");
        List<String> normalized = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!normalized.isEmpty()) {
                    normalized.remove(normalized.size() - 1);
                }
                continue;
            }
            normalized.add(part);
        }
        return String.join("/", normalized);
    }

    private static final class LoadedScene {
        private final Identifier sourceId;
        private final Matrix4f sceneToRoom;
        private final ScenePlacement guiPlacement;
        private final List<LoadedObject> objects;

        private LoadedScene(
            Identifier sourceId,
            Matrix4f sceneToRoom,
            ScenePlacement guiPlacement,
            List<LoadedObject> objects)
        {
            this.sourceId = sourceId;
            this.sceneToRoom = sceneToRoom;
            this.guiPlacement = guiPlacement;
            this.objects = objects;
        }

        private void render(float partialTick) {
            RenderSystemState.render(partialTick, this);
        }
    }

    public record ScenePlacement(Vector3f position, Matrix4f rotation, float scale) {
    }

    private static final class LoadedObject {
        private final TheaterObjModel model;
        private final Identifier textureId;
        private final boolean translucent;
        private final Vector3f position;
        private final Vector3f rotation;
        private final Vector3f scale;

        private LoadedObject(
            TheaterObjModel model,
            Identifier textureId,
            boolean translucent,
            Vector3f position,
            Vector3f rotation,
            Vector3f scale)
        {
            this.model = model;
            this.textureId = textureId;
            this.translucent = translucent;
            this.position = position;
            this.rotation = rotation;
            this.scale = scale;
        }

        private void render(Matrix4fStack poseStack, VertexConsumerProvider.Immediate bufferSource) {
            poseStack.pushMatrix();
            try {
                poseStack.translate(this.position.x, this.position.y, this.position.z);
                poseStack.rotateX(this.rotation.x * MathHelper.RADIANS_PER_DEGREE);
                poseStack.rotateY(this.rotation.y * MathHelper.RADIANS_PER_DEGREE);
                poseStack.rotateZ(this.rotation.z * MathHelper.RADIANS_PER_DEGREE);
                poseStack.scale(this.scale.x, this.scale.y, this.scale.z);
                this.model.render(new Matrix4f(poseStack), bufferSource, this.textureId, this.translucent);
            } finally {
                poseStack.popMatrix();
            }
        }
    }

    private enum LoadState {
        UNLOADED,
        LOADED,
        MISSING,
        FAILED
    }

    private static final class MissingSceneException extends IOException {
        private MissingSceneException(String message) {
            super(message);
        }
    }

    private static final class RenderSystemState {
        private static void render(float partialTick, LoadedScene scene) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                MC.getFramebuffer().getColorAttachment(), 0xFF000000,
                MC.getFramebuffer().getDepthAttachment(), 1.0
            );

            RenderSystem.getModelViewStack().pushMatrix().identity();
            try {
                RenderHelper.applyVRModelView(DATA_HOLDER.currentPass, RenderSystem.getModelViewStack());
                ((GameRendererExtension) MC.gameRenderer).vivecraft$resetProjectionMatrix(partialTick);

                Matrix4fStack poseStack = new Matrix4fStack(16);
                Vec3d eye = DATA_HOLDER.vrPlayer.vrdata_world_render.getEye(DATA_HOLDER.currentPass).getPosition();
                Vec3d origin = DATA_HOLDER.vrPlayer.vrdata_world_render.origin;

                // Same anchoring as Vivecraft's renderMenuEnvironment: room origin relative to the
                // eye, with the world rotation removed so the room aligns with the GUI panel
                // (GUI_POS_ROOM is in the same room-origin coordinate space).
                poseStack.translate(
                    (float) (origin.x - eye.x),
                    (float) (origin.y - eye.y),
                    (float) (origin.z - eye.z)
                );
                poseStack.rotateY(DATA_HOLDER.vrPlayer.vrdata_world_render.rotation_radians);
                poseStack.mul(scene.sceneToRoom);

                if (scene.objects.isEmpty()) {
                    VREffectsHelper.renderMenuEnvironment();
                    return;
                }

                try (BufferAllocator vertexBuffer = new BufferAllocator(786432)) {
                    VertexConsumerProvider.Immediate bufferSource = VertexConsumerProvider.immediate(vertexBuffer);
                    for (LoadedObject object : scene.objects) {
                        object.render(poseStack, bufferSource);
                    }
                    bufferSource.draw();
                }
            } finally {
                RenderSystem.getModelViewStack().popMatrix();
            }
        }
    }
}
