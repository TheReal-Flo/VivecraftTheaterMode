package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.compat.ControlifyCompat;
import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.vivecraft.client_vr.render.helpers.RenderHelper;
import org.vivecraft.client_vr.render.helpers.VRPassHelper;

/**
 * Hides Vivecraft's GUI cursor crosshair while Controlify is driving input with a controller.
 *
 * <p>While a screen is open, {@link VRPassHelper} draws a crosshair at the mouse position
 * ({@link RenderHelper#drawMouseMenuQuad}) so you can point at GUI elements. When Controlify is
 * actively controlling, GUI navigation is done with the controller, so that mouse-cursor crosshair
 * is redundant and just sits on screen - so we skip drawing it (in Theater mode). The matching
 * cursor functionality is disabled in {@code GuiHandlerTheaterMixin}.
 */
@Mixin(VRPassHelper.class)
abstract class VRPassHelperCursorTheaterMixin {
    @Redirect(
        method = "renderAndSubmit",
        at = @At(
            value = "INVOKE",
            target = "Lorg/vivecraft/client_vr/render/helpers/RenderHelper;drawMouseMenuQuad(Lnet/minecraft/client/gui/DrawContext;II)V"
        )
    )
    private static void vtm$skipGuiCursorForControlify(DrawContext guiGraphics, int x, int y) {
        if (TheaterMode.isActive() && ControlifyCompat.isControllerActive()) {
            return;
        }
        RenderHelper.drawMouseMenuQuad(guiGraphics, x, y);
    }
}
