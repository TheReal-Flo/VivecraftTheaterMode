package dev.justfeli.vtm.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.vivecraft.client.gui.settings.GuiMainVRSettings;

@Mixin(GuiMainVRSettings.class)
interface GuiMainVRSettingsAccessor {
    @Accessor("isConfirm")
    void vtm$setConfirm(boolean value);
}
