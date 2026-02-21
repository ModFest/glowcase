package dev.hephaestus.glowcase.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderType.CompositeRenderType.class)
public interface CompositeRenderTypeAccessor {
	@Accessor("renderPipeline")
	RenderPipeline getPipeline();

	@Accessor("state")
	RenderType.CompositeState getPhases();
}
