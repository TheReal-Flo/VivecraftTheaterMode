package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import dev.justfeli.vtm.client.render.TheaterRenderer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the crosshair target follow the Theater camera instead of the VR aim.
 *
 * <p>In VR, Vivecraft replaces {@code GameRenderer.updateCrosshairTarget} (the crosshair raycast)
 * with one driven by the HMD/controller aim, so block breaking targets whatever the headset points
 * at - not what is centred on the theater screen. Here we bracket that call: first push the
 * mod-owned view yaw onto the player so its eye/look vector matches the theater camera, then run the
 * vanilla raycast by entering the vanilla bypass ({@code VR_RUNNING = false}), which disarms
 * Vivecraft's pick override. The resulting {@code crosshairTarget} - used by block breaking and
 * interaction - then matches the on-screen view.
 */
@Mixin(value = MinecraftClient.class, priority = 2000)
abstract class MinecraftClientPickTheaterMixin {
    @Inject(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;updateCrosshairTarget(F)V")
    )
    private void vtm$beforeTheaterPick(CallbackInfo ci) {
        if (TheaterMode.isActive()) {
            TheaterRenderer.applyViewYawToPlayer();
            TheaterMode.beginVanillaBypass();
        }
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;updateCrosshairTarget(F)V",
            shift = At.Shift.AFTER
        )
    )
    private void vtm$afterTheaterPick(CallbackInfo ci) {
        if (TheaterMode.isActive()) {
            TheaterMode.endVanillaBypass();
        }
    }
}
