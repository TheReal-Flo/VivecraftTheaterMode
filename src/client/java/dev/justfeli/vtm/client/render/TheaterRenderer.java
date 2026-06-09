package dev.justfeli.vtm.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.vivecraft.api.client.data.RenderPass;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.extensions.GameRendererExtension;
import org.vivecraft.client_vr.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.client_vr.render.helpers.RenderHelper;
import org.vivecraft.client_vr.render.helpers.VREffectsHelper;
import org.vivecraft.client_xr.render_pass.RenderPassManager;
import org.vivecraft.client_xr.render_pass.WorldRenderPass;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import dev.justfeli.vtm.mixin.client.EntityAccessor;
import dev.justfeli.vtm.mixin.client.MinecraftClientAccessor;

public final class TheaterRenderer {
    private static final int THEATER_WIDTH = 1920;
    private static final int THEATER_HEIGHT = 1080;
    private static final float THEATER_DISTANCE = 2.5F;
    private static final float THEATER_SCALE = 2.0F;

    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final ClientDataHolderVR DATA_HOLDER = ClientDataHolderVR.getInstance();

    private static Framebuffer theaterFramebuffer;
    private static boolean renderingTheaterFrame;
    private static boolean renderingTheaterPanel;
    private static Vector3f panelAnchorPosition;
    private static Matrix4f panelAnchorRotation;

    private static final double LOOK_SENSITIVITY = 0.15D;
    private static float theaterViewYaw;
    private static boolean theaterViewYawValid;
    private static float previousPlayerYaw;
    private static float previousPlayerPitch;
    private static float previousPlayerLastYaw;
    private static float previousPlayerLastPitch;
    private static float previousPlayerHeadYaw;
    private static float previousPlayerBodyYaw;
    private static float previousPlayerEyeHeight;
    private static boolean playerRotationOverridden;

    private TheaterRenderer() {
    }

    public static boolean isRenderingTheaterFrame() {
        return renderingTheaterFrame;
    }

    public static boolean isRenderingTheaterPanel() {
        return renderingTheaterPanel;
    }

    /**
     * Accumulates raw mouse look deltas into a mod-owned yaw used purely for the theater camera.
     *
     * <p>The theater camera can't reliably read the player entity's yaw: even with the seated keyhole
     * and the render-view-entity cache neutralised, the entity yaw still gets pulled back toward the
     * VR/head facing between frames, which makes the rendered view snap back horizontally. Pitch is
     * not affected, so we only take over yaw. By integrating the same delta vanilla mouse look uses
     * (cursorDeltaX * 0.15), this owns the horizontal view independently of anything Vivecraft does to
     * the entity, while leaving the entity itself (movement, aim, networking) untouched.
     */
    public static void accumulateViewYaw(double cursorDeltaX) {
        if (theaterViewYawValid) {
            theaterViewYaw += (float) (cursorDeltaX * LOOK_SENSITIVITY);
        }
    }

    /**
     * Makes the accumulated view yaw authoritative on the actual player, so the body turns and
     * movement is computed relative to where the Theater camera is looking. Called at the start of
     * the movement tick (before {@code travel}/{@code freemoveDirection} read the yaw). Vanilla mouse
     * look adds the same delta to both the entity yaw and the accumulator, so they stay in lockstep
     * and there is no double turning.
     */
    public static void applyViewYawToPlayer() {
        if (MC.player == null) {
            return;
        }
        if (!theaterViewYawValid) {
            theaterViewYaw = MC.player.getYaw();
            theaterViewYawValid = true;
        }
        MC.player.setYaw(theaterViewYaw);
        MC.player.lastYaw = theaterViewYaw;
        if (MC.player instanceof LivingEntity livingEntity) {
            livingEntity.headYaw = theaterViewYaw;
            livingEntity.bodyYaw = theaterViewYaw;
        }
    }

    public static Framebuffer getTheaterFramebuffer() {
        return theaterFramebuffer;
    }

    public static boolean hasTheaterFramebuffer() {
        return theaterFramebuffer != null;
    }

    public static boolean shouldRenderGameplayFrame() {
        return TheaterMode.isActive() &&
            DATA_HOLDER.vrPlayer != null &&
            DATA_HOLDER.vr != null &&
            MC.world != null &&
            MC.player != null &&
            MC.interactionManager != null &&
            MC.gameRenderer != null &&
            MC.currentScreen == null &&
            !MC.isPaused();
    }

