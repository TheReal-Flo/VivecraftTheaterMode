package dev.justfeli.vtm.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.extensions.GameRendererExtension;
import org.vivecraft.client_vr.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.client_vr.render.helpers.RenderHelper;
import org.vivecraft.client_vr.render.helpers.VREffectsHelper;
import org.vivecraft.client_xr.render_pass.RenderPassManager;
import org.vivecraft.client_xr.render_pass.WorldRenderPass;

import dev.justfeli.vtm.client.playmode.TheaterMode;
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

    private TheaterRenderer() {
    }

    public static boolean isRenderingTheaterFrame() {
        return renderingTheaterFrame;
    }

    public static Framebuffer getTheaterFramebuffer() {
        return theaterFramebuffer;
    }

    public static boolean hasTheaterFramebuffer() {
        return theaterFramebuffer != null;
    }

    public static void renderVanillaFrameToGui() {
        if (renderingTheaterFrame || !TheaterMode.isActive()) {
            return;
        }
        if (DATA_HOLDER.vrPlayer == null || DATA_HOLDER.vr == null) {
            return;
        }
        if (MC.world == null || MC.gameRenderer == null) {
            return;
        }
        ensureTheaterFramebuffer();
        if (theaterFramebuffer == null) {
            return;
        }

        Framebuffer previousFramebuffer = MC.getFramebuffer();
        WorldRenderPass previousWorldPass = RenderPassManager.WRP;
        var previousPass = DATA_HOLDER.currentPass;

        renderingTheaterFrame = true;
        TheaterMode.beginVanillaBypass();
        try {
            RenderPassManager.setVanillaRenderPass();
            ((MinecraftClientAccessor) MC).vtm$setFramebuffer(theaterFramebuffer);
            theaterFramebuffer.resize(THEATER_WIDTH, THEATER_HEIGHT);
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                theaterFramebuffer.getColorAttachment(), 0xFF000000,
                theaterFramebuffer.getDepthAttachment(), 1.0
            );

            RenderTickCounter renderTickCounter = MC.getRenderTickCounter();
            MC.gameRenderer.render(renderTickCounter, true);
        } finally {
            ((MinecraftClientAccessor) MC).vtm$setFramebuffer(previousFramebuffer);
            if (previousWorldPass != null) {
                DATA_HOLDER.currentPass = previousPass;
                RenderPassManager.setWorldRenderPass(previousWorldPass);
            } else {
                RenderPassManager.setVanillaRenderPass();
                DATA_HOLDER.currentPass = previousPass;
            }
            TheaterMode.endVanillaBypass();
            renderingTheaterFrame = false;
        }
    }

    public static void renderEnvironment(float partialTick) {
        if (DATA_HOLDER.vrPlayer == null || DATA_HOLDER.vr == null) {
            return;
        }

        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(MC.getFramebuffer().getDepthAttachment(), 1.0);
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
        if (DATA_HOLDER.vrPlayer == null || DATA_HOLDER.vr == null) {
            return;
        }

        GuiHandler.GUI_POS_ROOM = getPanelRoomPosition();
        GuiHandler.GUI_ROTATION_ROOM = getPanelRoomRotation(GuiHandler.GUI_POS_ROOM);
        GuiHandler.GUI_SCALE = THEATER_SCALE;
    }

    public static void renderTheaterLayer(float partialTick, boolean depthAlways) {
        if (!TheaterMode.isActive() || MC.currentScreen != null || theaterFramebuffer == null) {
            return;
        }

        Vector3f panelPosition = getPanelRoomPosition();
        VREffectsHelper.render2D(partialTick, theaterFramebuffer, panelPosition, getPanelRoomRotation(panelPosition), depthAlways);
    }

    private static void ensureTheaterFramebuffer() {
        if (theaterFramebuffer == null) {
            theaterFramebuffer = new SimpleFramebuffer("Theater", THEATER_WIDTH, THEATER_HEIGHT, true);
        }
    }

    private static Vector3f getPanelRoomPosition() {
        Vector3f headPosition = DATA_HOLDER.vrPlayer.vrdata_room_pre.hmd.getPositionF();
        float yaw = getAverageYawRadians();
        Vector3f direction = new Vector3f(-MathHelper.sin(yaw), 0.0F, MathHelper.cos(yaw));
        return new Vector3f(headPosition).add(direction.mul(THEATER_DISTANCE));
    }

    private static Matrix4f getPanelRoomRotation(Vector3f roomPosition) {
        Vector3f headPosition = DATA_HOLDER.vrPlayer.vrdata_room_pre.hmd.getPositionF();
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
