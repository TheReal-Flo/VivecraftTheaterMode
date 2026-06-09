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
 * Feeds raw mouse look deltas into the Theater camera's mod-owned yaw accumulator, so horizontal
 * look survives Vivecraft pulling the entity yaw back toward the head/VR facing between frames.
 */
@Mixin(value = Entity.class, priority = 2000)
abstract class EntityLookTheaterMixin {
    @Unique
    private boolean vtm$controlsTheaterLook() {
        return TheaterMode.isActive() && (Object) this == MinecraftClient.getInstance().player;
    }

    @Inject(method = "changeLookDirection", at = @At("TAIL"))
    private void vtm$accumulateTheaterYaw(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (!vtm$controlsTheaterLook()) {
            return;
        }

        TheaterRenderer.accumulateViewYaw(cursorDeltaX);
    }
}
