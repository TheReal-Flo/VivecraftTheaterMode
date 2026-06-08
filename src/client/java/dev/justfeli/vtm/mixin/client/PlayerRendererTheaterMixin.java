package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client.extensions.EntityRenderStateExtension;

@Mixin(PlayerEntityRenderer.class)
abstract class PlayerRendererTheaterMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void vtm$clearLocalVrRenderState(
        AbstractClientPlayerEntity player,
        PlayerEntityRenderState state,
        float tickProgress,
        CallbackInfo ci
    ) {
        if (TheaterMode.isActive() && player == MinecraftClient.getInstance().player) {
            ((EntityRenderStateExtension) state).vivecraft$setRotInfo(null);
        }
    }
}
