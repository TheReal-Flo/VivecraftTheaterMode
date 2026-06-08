package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.render.TheaterRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Camera.class, priority = 2000)
abstract class CameraTheaterMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void vtm$forceVanillaEyePosition(
        BlockView area,
        Entity focusedEntity,
        boolean thirdPerson,
        boolean inverseView,
        float tickProgress,
        CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!TheaterRenderer.isRenderingTheaterFrame() || focusedEntity != client.player || client.player == null) {
            return;
        }

        Vec3d playerPos = client.player.getLerpedPos(tickProgress);
        double eyeY = playerPos.y + client.player.getEyeHeight(client.player.getPose());
        ((CameraAccessor) this).vtm$setPos(playerPos.x, eyeY, playerPos.z);
    }
}
