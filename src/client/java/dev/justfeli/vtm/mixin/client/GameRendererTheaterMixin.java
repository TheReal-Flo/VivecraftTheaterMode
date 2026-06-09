package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.render.TheaterRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vivecraft's "render view entity" (RVE) machinery temporarily moves the camera entity to the VR
 * eye pose for the duration of {@code renderLevel}, caching the real pose first and restoring it at
 * the end. The restore is guarded by {@code !RenderPassType.isVanilla()}.
 *
 * <p>The theater capture renders the world while forcing {@code isVanilla() == true}, so the cache
 * runs but the restore is skipped. That leaves Vivecraft's {@code cached} flag stuck true: the next
 * real VR frame then skips re-caching and restores the camera entity to a STALE position and head
 * yaw, which manifests as the player position rubber-banding (broken movement) and the horizontal
 * view snapping back.
 *
 * <p>Rather than try to re-balance after the fact, make the whole RVE cycle inert while the theater
 * frame is being captured. {@code setupRVEAtDevice} self-guards on {@code cached}, so cancelling the
 * three public entry points here means nothing is cached, nothing is moved to the eye pose, and
 * nothing stale is restored - the entity keeps its real (mouse-driven) pose throughout. This only
 * applies during {@link TheaterRenderer#isRenderingTheaterFrame()}; the normal VR passes are
 * untouched.
 */
@Mixin(value = GameRenderer.class, priority = 1500)
abstract class GameRendererTheaterMixin {
    @Inject(method = "vivecraft$cacheRVEPos", at = @At("HEAD"), cancellable = true, remap = false)
    private void vtm$skipCacheRveInTheater(Entity entity, CallbackInfo ci) {
        if (TheaterRenderer.isRenderingTheaterFrame()) {
            ci.cancel();
        }
    }

    @Inject(method = "vivecraft$setupRVE", at = @At("HEAD"), cancellable = true, remap = false)
    private void vtm$skipSetupRveInTheater(CallbackInfo ci) {
        if (TheaterRenderer.isRenderingTheaterFrame()) {
            ci.cancel();
        }
    }

    @Inject(method = "vivecraft$restoreRVEPos", at = @At("HEAD"), cancellable = true, remap = false)
    private void vtm$skipRestoreRveInTheater(Entity entity, CallbackInfo ci) {
        if (TheaterRenderer.isRenderingTheaterFrame()) {
            ci.cancel();
        }
    }
}
