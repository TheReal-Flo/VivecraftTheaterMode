package dev.justfeli.vtm.mixin.client;

import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameOptions.class)
public interface GameOptionsAccessor {
    @Accessor("hudHidden")
    boolean vtm$isHudHidden();

    @Accessor("hudHidden")
    void vtm$setHudHidden(boolean hudHidden);
}
