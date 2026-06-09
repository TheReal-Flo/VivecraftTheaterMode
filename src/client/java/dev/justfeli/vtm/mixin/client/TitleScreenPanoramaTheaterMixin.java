package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores the title-screen panorama in Theater mode.
 *
 * <p>{@link TitleScreen} reimplements {@code renderPanoramaBackground} (it draws the rotating panorama
 * directly rather than calling {@link Screen}), and overrides {@code renderBackground} to an empty
 * method. So {@code ScreenBackgroundTheaterMixin}'s {@link Screen}-targeted injects never run for the
 * main menu. On top of that Vivecraft cancels the panorama (its own {@code TitleScreenMixin}, plus
 * {@code ScreenVRMixin}) whenever a menu world / fallback panorama is configured, leaving the title
 * screen with a black backdrop on the theater panel.
 *
 * <p>We redraw the panorama at HEAD with priority 500 so it runs before Vivecraft's HEAD cancel
 * (priority 1000); Vivecraft then cancels the original body, so the panorama is drawn exactly once.
 * Vivecraft's {@code CubeMapVRMixin} still swaps in its VR-solid panorama pipeline, so this renders the
 * same way Vivecraft's own VR title panorama does.
 *
 * <p>The mixin extends {@link Screen} so the inherited {@code ROTATING_PANORAMA_RENDERER} static and
 * {@code width}/{@code height} can be referenced directly (the same pattern Vivecraft's TitleScreenMixin
 * uses for inherited members); {@code @Shadow}ing an inherited static field does not resolve on the
 * subclass target.
 */
@Mixin(value = TitleScreen.class, priority = 500)
abstract class TitleScreenPanoramaTheaterMixin extends Screen {
    private TitleScreenPanoramaTheaterMixin(Text title) {
        super(title);
    }

    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"))
    private void vtm$restorePanorama(DrawContext context, float delta, CallbackInfo ci) {
        if (TheaterMode.isActive()) {
            // Same call vanilla TitleScreen.renderPanoramaBackground makes (full opacity, no fade-in).
            ROTATING_PANORAMA_RENDERER.render(context, this.width, this.height, 1.0F, delta);
        }
    }
}
