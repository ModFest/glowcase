package dev.hephaestus.glowcase.mixin.client.bakedbe.chunk;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public interface RenderSectionAccessor {
	@Accessor SectionRenderDispatcher getThis$0();
}
