package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.compat.ControlifyCompat;
import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets Controlify's virtual mouse cursor work in Theater GUIs.
 *
 * <p>Controlify moves the OS cursor with {@code glfwSetCursorPos} to snap onto slots. But Vivecraft's
 * {@code Mouse.onCursorPos} rescales every cursor position by {@code GUI_WIDTH/actualScreenWidth}
 * (its emulated VR GUI resolution), which double-transforms Controlify's target so the hovered slot
 * ends up wrong. While Controlify is driving a Theater screen we run the cursor callback through the
 * vanilla bypass ({@code VR_RUNNING = false}), which disables that rescale (it is gated on
 * {@code VR_RUNNING}), so Controlify's position reaches the screen unchanged. Low priority so the
 * bypass is set before Vivecraft's scaling reads {@code VR_RUNNING}.
 */
@Mixin(value = Mouse.class, priority = 500)
abstract class MouseCursorTheaterBypassMixin {
    @Unique
    private boolean vtm$cursorBypassActive;

    @Unique
    private static boolean vtm$shouldBypassCursor() {
        return TheaterMode.isActive() &&
            MinecraftClient.getInstance().currentScreen != null &&
            ControlifyCompat.isControllerActive();
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void vtm$beginCursorBypass(CallbackInfo ci) {
        if (vtm$shouldBypassCursor()) {
            vtm$cursorBypassActive = true;
            TheaterMode.beginVanillaBypass();
        }
    }

    @Inject(method = "onCursorPos", at = @At("RETURN"))
    private void vtm$endCursorBypass(CallbackInfo ci) {
        if (vtm$cursorBypassActive) {
            vtm$cursorBypassActive = false;
            TheaterMode.endVanillaBypass();
        }
    }
}
