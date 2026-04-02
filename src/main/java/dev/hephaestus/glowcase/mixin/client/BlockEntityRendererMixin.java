package dev.hephaestus.glowcase.mixin.client;

import dev.hephaestus.glowcase.mixinsupport.BakingRendererExtension;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockEntityRenderer.class)
public interface BlockEntityRendererMixin extends BakingRendererExtension {
	@Override
	default boolean glowcase$isBakingRenderer() {
		return false;
	}
}
