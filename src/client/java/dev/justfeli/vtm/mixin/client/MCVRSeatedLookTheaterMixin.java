package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.vivecraft.client_vr.provider.MCVR;

/**
 * In seated VR, {@link MCVR#updateAim()} hijacks the desktop mouse: while the cursor is grabbed it
 * maps the mouse X position onto a "keyhole" that rotates {@code seatedRot}/{@code worldRotation}
 * (with a dead zone and constant cursor re-centering) and maps the mouse Y onto the controller
 * {@code aimPitch} instead of the view pitch. That is what limits Theater turning to the keyhole
 * range (~45deg), makes it rubberband (the cursor is teleported back to the FOV edge every frame),
 * and prevents the mouse from changing the actual view pitch.
 *
 * <p>In Theater mode we want plain, unlimited vanilla mouse look, so we make the keyhole's
 * grab check read {@code false}. The cursor stays grabbed for vanilla {@code Mouse.updateMouse}
 * (which the {@link MouseTheaterBypassMixin} lets through), but Vivecraft no longer touches it.
 */
@Mixin(MCVR.class)
abstract class MCVRSeatedLookTheaterMixin {
    @Redirect(
        method = "updateAim",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Mouse;isCursorLocked()Z")
    )
    private boolean vtm$skipSeatedKeyholeInTheater(Mouse mouse) {
        return mouse.isCursorLocked() && !TheaterMode.isActive();
    }
}
