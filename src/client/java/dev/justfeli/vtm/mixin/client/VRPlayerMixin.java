package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.gameplay.VRPlayer;

@Mixin(VRPlayer.class)
abstract class VRPlayerMixin {
    @Inject(method = "doPermanentLookOverride", at = @At("HEAD"), cancellable = true)
    private void vtm$disablePermanentLookOverride(ClientPlayerEntity player, VRData data, CallbackInfo ci) {
        if (TheaterMode.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "doPlayerMoveInRoom", at = @At("HEAD"), cancellable = true)
    private void vtm$disableRoomscaleMovement(ClientPlayerEntity player, CallbackInfo ci) {
        if (TheaterMode.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "freemoveDirection", at = @At("HEAD"), cancellable = true)
    private void vtm$useVanillaMovementDirection(
        ClientPlayerEntity player,
        Vec3d relative,
        float speed,
        CallbackInfoReturnable<Vec3d> cir
    ) {
        if (TheaterMode.isActive()) {
            cir.setReturnValue(vtm$movementInputToVelocity(relative, speed, player.getYaw()));
        }
    }

    @Inject(method = "applyDrag", at = @At("HEAD"), cancellable = true)
    private void vtm$disableVrDrag(ClientPlayerEntity player, Vec3d movement, CallbackInfo ci) {
        if (TheaterMode.isActive()) {
            ci.cancel();
        }
    }

    private static Vec3d vtm$movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        double lengthSquared = movementInput.lengthSquared();
        if (lengthSquared < 1.0E-7) {
            return Vec3d.ZERO;
        }

        Vec3d scaledInput = (lengthSquared > 1.0D ? movementInput.normalize() : movementInput).multiply(speed);
        float sinYaw = MathHelper.sin(yaw * MathHelper.RADIANS_PER_DEGREE);
        float cosYaw = MathHelper.cos(yaw * MathHelper.RADIANS_PER_DEGREE);
        return new Vec3d(
            scaledInput.x * cosYaw - scaledInput.z * sinYaw,
            scaledInput.y,
            scaledInput.z * cosYaw + scaledInput.x * sinYaw
        );
    }
}
