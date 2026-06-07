package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardInput.class, priority = 2000)
abstract class KeyboardInputTheaterBypassMixin {
    private boolean vtm$shouldBypassKeyboard() {
        return TheaterMode.isActive() && MinecraftClient.getInstance().currentScreen == null;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void vtm$beginKeyboardBypass(CallbackInfo ci) {
        if (vtm$shouldBypassKeyboard()) {
            TheaterMode.beginVanillaBypass();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void vtm$endKeyboardBypass(CallbackInfo ci) {
        if (vtm$shouldBypassKeyboard()) {
            TheaterMode.endVanillaBypass();
        }
    }
}
