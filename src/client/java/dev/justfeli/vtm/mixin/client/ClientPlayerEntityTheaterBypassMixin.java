package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
abstract class ClientPlayerEntityTheaterBypassMixin {
    private boolean vtm$shouldBypassPlayer() {
        return TheaterMode.isActive() && MinecraftClient.getInstance().currentScreen == null;
    }

    @Inject(method = {"tick", "aiStep"}, at = @At("HEAD"))
    private void vtm$beginPlayerTickBypass(CallbackInfo ci) {
        if (vtm$shouldBypassPlayer()) {
            TheaterMode.beginVanillaBypass();
        }
    }

    @Inject(method = {"tick", "aiStep"}, at = @At("TAIL"))
    private void vtm$endPlayerTickBypass(CallbackInfo ci) {
        if (vtm$shouldBypassPlayer()) {
            TheaterMode.endVanillaBypass();
        }
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void vtm$beginPlayerMoveBypass(CallbackInfo ci) {
        if (vtm$shouldBypassPlayer()) {
            TheaterMode.beginVanillaBypass();
        }
    }

    @Inject(method = "move", at = @At("TAIL"))
    private void vtm$endPlayerMoveBypass(CallbackInfo ci) {
        if (vtm$shouldBypassPlayer()) {
            TheaterMode.endVanillaBypass();
        }
    }
}
