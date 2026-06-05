package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterModeState;
import dev.justfeli.vtm.client.playmode.TheaterPlayMode;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client.gui.framework.widgets.GuiVROption;
import org.vivecraft.client.gui.settings.GuiMainVRSettings;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.settings.VRSettings;

@Mixin(GuiMainVRSettings.class)
abstract class GuiMainVRSettingsMixin {
    @Inject(
        method = "lambda$new$0(Lorg/vivecraft/client/gui/framework/widgets/GuiVROption;Lnet/minecraft/util/math/Vec2f;)Ljava/lang/Boolean;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void vtm$cyclePlayMode(
        GuiVROption button,
        Vec2f clickPos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        VRSettings vrSettings = ClientDataHolderVR.getInstance().vrSettings;
        TheaterModeState.ensureLoaded(vrSettings);

        TheaterPlayMode currentMode = TheaterModeState.getMode();
        if (currentMode == TheaterPlayMode.STANDING) {
            TheaterModeState.queuePendingMode(TheaterPlayMode.SEATED);
            ((GuiMainVRSettingsAccessor) this).vtm$setConfirm(true);
            ((GuiVROptionsBaseAccessor) this).vtm$setReinit(true);
            cir.setReturnValue(Boolean.TRUE);
            return;
        }

        TheaterModeState.setMode(currentMode.next());
        TheaterModeState.syncIntoVivecraft(vrSettings);
        TheaterModeState.save();
        ((GuiVROptionsBaseAccessor) this).vtm$setReinit(true);
        cir.setReturnValue(Boolean.TRUE);
    }

    @Inject(
        method = "lambda$new$1(Lorg/vivecraft/client/gui/framework/widgets/GuiVROption;Lnet/minecraft/util/math/Vec2f;)Ljava/lang/Boolean;",
        at = @At("HEAD")
    )
    private void vtm$clearPendingModeOnCancel(
        GuiVROption button,
        Vec2f clickPos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        TheaterModeState.clearPendingMode();
    }

    @Inject(
        method = "lambda$new$2(Lorg/vivecraft/client/gui/framework/widgets/GuiVROption;Lnet/minecraft/util/math/Vec2f;)Ljava/lang/Boolean;",
        at = @At("HEAD")
    )
    private void vtm$applyPendingModeOnConfirm(
        GuiVROption button,
        Vec2f clickPos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        VRSettings vrSettings = ClientDataHolderVR.getInstance().vrSettings;
        TheaterModeState.ensureLoaded(vrSettings);
        TheaterModeState.setMode(TheaterModeState.consumePendingMode());
        TheaterModeState.syncIntoVivecraft(vrSettings);
        TheaterModeState.save();
    }
}
