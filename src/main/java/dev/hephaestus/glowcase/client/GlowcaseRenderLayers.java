package dev.hephaestus.glowcase.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.hephaestus.glowcase.Glowcase;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.util.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;

public abstract class GlowcaseRenderLayers extends RenderType {
	public static final Function<Boolean, RenderPipeline> SCREEN_PROGRAM = Util.memoize((culling) -> RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
			.withLocation(Glowcase.id("pipeline/screen"))
			.withVertexShader("core/rendertype_text")
			.withFragmentShader("core/rendertype_text")
			.withCull(culling)
			.withSampler("Sampler0")
			.withSampler("Sampler2")
			.withDepthBias(-1.0F, -1.0F)
			.build()
	));

	public static final RenderPipeline TEXT_PLATE_PROGRAM = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).withLocation(Glowcase.id("pipeline/text_plate")).withCull(false).build()
	);

	// Use a custom render layer to render the text plate - mimics DrawableHelper's RenderSystem call
	public static final RenderType TEXT_PLATE = RenderType.create("glowcase_text_plate",
		256,
		true,
		true,
		TEXT_PLATE_PROGRAM,
		RenderType.CompositeState.builder().setTextureState(NO_TEXTURE).createCompositeState(false));


	private static final BiFunction<Identifier, Boolean, RenderType> SCREEN = Util.memoize((texture, culling) -> {
		return RenderType.create(
			"glowcase_screen",
			786432,
			false,
			true,
			SCREEN_PROGRAM.apply(culling),
			CompositeState.builder()
				.setTextureState(new TextureStateShard(texture, false))
				.setLightmapState(LIGHTMAP)
				.createCompositeState(false));
	});

	public GlowcaseRenderLayers(String name, int size, boolean hasCrumbling, boolean translucent, Runnable begin, Runnable end) {
		super(name, size, hasCrumbling, translucent, begin, end);
	}

	public static RenderType getScreen(Identifier texture, boolean culling) {
		return SCREEN.apply(texture, culling);
	}
}
