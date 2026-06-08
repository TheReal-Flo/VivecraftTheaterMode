package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client.ClientVRPlayers;

import java.util.UUID;

@Mixin(value = ClientVRPlayers.class, remap = false)
abstract class ClientVRPlayersTheaterMixin {
    @Inject(method = "isVRPlayer(Lnet/minecraft/class_1297;)Z", at = @At("HEAD"), cancellable = true)
    private void vtm$localPlayerIsVanillaInTheater(Entity player, CallbackInfoReturnable<Boolean> cir) {
        if (TheaterMode.isActive() && player == MinecraftClient.getInstance().player) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isVRPlayer(Ljava/util/UUID;)Z", at = @At("HEAD"), cancellable = true)
    private void vtm$localPlayerUuidIsVanillaInTheater(UUID uuid, CallbackInfoReturnable<Boolean> cir) {
        if (TheaterMode.isActive() &&
            MinecraftClient.getInstance().player != null &&
            uuid.equals(MinecraftClient.getInstance().player.getUuid()))
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isVRAndSeated", at = @At("HEAD"), cancellable = true)
    private void vtm$localPlayerIsNotSeatedVrInTheater(UUID uuid, CallbackInfoReturnable<Boolean> cir) {
        if (TheaterMode.isActive() &&
            MinecraftClient.getInstance().player != null &&
            uuid.equals(MinecraftClient.getInstance().player.getUuid()))
        {
            cir.setReturnValue(false);
        }
    }
}
