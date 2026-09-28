package dev.hephaestus.glowcase.client;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class GlowcaseRenderTypes {
	private static final DepthStencilState DEPTH_BIAS = new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 1.0F, 10.0F);

	private static final Function<Boolean, RenderPipeline> SCREEN_PROGRAM = Util.memoize((culling) -> RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
			.withLocation(Glowcase.id("pipeline/screen"))
			.withCull(culling)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withDepthStencilState(DEPTH_BIAS)
			.build()
	));

	private static final OitPipelineSet SCREEN_OIT = RenderPipelines.register(
		OitPipelineSet.builder(
			"glowcase_screen",
			RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
		).build()
	);

	private static final BiFunction<Identifier, Boolean, RenderType> SCREEN = Util.memoize((texture, culling) -> {
		RenderSetup state = RenderSetup.builder(SCREEN_PROGRAM.apply(culling))
			.withTexture("Sampler0", texture)
			.setOitPipelines(SCREEN_OIT)
			.useLightmap()
			.sortOnUpload()
			.createRenderSetup();
		return RenderType.create("glowcase_screen", state);
	});

	public static RenderType getScreen(Identifier texture, boolean culling) {
		return SCREEN.apply(texture, culling);
	}
}
