package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.gameplay.screenhandlers.GuiHandler;

@Mixin(GuiHandler.class)
abstract class GuiHandlerTheaterMixin {
    @Inject(method = "processGui", at = @At("HEAD"), cancellable = true)
    private static void vtm$disableGuiTrackingInTheater(CallbackInfo ci) {
        if (TheaterMode.isActive() && MinecraftClient.getInstance().currentScreen == null) {
            ci.cancel();
        }
    }

    @Inject(method = "processBindingsGui", at = @At("HEAD"), cancellable = true)
    private static void vtm$disableGuiBindingsInTheater(CallbackInfo ci) {
        if (TheaterMode.isActive() && MinecraftClient.getInstance().currentScreen == null) {
            ci.cancel();
        }
    }
}
