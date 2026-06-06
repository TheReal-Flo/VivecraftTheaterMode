package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterMode;
import dev.justfeli.vtm.client.render.TheaterRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.gameplay.screenhandlers.GuiHandler;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientRenderMixin {
    @Inject(method = "method_1523", at = @At("HEAD"))
    private void vtm$beginGameplayBypassBeforeFrame(boolean tick, CallbackInfo ci) {
        if (TheaterMode.isActive() && MinecraftClient.getInstance().currentScreen == null) {
            TheaterMode.beginVanillaBypass();
        }
    }

    @Inject(
        method = "method_1523",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_757;method_3192(Lnet/minecraft/class_9779;Z)V"
        )
    )
    private void vtm$endGameplayBypassBeforeMainRender(boolean tick, CallbackInfo ci) {
        if (TheaterMode.isActive() && MinecraftClient.getInstance().currentScreen == null) {
            TheaterMode.endVanillaBypass();
        }
    }

    @Inject(
        method = "method_1523",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_757;method_3192(Lnet/minecraft/class_9779;Z)V"
        )
    )
    private void vtm$clearGuiFramebufferBeforeMainRender(boolean tick, CallbackInfo ci) {
        if (TheaterMode.isActive() && GuiHandler.GUI_FRAMEBUFFER != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                GuiHandler.GUI_FRAMEBUFFER.getColorAttachment(), 0x00000000,
                GuiHandler.GUI_FRAMEBUFFER.getDepthAttachment(), 1.0
            );
        }
    }

    @Inject(
        method = "method_1523",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_757;method_3192(Lnet/minecraft/class_9779;Z)V",
            shift = At.Shift.AFTER
        )
    )
    private void vtm$resumeGameplayBypassAfterMainRender(boolean tick, CallbackInfo ci) {
        if (TheaterMode.isActive() && MinecraftClient.getInstance().currentScreen == null) {
            TheaterMode.beginVanillaBypass();
        }
    }

    @Inject(
        method = "method_1523",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_757;method_3192(Lnet/minecraft/class_9779;Z)V",
            shift = At.Shift.AFTER
        )
    )
    private void vtm$renderTheaterFramebufferAfterMainRender(boolean tick, CallbackInfo ci) {
        if (TheaterMode.isActive()) {
            TheaterRenderer.renderVanillaFrameToGui();
        }
    }

    @Inject(method = "method_1523", at = @At("TAIL"))
    private void vtm$endGameplayBypassAfterFrame(boolean tick, CallbackInfo ci) {
        if (TheaterMode.isActive() && MinecraftClient.getInstance().currentScreen == null) {
            TheaterMode.endVanillaBypass();
        }
    }
}
