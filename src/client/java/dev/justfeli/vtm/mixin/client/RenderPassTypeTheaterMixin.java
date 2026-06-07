package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.render.TheaterRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_xr.render_pass.RenderPassType;

@Mixin(value = RenderPassType.class, remap = false)
abstract class RenderPassTypeTheaterMixin {
    @Inject(method = "isVanilla", at = @At("HEAD"), cancellable = true)
    private static void vtm$forceVanillaForTheaterCopy(CallbackInfoReturnable<Boolean> cir) {
        if (TheaterRenderer.isRenderingTheaterFrame()) {
            cir.setReturnValue(true);
        }
    }
}