    public static void renderVanillaFrameToGui() {
        if (renderingTheaterFrame || !shouldRenderGameplayFrame()) {
            return;
        }
        ensureTheaterFramebuffer();
        if (theaterFramebuffer == null) {
            return;
        }

        Framebuffer previousFramebuffer = MC.getFramebuffer();
        MinecraftClientAccessor clientAccessor = (MinecraftClientAccessor) MC;
        Entity previousCameraEntity = clientAccessor.vtm$getCameraEntity();
        WorldRenderPass previousWorldPass = RenderPassManager.WRP;
        var previousPass = DATA_HOLDER.currentPass;
        renderingTheaterFrame = true;
        TheaterMode.beginVanillaBypass();
        try {
            RenderPassManager.setVanillaRenderPass();
            clientAccessor.vtm$setFramebuffer(theaterFramebuffer);
            clientAccessor.vtm$setCameraEntity(MC.player);
            theaterFramebuffer.resize(THEATER_WIDTH, THEATER_HEIGHT);
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                theaterFramebuffer.getColorAttachment(), 0xFF000000,
                theaterFramebuffer.getDepthAttachment(), 1.0
            );

            vtm$applyCleanPlayerRotation();
            RenderTickCounter renderTickCounter = MC.getRenderTickCounter();
            MC.gameRenderer.getCamera().update(
                MC.world,
                MC.player,
                false,
                false,
                renderTickCounter.getTickProgress(true)
            );
            ((GameRendererExtension) MC.gameRenderer).vivecraft$resetProjectionMatrix(
                renderTickCounter.getTickProgress(false)
            );
            MC.gameRenderer.renderWorld(renderTickCounter);
        } finally {
            vtm$restorePlayerRotation();
            clientAccessor.vtm$setFramebuffer(previousFramebuffer);
            clientAccessor.vtm$setCameraEntity(previousCameraEntity);
            restorePreviousRenderPass(previousWorldPass, previousPass);
            TheaterMode.endVanillaBypass();
            renderingTheaterFrame = false;
        }
    }

    public static void renderEnvironment(float partialTick) {
        if (DATA_HOLDER.vrPlayer == null || DATA_HOLDER.vr == null) {
            return;
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            MC.getFramebuffer().getColorAttachment(), 0xFF000000,
            MC.getFramebuffer().getDepthAttachment(), 1.0
        );
        RenderSystem.getModelViewStack().pushMatrix().identity();
        try {
            RenderHelper.applyVRModelView(DATA_HOLDER.currentPass, RenderSystem.getModelViewStack());
            ((GameRendererExtension) MC.gameRenderer).vivecraft$resetProjectionMatrix(partialTick);
            VREffectsHelper.renderMenuEnvironment();
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
        }
    }

    public static void placeGuiSurface() {
        if (!TheaterMode.isActive() || DATA_HOLDER.vrPlayer == null || DATA_HOLDER.vr == null) {
            resetPanelAnchor();
            return;
        }

        ensurePanelAnchor();
        GuiHandler.GUI_POS_ROOM = new Vector3f(panelAnchorPosition);
        GuiHandler.GUI_ROTATION_ROOM = new Matrix4f(panelAnchorRotation);
        GuiHandler.GUI_SCALE = THEATER_SCALE;
    }

    public static void renderTheaterLayer(float partialTick, boolean depthAlways) {
        if (!TheaterMode.isActive() || MC.currentScreen != null || theaterFramebuffer == null) {
            return;
        }

        ensurePanelAnchor();
        // Mark this render2D as the theater screen so its panel quad is drawn opaque (see
        // VREffectsHelperMixin): the world framebuffer has correct sky colour but alpha 0 in sky
        // areas, and the default translucent panel lets the menu environment bleed through there.
        renderingTheaterPanel = true;
        try {
            VREffectsHelper.render2D(partialTick, theaterFramebuffer, new Vector3f(panelAnchorPosition),
                new Matrix4f(panelAnchorRotation), depthAlways);
        } finally {
            renderingTheaterPanel = false;
        }
    }

    private static void vtm$applyCleanPlayerRotation() {
        if (MC.player == null || playerRotationOverridden) {
            return;
        }

        previousPlayerYaw = MC.player.getYaw();
        previousPlayerPitch = MC.player.getPitch();
        previousPlayerLastYaw = MC.player.lastYaw;
        previousPlayerLastPitch = MC.player.lastPitch;
        EntityAccessor playerAccessor = (EntityAccessor) MC.player;
        previousPlayerEyeHeight = playerAccessor.vtm$getStandingEyeHeightField();

        if (!theaterViewYawValid) {
            theaterViewYaw = MC.player.getYaw();
            theaterViewYawValid = true;
        }

        // Yaw is driven by the mod-owned accumulator (mouse deltas), pitch stays as the live entity
        // pitch, which already tracks the mouse correctly.
        float renderYaw = theaterViewYaw;
        float renderPitch = MC.player.getPitch();
        float renderLastYaw = theaterViewYaw;
        float renderLastPitch = MC.player.lastPitch;
        float renderHeadYaw = theaterViewYaw;
        float renderBodyYaw = theaterViewYaw;

        MC.player.setYaw(renderYaw);
        MC.player.setPitch(renderPitch);
        MC.player.lastYaw = renderLastYaw;
        MC.player.lastPitch = renderLastPitch;
        playerAccessor.vtm$setStandingEyeHeightField(MC.player.getEyeHeight(MC.player.getPose()));

        if (MC.player instanceof LivingEntity livingEntity) {
            previousPlayerHeadYaw = livingEntity.headYaw;
            previousPlayerBodyYaw = livingEntity.bodyYaw;
            livingEntity.headYaw = renderHeadYaw;
            livingEntity.bodyYaw = renderBodyYaw;
        }

        playerRotationOverridden = true;
    }

    private static void vtm$restorePlayerRotation() {
        if (!playerRotationOverridden || MC.player == null) {
            return;
        }

        MC.player.setYaw(previousPlayerYaw);
        MC.player.setPitch(previousPlayerPitch);
        MC.player.lastYaw = previousPlayerLastYaw;
        MC.player.lastPitch = previousPlayerLastPitch;
        ((EntityAccessor) MC.player).vtm$setStandingEyeHeightField(previousPlayerEyeHeight);

        if (MC.player instanceof LivingEntity livingEntity) {
            livingEntity.headYaw = previousPlayerHeadYaw;
            livingEntity.bodyYaw = previousPlayerBodyYaw;
        }

        playerRotationOverridden = false;
    }

    private static void ensureTheaterFramebuffer() {
        if (theaterFramebuffer == null) {
            theaterFramebuffer = new SimpleFramebuffer("Theater", THEATER_WIDTH, THEATER_HEIGHT, true);
        }
    }

    private static void ensurePanelAnchor() {
        if (panelAnchorPosition != null && panelAnchorRotation != null) {
            return;
        }

        Vector3f liveHeadPosition = DATA_HOLDER.vrPlayer.vrdata_room_pre.hmd.getPositionF();
        Vector3f headPosition = new Vector3f(0.0F, liveHeadPosition.y, 0.0F);
        float yaw = getAverageYawRadians();
        Vector3f direction = new Vector3f(-MathHelper.sin(yaw), 0.0F, MathHelper.cos(yaw));
        panelAnchorPosition = new Vector3f(headPosition).add(direction.mul(THEATER_DISTANCE));
        panelAnchorRotation = getPanelRoomRotation(headPosition, panelAnchorPosition);
    }

    private static void resetPanelAnchor() {
        panelAnchorPosition = null;
        panelAnchorRotation = null;
        // Re-seed the view yaw from the real player yaw the next time Theater mode starts.
        theaterViewYawValid = false;
    }

    private static void restorePreviousRenderPass(WorldRenderPass previousWorldPass, RenderPass previousPass) {
        if (previousWorldPass != null) {
            RenderPassManager.setWorldRenderPass(previousWorldPass);
            DATA_HOLDER.currentPass = getCompatibleRenderPass(previousWorldPass, previousPass);
            return;
        }

        if (previousPass == RenderPass.GUI) {
            RenderPassManager.setGUIRenderPass();
            return;
        }

        if (previousPass == RenderPass.MIRROR) {
            RenderPassManager.setMirrorRenderPass();
            return;
        }

        RenderPassManager.setVanillaRenderPass();
    }

    private static RenderPass getCompatibleRenderPass(WorldRenderPass previousWorldPass, RenderPass previousPass) {
        if (previousPass != null && WorldRenderPass.getByRenderPass(previousPass) == previousWorldPass) {
            return previousPass;
        }

        if (previousWorldPass == WorldRenderPass.STEREO_XR) {
            return RenderPass.LEFT;
        }
        if (previousWorldPass == WorldRenderPass.CENTER) {
            return RenderPass.CENTER;
        }
        if (previousWorldPass == WorldRenderPass.MIXED_REALITY) {
            return RenderPass.THIRD;
        }
        if (previousWorldPass == WorldRenderPass.LEFT_TELESCOPE) {
            return RenderPass.SCOPEL;
        }
        if (previousWorldPass == WorldRenderPass.RIGHT_TELESCOPE) {
            return RenderPass.SCOPER;
        }
        if (previousWorldPass == WorldRenderPass.CAMERA) {
            return RenderPass.CAMERA;
        }

        return RenderPass.VANILLA;
    }

    private static Matrix4f getPanelRoomRotation(Vector3f headPosition, Vector3f roomPosition) {
        Vector3f look = roomPosition.sub(headPosition, new Vector3f());
        float pitch = (float) Math.asin(look.y / look.length());
        float yaw = MathHelper.PI + (float) Math.atan2(look.x, look.z);
        return new Matrix4f().rotationY(yaw).rotateX(pitch);
    }

    private static float getAverageYawRadians() {
        if (DATA_HOLDER.vr.hmdYawSamples.isEmpty()) {
            return MathHelper.RADIANS_PER_DEGREE * DATA_HOLDER.vrPlayer.vrdata_room_pre.hmd.getYaw();
        }

        float yawDegrees = 0.0F;
        for (float sample : DATA_HOLDER.vr.hmdYawSamples) {
            yawDegrees += sample;
        }

        return MathHelper.RADIANS_PER_DEGREE * (yawDegrees / DATA_HOLDER.vr.hmdYawSamples.size());
    }
}
