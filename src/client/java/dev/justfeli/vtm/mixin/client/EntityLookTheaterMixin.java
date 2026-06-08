package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import dev.justfeli.vtm.client.render.TheaterRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps Theater mode synced to the real player rotation without replacing vanilla look handling.
 */
@Mixin(value = Entity.class, priority = 2000)
abstract class EntityLookTheaterMixin {
    @Unique
    private boolean vtm$controlsTheaterLook() {
        return TheaterMode.isActive() && (Object) this == MinecraftClient.getInstance().player;
    }

    @Inject(method = "changeLookDirection", at = @At("TAIL"))
    private void vtm$capturePlayerLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (!vtm$controlsTheaterLook()) {
            return;
        }

        Entity entity = (Entity) (Object) this;
        TheaterRenderer.captureCleanRotation(entity.getYaw(), entity.getPitch());
    }
}
