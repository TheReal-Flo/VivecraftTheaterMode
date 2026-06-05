package dev.justfeli.vtm.mixin.client;

import dev.justfeli.vtm.client.playmode.TheaterModeState;
import dev.justfeli.vtm.client.playmode.TheaterPlayMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_vr.settings.VRSettings;

@Mixin(VRSettings.class)
abstract class VRSettingsPlayModeMixin {
    @Inject(method = "loadOptions()V", at = @At("TAIL"))
    private void vtm$loadPlayMode(CallbackInfo ci) {
        TheaterModeState.loadOrInfer((VRSettings) (Object) this);
    }

    @Inject(method = "saveOptions()V", at = @At("HEAD"))
    private void vtm$syncPlayModeBeforeSave(CallbackInfo ci) {
        VRSettings vrSettings = (VRSettings) (Object) this;
        TheaterModeState.ensureLoaded(vrSettings);
        TheaterModeState.save();
    }

    @Inject(
        method = "loadDefault(Lorg/vivecraft/client_vr/settings/VRSettings$VrOptions;)V",
        at = @At("HEAD")
    )
    private void vtm$resetPlayMode(VRSettings.VrOptions option, CallbackInfo ci) {
        if (option != VRSettings.VrOptions.PLAY_MODE_SEATED) {
            return;
        }

        TheaterModeState.setMode(TheaterPlayMode.STANDING);
        TheaterModeState.syncIntoVivecraft((VRSettings) (Object) this);
        TheaterModeState.save();
    }

    @Inject(
        method = "getButtonDisplayString(Lorg/vivecraft/client_vr/settings/VRSettings$VrOptions;Z)Ljava/lang/String;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void vtm$getPlayModeDisplayString(
        VRSettings.VrOptions option,
        boolean valueOnly,
        CallbackInfoReturnable<String> cir
    ) {
        if (option != VRSettings.VrOptions.PLAY_MODE_SEATED) {
            return;
        }

        TheaterModeState.ensureLoaded((VRSettings) (Object) this);
        cir.setReturnValue(valueOnly ? TheaterModeState.getModeDisplayString() : TheaterModeState.getButtonDisplayString());
    }

    @Inject(
        method = "setOptionValue(Lorg/vivecraft/client_vr/settings/VRSettings$VrOptions;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void vtm$cyclePlayMode(VRSettings.VrOptions option, CallbackInfo ci) {
        if (option != VRSettings.VrOptions.PLAY_MODE_SEATED) {
            return;
        }

        VRSettings vrSettings = (VRSettings) (Object) this;
        TheaterModeState.ensureLoaded(vrSettings);
        TheaterModeState.setMode(TheaterModeState.getMode().next());
        TheaterModeState.syncIntoVivecraft(vrSettings);
        TheaterModeState.save();
        ci.cancel();
    }
}
