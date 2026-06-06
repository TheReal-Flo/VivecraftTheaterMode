package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
}
