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
 * Keeps a contamination-immune mouse-look accumulator for the theater camera.
 *
 * <p>{@code changeLookDirection} applies the mouse delta relative to the entity's current yaw/pitch.
 * In theater mode Vivecraft's render-view-entity logic transiently overwrites the entity rotation
 * with the HMD pose (and skips its restore on vanilla passes), so applying the delta on top of that
 * value would inherit the HMD drift. We reset the entity to our stored clean rotation before the
 * relative delta is applied, then capture the result, so the clean value only ever changes by mouse
 * deltas.
 */
@Mixin(value = Entity.class, priority = 2000)
abstract class EntityLookTheaterMixin {
    @Unique
    private boolean vtm$controlsTheaterLook() {
        return TheaterMode.isActive() && (Object) this == MinecraftClient.getInstance().player;
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"))
    private void vtm$resetToCleanBeforeLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (!vtm$controlsTheaterLook()) {
            return;
        }

        Entity entity = (Entity) (Object) this;
        if (TheaterRenderer.hasCleanRotation()) {
            entity.setYaw(TheaterRenderer.getCleanYaw());
            entity.setPitch(TheaterRenderer.getCleanPitch());
        } else {
            // First use: seed the accumulator from the current rotation.
            TheaterRenderer.captureCleanRotation(entity.getYaw(), entity.getPitch());
        }
    }

    @Inject(method = "changeLookDirection", at = @At("TAIL"))
    private void vtm$captureCleanAfterLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (!vtm$controlsTheaterLook()) {
            return;
        }

        Entity entity = (Entity) (Object) this;
        TheaterRenderer.captureCleanRotation(entity.getYaw(), entity.getPitch());
    }
}
