package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import dev.justfeli.vtm.client.render.TheaterRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.render.helpers.ShaderHelper;
import org.vivecraft.client_vr.render.helpers.VREffectsHelper;

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
}
