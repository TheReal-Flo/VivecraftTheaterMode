package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayerEntity.class, priority = 2000)
abstract class ClientPlayerEntityMoveMixin {
    @Unique
    private Entity vtm$previousCameraEntity;

    @Unique
    private boolean vtm$cameraEntityOverridden;

    @Inject(method = "move", at = @At("HEAD"))
    private void vtm$useVanillaMoveInTheater(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!TheaterMode.isActive() || client.player != (Object) this) {
            return;
        }

        MinecraftClientAccessor accessor = (MinecraftClientAccessor) client;
        Entity currentCameraEntity = accessor.vtm$getCameraEntity();
        if (currentCameraEntity == (Object) this) {
            this.vtm$previousCameraEntity = currentCameraEntity;
            this.vtm$cameraEntityOverridden = true;
            accessor.vtm$setCameraEntity(null);
        }
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void vtm$restoreCameraEntityAfterMove(CallbackInfo ci) {
        if (!this.vtm$cameraEntityOverridden) {
            return;
        }

        ((MinecraftClientAccessor) MinecraftClient.getInstance()).vtm$setCameraEntity(this.vtm$previousCameraEntity);
        this.vtm$previousCameraEntity = null;
        this.vtm$cameraEntityOverridden = false;
    }
}
