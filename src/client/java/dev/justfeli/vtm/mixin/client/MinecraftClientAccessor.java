package dev.justfeli.vtm.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("field_1689")
    void vtm$setFramebuffer(Framebuffer framebuffer);

    @Accessor("field_1719")
    Entity vtm$getCameraEntity();

    @Accessor("field_1719")
    void vtm$setCameraEntity(Entity entity);
}
