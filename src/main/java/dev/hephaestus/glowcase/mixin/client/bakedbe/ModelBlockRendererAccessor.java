package dev.hephaestus.glowcase.mixin.client.bakedbe;

import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelBlockRenderer.class)
public interface ModelBlockRendererAccessor {
	@Accessor BlockModelLighter getLighter();
}
