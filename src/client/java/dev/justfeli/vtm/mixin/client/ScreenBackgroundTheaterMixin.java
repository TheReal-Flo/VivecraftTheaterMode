package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores the vanilla screen dim while in Theater mode.
 *
 * <p>Vivecraft's {@code ScreenVRMixin} cancels {@code renderBackground}, {@code renderInGameBackground}
 * and the blur whenever VR is running, because its screens float as standalone panels where a dim would
 * be pointless. In Theater mode the screen is composited flat onto the theater panel over the captured
 * game frame, so the vanilla dim is exactly what we want back.
 *
 * <p>Vanilla uses two different dims depending on the screen, so we restore each on its real method:
 * <ul>
 *   <li>Regular screens: {@code renderBackground} runs {@code applyBlur} + {@code renderDarkening} (a
 *       light textured dim). We redraw just the darkening; the blur is left off since it would sample
 *       the empty GUI framebuffer rather than the world.</li>
 *   <li>{@code HandledScreen}s (inventories, etc.) override {@code renderBackground} and instead dim via
 *       {@code renderInGameBackground} (a stronger gradient) before drawing the container. That method
 *       lives on {@link Screen}, so injecting here also covers those screens.</li>
 * </ul>
 *
 * <p>Both injects run at HEAD with priority 500 so they execute before Vivecraft's HEAD cancel (priority
 * 1000): we draw the dim, then let Vivecraft cancel the rest of the original method (avoiding the blur
 * and any double draw). {@code renderDarkening} isn't cancelled by Vivecraft, so it's safe to delegate to.
 */
@Mixin(value = Screen.class, priority = 500)
abstract class ScreenBackgroundTheaterMixin {
    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected abstract void renderDarkening(DrawContext context);

    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void vtm$restoreDarkening(
        DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci)
    {
        if (TheaterMode.isActive() && MinecraftClient.getInstance().world != null) {
            renderDarkening(context);
        }
    }

    @Inject(method = "renderInGameBackground", at = @At("HEAD"))
    private void vtm$restoreInGameBackground(DrawContext context, CallbackInfo ci) {
        if (TheaterMode.isActive() && MinecraftClient.getInstance().world != null) {
            // The stronger gradient vanilla's renderInGameBackground draws (0xC0101010 -> 0xD0101010).
            context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        }
    }
}
