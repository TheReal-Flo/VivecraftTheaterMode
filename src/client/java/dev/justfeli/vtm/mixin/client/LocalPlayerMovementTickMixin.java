package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 2000)
abstract class LocalPlayerMovementTickMixin {
    @Unique
    private boolean vtm$movementTickBypassActive;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void vtm$beginVanillaMovementTick(CallbackInfo ci) {
        if (!TheaterMode.isActive() || (Object) this != MinecraftClient.getInstance().player) {
            return;
        }

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
    }
}
