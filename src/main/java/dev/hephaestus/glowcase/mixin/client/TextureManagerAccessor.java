package dev.hephaestus.glowcase.mixin.client;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureManager.class)
public interface TextureManagerAccessor {

	@Accessor("resourceManager")
	ResourceManager glowcase$getResourceManager();
}
