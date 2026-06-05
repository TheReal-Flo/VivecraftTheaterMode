package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.render.TheaterRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientFramebufferMixin {
    @Inject(method = "method_1522", at = @At("HEAD"), cancellable = true)
    private void vtm$redirectFramebuffer(CallbackInfoReturnable<Framebuffer> cir) {
        if (TheaterRenderer.isRenderingTheaterFrame() && TheaterRenderer.getTheaterFramebuffer() != null) {
            cir.setReturnValue(TheaterRenderer.getTheaterFramebuffer());
        }
    }
}
