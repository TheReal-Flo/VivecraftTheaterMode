package dev.justfeli.vtm.mixin.client;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("standingEyeHeight")
    float vtm$getStandingEyeHeightField();

    @Accessor("standingEyeHeight")
    void vtm$setStandingEyeHeightField(float standingEyeHeight);
}
