package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import dev.justfeli.vtm.client.render.TheaterRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces Theater-mode mouse look with exact vanilla turn math backed by a clean accumulator.
 *
 * <p>Using the original method was still letting theater look inherit enough Vivecraft state to
 * feel clamped and offset. For Theater mode we apply the exact vanilla math ourselves:
 * {@code pitch += deltaY * 0.15}, {@code yaw += deltaX * 0.15}, and clamp pitch to [-90, 90].
 * That makes the theater camera source fully independent from the transient HMD render-view state.
 */
@Mixin(value = Entity.class, priority = 2000)
abstract class EntityLookTheaterMixin {
    @Unique
    private boolean vtm$controlsTheaterLook() {
        return TheaterMode.isActive() && (Object) this == MinecraftClient.getInstance().player;
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void vtm$applyVanillaLookDirectly(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (!vtm$controlsTheaterLook()) {
            return;
        }

        Entity entity = (Entity) (Object) this;
        float yaw = TheaterRenderer.hasCleanRotation() ? TheaterRenderer.getCleanYaw() : entity.getYaw();
        float pitch = TheaterRenderer.hasCleanRotation() ? TheaterRenderer.getCleanPitch() : entity.getPitch();

        float pitchDelta = (float) cursorDeltaY * 0.15F;
        float yawDelta = (float) cursorDeltaX * 0.15F;
        pitch = MathHelper.clamp(pitch + pitchDelta, -90.0F, 90.0F);
        yaw += yawDelta;

        entity.setPitch(pitch);
        entity.setYaw(yaw);
        entity.lastPitch = MathHelper.clamp(entity.lastPitch + pitchDelta, -90.0F, 90.0F);
        entity.lastYaw += yawDelta;
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.headYaw = yaw;
            livingEntity.bodyYaw = yaw;
        }
        TheaterRenderer.captureCleanRotation(yaw, pitch);
        ci.cancel();
    }
}
