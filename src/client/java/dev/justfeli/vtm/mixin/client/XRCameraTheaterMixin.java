package dev.justfeli.vtm.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.justfeli.vtm.client.render.TheaterRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.render.XRCamera;

/**
 * Makes the theater frame render from the vanilla, mouse-driven player camera instead of the HMD.
 *
 * <p>Two things are needed. First, {@link XRCamera#update} branches on
 * {@code RenderPassType.isVanilla()} to decide between the vanilla camera (player rotation) and the
 * VR camera (HMD pose); we force the vanilla branch while the theater frame renders. Second, the
 * camera reads the player entity's yaw/pitch, but Vivecraft's render-view-entity logic transiently
 * overwrites those with the HMD pose during rendering (and skips its own restore on vanilla passes),
 * so we re-assert the captured mouse-driven rotation right before the camera reads it.
 */
@Mixin(value = XRCamera.class, priority = 2000)
abstract class XRCameraTheaterMixin {
    @Inject(method = "update", at = @At("HEAD"))
    private void vtm$applyCleanRotation(
        BlockView area,
        Entity focusedEntity,
        boolean thirdPerson,
        boolean inverseView,
        float tickDelta,
        CallbackInfo ci
    ) {
        if (focusedEntity == null
            || !TheaterRenderer.isRenderingTheaterFrame()
            || !TheaterRenderer.hasCleanRotation()) {
            return;
        }

        float yaw = TheaterRenderer.getCleanYaw();
        float pitch = TheaterRenderer.getCleanPitch();
        focusedEntity.setYaw(yaw);
        focusedEntity.setPitch(pitch);
        focusedEntity.lastYaw = yaw;
        focusedEntity.lastPitch = pitch;
    }

    @WrapOperation(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lorg/vivecraft/client_xr/render_pass/RenderPassType;isVanilla()Z"
        )
    )
    private boolean vtm$forceVanillaCameraInTheater(Operation<Boolean> original) {
        if (TheaterRenderer.isRenderingTheaterFrame()) {
            return true;
        }
        return original.call();
    }
}
