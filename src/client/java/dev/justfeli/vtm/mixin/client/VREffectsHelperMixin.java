package dev.justfeli.vtm.mixin.client;

import com.mojang.blaze3d.textures.GpuTexture;
import dev.justfeli.vtm.client.playmode.TheaterMode;
import dev.justfeli.vtm.client.render.TheaterRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.render.helpers.ShaderHelper;
import org.vivecraft.client_vr.render.helpers.VREffectsHelper;
import org.vivecraft.client_vr.render.rendertypes.VRRenderTypes;

@Mixin(VREffectsHelper.class)
abstract class VREffectsHelperMixin {
    @Inject(method = "renderGuiAndShadow(FZZ)V", at = @At("HEAD"))
    private static void vtm$captureTheaterFrame(
        float partialTick,
        boolean depthAlways,
        boolean shadowFirst,
        CallbackInfo ci
    ) {
        if (TheaterMode.isActive()) {
            TheaterRenderer.placeGuiSurface();
            TheaterRenderer.renderEnvironment(partialTick);
        }
    }

    @Inject(method = "renderGuiLayer(FZ)V", at = @At("HEAD"), cancellable = true)
    private static void vtm$renderTheaterLayer(float partialTick, boolean depthAlways, CallbackInfo ci) {
        if (TheaterMode.isActive() &&
            MinecraftClient.getInstance().currentScreen == null &&
            TheaterRenderer.hasTheaterFramebuffer())
        {
            Framebuffer guiFramebuffer = org.vivecraft.client_vr.gameplay.screenhandlers.GuiHandler.GUI_FRAMEBUFFER;
            if (guiFramebuffer != null) {
                ShaderHelper.blit(guiFramebuffer, TheaterRenderer.getTheaterFramebuffer(), true);
            }
            TheaterRenderer.renderTheaterLayer(partialTick, true);
            ci.cancel();
        }
    }

    /**
     * The theater world framebuffer has the correct sky colour but alpha 0 in sky areas, so the
     * default translucent panel lets the menu environment bleed through (notably the lower sky).
     * When the panel being drawn is the theater screen, use the opaque (solid) render type instead,
     * which ignores the framebuffer alpha and shows the sky colour while occluding the environment.
     */
    @Redirect(
        method = "renderScreen",
        at = @At(
            value = "INVOKE",
            target = "Lorg/vivecraft/client_vr/render/rendertypes/VRRenderTypes;entityTranslucentNoCardinalLight(Lcom/mojang/blaze3d/textures/GpuTexture;Z)Lnet/minecraft/client/render/RenderLayer;"
        )
    )
    private static RenderLayer vtm$opaqueTheaterPanel(GpuTexture texture, boolean depthAlways) {
        if (TheaterRenderer.isRenderingTheaterPanel()) {
            return VRRenderTypes.entitySolidNoCardinalLight(texture);
        }
        return VRRenderTypes.entityTranslucentNoCardinalLight(texture, depthAlways);
    }
}
