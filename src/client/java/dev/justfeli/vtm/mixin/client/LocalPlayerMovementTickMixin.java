package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import dev.justfeli.vtm.client.render.TheaterRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.ClientDataHolderVR;

@Mixin(value = LivingEntity.class, priority = 2000)
abstract class LocalPlayerMovementTickMixin {
    @Unique
    private boolean vtm$movementTickBypassActive;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void vtm$beginVanillaMovementTick(CallbackInfo ci) {
        if (!TheaterMode.isActive() || (Object) this != MinecraftClient.getInstance().player) {
            return;
        }

        // Drive the real player yaw from the Theater view accumulator before movement is computed,
        // so the body turns and walking is relative to where the camera looks.
        TheaterRenderer.applyViewYawToPlayer();

        vtm$movementTickBypassActive = true;
        TheaterMode.beginVanillaBypass();
    }

    @Inject(method = "tickMovement", at = @At("RETURN"))
    private void vtm$endVanillaMovementTick(CallbackInfo ci) {
        if (!vtm$movementTickBypassActive) {
            return;
        }

        vtm$movementTickBypassActive = false;
        TheaterMode.endVanillaBypass();

        // Theater bypasses Vivecraft's room-scale move, so the room origin never follows the player.
        // The headset-driven main VR pass (which shares the chunk build/cull state used by the panel
        // render) then stays anchored to the start point, so the world both loads and renders from
        // where you started. Re-centre the room on the player each tick so the headset pass tracks the
        // player; this is invisible because the panel is anchored in room space and stays in front.
        ClientDataHolderVR dataHolder = ClientDataHolderVR.getInstance();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (dataHolder.vrPlayer != null && player != null) {
            dataHolder.vrPlayer.snapRoomOriginToPlayerEntity(player, false, false);
        }
    }
}
