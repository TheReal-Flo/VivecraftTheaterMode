package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayerEntity.class, priority = 2000)
abstract class ClientPlayerEntityMoveMixin {
    @Inject(method = "move", at = @At("HEAD"))
    private void vtm$useVanillaMoveInTheater(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!TheaterMode.isActive() || client.player != (Object) this) {
            return;
        }

        TheaterMode.beginVanillaBypass();
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void vtm$restoreCameraEntityAfterMove(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (TheaterMode.isActive() && client.player == (Object) this) {
            TheaterMode.endVanillaBypass();
        }
    }
}
