package dev.justfeli.vtm.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.vivecraft.client.gui.framework.screens.GuiVROptionsBase;

@Mixin(GuiVROptionsBase.class)
interface GuiVROptionsBaseAccessor {
    @Accessor("reinit")
    void vtm$setReinit(boolean value);
}
