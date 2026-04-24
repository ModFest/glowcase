package dev.hephaestus.glowcase.client.render.bakedbe;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.hephaestus.glowcase.client.render.bakedbe.buffers.BakedBEBufferSource;
import dev.hephaestus.glowcase.client.render.bakedbe.buffers.BakedBERenderBuffers;
import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh;
import dev.hephaestus.glowcase.mixin.client.bakedbe.FeatureRenderDispatcherAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;

import java.io.Closeable;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class BakedBERenderDispatcher {
	private final ModelManager modelManager;
	private final BakedBEBufferSource bufferSource;
	private final AtlasManager atlasManager;
	private final OutlineBufferSource outlineBufferSource;
	private final MultiBufferSource.BufferSource crumblingBufferSource;
	private final Font font;
	private final GameRenderState gameRenderState;
	private final ShadowFeatureRenderer shadowFeatureRenderer = new ShadowFeatureRenderer();
	private final FlameFeatureRenderer flameFeatureRenderer = new FlameFeatureRenderer();
	private final ModelFeatureRenderer modelFeatureRenderer = new ModelFeatureRenderer();
	private final ModelPartFeatureRenderer modelPartFeatureRenderer = new ModelPartFeatureRenderer();
	private final NameTagFeatureRenderer nameTagFeatureRenderer = new NameTagFeatureRenderer();
	private final TextFeatureRenderer textFeatureRenderer = new TextFeatureRenderer();
	private final LeashFeatureRenderer leashFeatureRenderer = new LeashFeatureRenderer();
	private final ItemFeatureRenderer itemFeatureRenderer = new ItemFeatureRenderer();
	private final CustomFeatureRenderer customFeatureRenderer = new CustomFeatureRenderer();
	private final BlockFeatureRenderer blockFeatureRenderer = new BlockFeatureRenderer();

	public BakedBERenderDispatcher(
		final ModelManager modelManager,
		final BakedBEBufferSource bufferSource,
		final AtlasManager atlasManager,
		final OutlineBufferSource outlineBufferSource,
		final MultiBufferSource.BufferSource crumblingBufferSource,
		final Font font,
		final GameRenderState gameRenderState
	) {
		this.modelManager = modelManager;
		this.bufferSource = bufferSource;
		this.atlasManager = atlasManager;
		this.outlineBufferSource = outlineBufferSource;
		this.crumblingBufferSource = crumblingBufferSource;
		this.font = font;
		this.gameRenderState = gameRenderState;
	}

	public void renderSolidFeatures(final SubmitNodeStorage submitNodeStorage) {
		for (SubmitNodeCollection collection : submitNodeStorage.getSubmitsPerOrder().values()) {
			this.modelFeatureRenderer.renderSolid(collection, this.bufferSource, this.outlineBufferSource, this.crumblingBufferSource);
			this.modelPartFeatureRenderer.renderSolid(collection, this.bufferSource, this.outlineBufferSource, this.crumblingBufferSource);
			this.flameFeatureRenderer.renderSolid(collection, this.bufferSource, this.atlasManager);
			this.leashFeatureRenderer.renderSolid(collection, this.bufferSource);
			this.itemFeatureRenderer.renderSolid(collection, this.bufferSource, this.outlineBufferSource);
			this.blockFeatureRenderer.renderSolid(collection, this.bufferSource, this.modelManager.getBlockStateModelSet(), this.outlineBufferSource, this.gameRenderState.optionsRenderState);
			this.customFeatureRenderer.renderSolid(collection, this.bufferSource);
		}
	}

	public void renderTranslucentFeatures(final SubmitNodeStorage submitNodeStorage) {
		for (SubmitNodeCollection collection : submitNodeStorage.getSubmitsPerOrder().values()) {
			this.shadowFeatureRenderer.renderTranslucent(collection, this.bufferSource);
			this.modelFeatureRenderer.renderTranslucent(collection, this.bufferSource, this.outlineBufferSource, this.crumblingBufferSource);
			this.modelPartFeatureRenderer.renderTranslucent(collection, this.bufferSource, this.outlineBufferSource, this.crumblingBufferSource);
			this.nameTagFeatureRenderer.renderTranslucent(collection, this.bufferSource, this.font);
			this.textFeatureRenderer.renderTranslucent(collection, this.bufferSource);
			this.itemFeatureRenderer.renderTranslucent(collection, this.bufferSource, this.outlineBufferSource);
			this.blockFeatureRenderer
				.renderTranslucent(
					collection,
					this.bufferSource,
					this.modelManager.getBlockStateModelSet(),
					this.outlineBufferSource,
					this.crumblingBufferSource,
					this.gameRenderState.optionsRenderState
				);
			this.customFeatureRenderer.renderTranslucent(collection, this.bufferSource);
		}
	}

	public void clearSubmitNodes(final SubmitNodeStorage submitNodeStorage) {
		submitNodeStorage.clear();
	}

	public BakedMeshes buildAllFeatures(final SubmitNodeStorage submitNodeStorage, final VertexSorting vertexSorting) {
		ProfilerFiller profiler = Profiler.get();
		Map<RenderType, CompiledMesh> solidMeshes;
		try (Zone _  = profiler.zone("solid")) {
			profiler.push("build_buffers");
			renderSolidFeatures(submitNodeStorage);

			profiler.popPush("build_mesh");
			solidMeshes = bufferSource.buildAllBatches(vertexSorting, false);
			profiler.pop();
		}

		Map<RenderType, CompiledMesh> translucentMeshes;
		try (Zone _  = profiler.zone("translucent")) {
			profiler.push("build_buffers");
			renderTranslucentFeatures(submitNodeStorage);

			profiler.popPush("build_mesh");
			translucentMeshes = bufferSource.buildAllBatches(vertexSorting, true);
			profiler.pop();
		}

		endFrame(submitNodeStorage);

		return new BakedMeshes(solidMeshes, translucentMeshes);
	}

	public void endFrame(final SubmitNodeStorage submitNodeStorage) {
		clearSubmitNodes(submitNodeStorage);
	}

	public static class Builder {
		public static BakedBERenderDispatcher createBakedBERenderDispatcher() {
			BakedBERenderBuffers renderBuffers = new BakedBERenderBuffers();
			GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
			FeatureRenderDispatcherAccessor renderDispatcherAccessor = (FeatureRenderDispatcherAccessor) gameRenderer.getFeatureRenderDispatcher();
			return new BakedBERenderDispatcher(
				renderDispatcherAccessor.getModelManager(),
				renderBuffers.bufferSource(),
				Minecraft.getInstance().getAtlasManager(),
				renderBuffers.outlineBufferSource(),
				renderBuffers.crumblingBufferSource(),
				renderDispatcherAccessor.getFont(),
				gameRenderer.getGameRenderState()
			);
		}
	}
}
