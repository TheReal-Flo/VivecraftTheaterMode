package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Mouse;

@Mixin(value = Mouse.class, priority = 3000)
abstract class MouseTheaterBypassMixin {
    @Unique
    private boolean vtm$mouseBypassActive;

    private static boolean vtm$shouldBypassMouse() {
        return TheaterMode.isActive() && MinecraftClient.getInstance().currentScreen == null;
    }

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void vtm$beginMoveBypass(double timeDelta, CallbackInfo ci) {
        if (!vtm$shouldBypassMouse()) {
            return;
        }

        vtm$mouseBypassActive = true;
        TheaterMode.beginVanillaBypass();
    }

    @Inject(method = "updateMouse", at = @At("RETURN"))
    private void vtm$endMoveBypass(double timeDelta, CallbackInfo ci) {
        if (!vtm$mouseBypassActive) {
            return;
        }

        vtm$mouseBypassActive = false;
        TheaterMode.endVanillaBypass();
    }
}
