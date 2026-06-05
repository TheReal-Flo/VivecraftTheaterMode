package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import dev.justfeli.vtm.client.render.TheaterRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.api.client.data.RenderPass;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.render.helpers.VRPassHelper;

@Mixin(VRPassHelper.class)
abstract class VRPassHelperMixin {
    @Inject(method = "renderSingleView", at = @At("TAIL"))
    private static void vtm$renderTheaterFramebuffer(
        RenderPass eye,
        RenderTickCounter.Dynamic deltaTracker,
        boolean renderLevel,
        CallbackInfo ci
    ) {
        if (!renderLevel || !TheaterMode.isActive() || TheaterRenderer.isRenderingTheaterFrame()) {
            return;
        }
        if (MinecraftClient.getInstance().world == null) {
            return;
        }
        if (!ClientDataHolderVR.getInstance().isFirstPass) {
            return;
        }

        TheaterRenderer.renderVanillaFrameToGui();
    }
}
