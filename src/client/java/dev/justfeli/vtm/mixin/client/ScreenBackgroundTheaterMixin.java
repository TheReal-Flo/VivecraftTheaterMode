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
 * Restores the vanilla in-world screen tint (the dark gradient that dims the game behind a GUI) while
 * in Theater mode.
 *
 * <p>Vivecraft's {@code ScreenVRMixin} cancels {@code renderBackground}, {@code renderInGameBackground}
 * and the blur whenever VR is running, because its screens float as standalone panels in the room where
 * a dim would be pointless. In Theater mode the screen is instead composited flat onto the theater panel
 * over the captured game frame, so the vanilla dim is exactly what we want back.
 *
 * <p>This injects at HEAD with priority 500 so it runs before Vivecraft's HEAD cancel (priority 1000),
 * drawing the gradient itself. It can't delegate to {@code renderInGameBackground} because Vivecraft
 * cancels that method too. The blur is deliberately left disabled: the blur post-effect samples the
 * (empty) GUI framebuffer rather than the world, so it would contribute nothing useful here.
 */
@Mixin(value = Screen.class, priority = 500)
abstract class ScreenBackgroundTheaterMixin {
    @Shadow
    public int width;

    @Shadow
    public int height;

    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void vtm$restoreTheaterTint(
        DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci)
    {
        if (TheaterMode.isActive() && MinecraftClient.getInstance().world != null) {
            // Same gradient vanilla's renderInGameBackground draws (0xC0101010 -> 0xD0101010).
            context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        }
    }
}
