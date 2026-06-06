package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Mouse;

@Mixin(Mouse.class)
abstract class MouseTheaterBypassMixin {
    private static boolean vtm$shouldBypassMouse() {
        return TheaterMode.isActive() && net.minecraft.client.MinecraftClient.getInstance().currentScreen == null;
    }

    @Inject(method = {"lockCursor", "unlockCursor"}, at = @At("HEAD"))
    private void vtm$beginCursorBypass(CallbackInfo ci) {
        if (vtm$shouldBypassMouse()) {
            TheaterMode.beginVanillaBypass();
        }
    }

    @Inject(method = {"lockCursor", "unlockCursor"}, at = @At("TAIL"))
    private void vtm$endCursorBypass(CallbackInfo ci) {
        if (vtm$shouldBypassMouse()) {
            TheaterMode.endVanillaBypass();
        }
    }

    @Inject(method = {"updateMouse", "onCursorPos"}, at = @At("HEAD"))
    private void vtm$beginMoveBypass(CallbackInfo ci) {
        if (vtm$shouldBypassMouse()) {
            TheaterMode.beginVanillaBypass();
        }
    }

    @Inject(method = {"updateMouse", "onCursorPos"}, at = @At("TAIL"))
    private void vtm$endMoveBypass(CallbackInfo ci) {
        if (vtm$shouldBypassMouse()) {
            TheaterMode.endVanillaBypass();
        }
    }

}
