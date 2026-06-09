package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_xr.render_pass.RenderPassManager;
import org.vivecraft.client_xr.render_pass.RenderPassType;

/**
 * Restores the vanilla 2D crosshair on the Theater screen.
 *
 * <p>In VR, {@code GuiVRMixin.vivecraft$cancelCrosshair} cancels {@link InGameHud}'s crosshair
 * whenever {@code RenderPassType.isGuiOnly()} - which is always true while the flat Theater HUD is
 * drawn - because VR normally shows a 3D world crosshair instead. In Theater mode we want the flat
 * crosshair on the panel like the rest of the HUD.
 *
 * <p>Vivecraft's cancel runs at {@code renderCrosshair} HEAD reading the pass type, so we briefly
 * set the pass type to non-GUI around the crosshair render: our HEAD runs first (low mixin priority),
 * which makes {@code isGuiOnly()} read false so the cancel is skipped and the vanilla crosshair draws;
 * RETURN restores the real pass type. The swap is scoped to this single method, so the other
 * GUI-only overlay cancels (vignette, portal, spyglass, ...) are unaffected.
 */
@Mixin(value = InGameHud.class, priority = 500)
abstract class InGameHudCrosshairTheaterMixin {
    @Unique
    private RenderPassType vtm$savedRenderPass;
    @Unique
    private boolean vtm$renderPassSwapped;

    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void vtm$allowTheaterCrosshair(CallbackInfo ci) {
        if (TheaterMode.isActive() && RenderPassType.isGuiOnly()) {
            vtm$savedRenderPass = RenderPassManager.RENDER_PASS_TYPE;
            RenderPassManager.RENDER_PASS_TYPE = RenderPassType.VANILLA;
            vtm$renderPassSwapped = true;
        }
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void vtm$restoreRenderPassAfterCrosshair(CallbackInfo ci) {
        if (vtm$renderPassSwapped) {
            RenderPassManager.RENDER_PASS_TYPE = vtm$savedRenderPass;
            vtm$renderPassSwapped = false;
        }
    }
}
