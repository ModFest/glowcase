package dev.hephaestus.glowcase.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
	@Invoker void callSetSectionDirty(final int sectionX, final int sectionY, final int sectionZ, final boolean playerChanged);
}
