package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientGameplayBypassMixin {
    private static boolean vtm$shouldBypassGameplay() {
        return TheaterMode.isActive() && MinecraftClient.getInstance().currentScreen == null;
    }

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void vtm$beginHandleInputBypass(CallbackInfo ci) {
        if (vtm$shouldBypassGameplay()) {
            TheaterMode.beginVanillaBypass();
        }
    }

    @Inject(method = "handleInputEvents", at = @At("TAIL"))
    private void vtm$endHandleInputBypass(CallbackInfo ci) {
        if (vtm$shouldBypassGameplay()) {
            TheaterMode.endVanillaBypass();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void vtm$beginTickBypass(CallbackInfo ci) {
        if (vtm$shouldBypassGameplay()) {
            TheaterMode.beginVanillaBypass();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void vtm$endTickBypass(CallbackInfo ci) {
        if (vtm$shouldBypassGameplay()) {
            TheaterMode.endVanillaBypass();
        }
    }
}
