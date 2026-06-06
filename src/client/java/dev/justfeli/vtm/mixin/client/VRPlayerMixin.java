package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.gameplay.VRPlayer;

@Mixin(VRPlayer.class)
abstract class VRPlayerMixin {
    @Shadow
    public Vec3d roomOrigin;

    @Shadow
    public VRData vrdata_world_pre;

    @Shadow
    public VRData vrdata_world_post;

    @Shadow
    public abstract void snapRoomOriginToPlayerEntity(net.minecraft.entity.Entity entity, boolean reset, boolean instant);

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

    @Inject(method = "preTick", at = @At("TAIL"))
    private void vtm$snapRoomOriginToVanillaPlayer(CallbackInfo ci) {
        ClientPlayerEntity player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (!TheaterMode.isActive() || player == null || this.vrdata_world_pre == null) {
            return;
        }

        this.snapRoomOriginToPlayerEntity(player, false, true);
        this.vrdata_world_pre.origin = this.roomOrigin;
    }

    @Inject(method = "postTick", at = @At("TAIL"))
    private void vtm$keepPostTickRoomOriginAligned(CallbackInfo ci) {
        if (TheaterMode.isActive() && this.vrdata_world_post != null) {
            this.vrdata_world_post.origin = this.roomOrigin;
        }
    }
}
