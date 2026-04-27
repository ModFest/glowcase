package dev.hephaestus.glowcase.client;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class GlowcaseRenderTypes {
	private static final DepthStencilState NEGATIVE_DEPTH_BIAS = new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -1.f, -1.f);

	private static final Function<Boolean, RenderPipeline> SCREEN_PROGRAM = Util.memoize((culling) -> RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
			.withLocation(Glowcase.id("pipeline/screen"))
			.withVertexShader("core/rendertype_text")
			.withFragmentShader("core/rendertype_text")
			.withCull(culling)
			.withSampler("Sampler0")
			.withSampler("Sampler2")
			.withDepthStencilState(NEGATIVE_DEPTH_BIAS)
			.build()
	));


	private static final BiFunction<Identifier, Boolean, RenderType> SCREEN = Util.memoize((texture, culling) -> {
		RenderSetup state = RenderSetup.builder(SCREEN_PROGRAM.apply(culling))
			.withTexture("Sampler0", texture)
			.useLightmap()
			.bufferSize(RenderType.SMALL_BUFFER_SIZE)
			.sortOnUpload()
			.createRenderSetup();
		return RenderType.create("glowcase_screen", state);
	});

	public static RenderType getScreen(Identifier texture, boolean culling) {
		return SCREEN.apply(texture, culling);
	}
}
