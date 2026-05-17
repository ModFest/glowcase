package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.class)
public interface RenderSystemAccessor {
	@Accessor static Thread getRenderThread() {
		throw new IllegalStateException();
	}
}
