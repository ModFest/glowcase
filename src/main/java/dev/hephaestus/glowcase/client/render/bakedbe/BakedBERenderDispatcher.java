package dev.hephaestus.glowcase.client.render.bakedbe;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.hephaestus.glowcase.client.render.bakedbe.buffers.BakedBEBufferSource;
import dev.hephaestus.glowcase.client.render.bakedbe.buffers.BakedBERenderBuffers;
import dev.hephaestus.glowcase.client.render.bakedbe.buffers.MeshTooComplex;
import dev.hephaestus.glowcase.client.render.bakedbe.section.RenderSectionPos;
import dev.hephaestus.glowcase.mixin.client.bakedbe.FeatureRenderDispatcherAccessor;
import dev.hephaestus.glowcase.util.collections.Pool;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
@Environment(EnvType.CLIENT)
public class BakedBERenderDispatcher {
	private static final int AVAILABLE_CORES = Runtime.getRuntime().availableProcessors();
	private static final Pool<BakedBERenderDispatcher> RENDER_DISPATCHERS = new Pool<>(BakedBERenderDispatcher::createBakedBERenderDispatcher, AVAILABLE_CORES);
	private static final Pool<SubmitNodeStorage> NODE_STORAGE_POOL = new Pool<>(SubmitNodeStorage::new, AVAILABLE_CORES);

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

	private BakedBERenderDispatcher(
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

	public static SubmitNodeStorage getNodeStorage() {
		return NODE_STORAGE_POOL.acquire();
	}

	public static void returnNodeStorage(final SubmitNodeStorage submitNodeStorage) {
		submitNodeStorage.clear();
		submitNodeStorage.endFrame();
		NODE_STORAGE_POOL.release(submitNodeStorage);
	}

	public static void checkPools() {
		NODE_STORAGE_POOL.check();
		RENDER_DISPATCHERS.check();
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

	public static BakedMeshes buildAllFeatures(final long sectionNode, final SubmitNodeStorage submitNodeStorage, final VertexSorting vertexSorting) {
		BakedBERenderDispatcher dispatcher = RENDER_DISPATCHERS.acquire();

		try {
			return dispatcher.buildAll(submitNodeStorage, vertexSorting);
		} catch (MeshTooComplex e) {
			ErrorRenderer.logError(e, sectionNode);
			return dispatcher.new ErrorRenderer().build(vertexSorting);
		}  finally {
			RENDER_DISPATCHERS.release(dispatcher);
			returnNodeStorage(submitNodeStorage);
		}
	}

	private BakedMeshes buildAll(final SubmitNodeStorage submitNodeStorage, final VertexSorting vertexSorting) throws MeshTooComplex {
		ProfilerFiller profiler = Profiler.get();

		try {
			profiler.push("build_mesh");
			renderSolidFeatures(submitNodeStorage);
			renderTranslucentFeatures(submitNodeStorage);

			profiler.popPush("bake");
			return new BakedMeshes(bufferSource.buildAllBatches(vertexSorting));
		} catch (MeshTooComplex | IllegalArgumentException e) {
			// We only want to catch the buffer capacity exceeded exception from IllegalArgumentException
			if (e instanceof IllegalArgumentException && !e.getStackTrace()[0].getClassName().equals(ByteBufferBuilder.class.getName())) throw e;
			throw bufferSource.abort(e);
		} finally {
			profiler.pop();
		}
	}

	private static BakedBERenderDispatcher createBakedBERenderDispatcher() {
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

	@SuppressWarnings("DuplicatedCode")
	public class ErrorRenderer {
		private static final Logger LOGGER = LoggerFactory.getLogger("Baked BE Compiler");
		private static final boolean LOG_EXCEPTION = !Boolean.getBoolean("glowcase.debug.no_section_exception");
		private static final AABB BOUNDING_BOX = RenderSectionPos.ORIGIN_BOUNDING_BOX.deflate(0.01);
		public static final int CUBE_COLOR = ARGB.colorFromFloat(0.375F, 1.0F, 0.0F, 0.0F);
		public static final int LINE_COLOR = ARGB.colorFromFloat(0.75F, 1.0F, 0.0F, 0.0F);

		public BakedMeshes build(VertexSorting sorting) {
			renderCube(BOUNDING_BOX, CUBE_COLOR);

			try {
				return new BakedMeshes(bufferSource.buildAllBatches(sorting), true);
			} catch (MeshTooComplex e) {
				throw new IllegalStateException("Failed to create fallback mesh", e);
			}
		}

		private void renderCube(AABB aabb, int color) {
			final VertexConsumer boxBuilder = bufferSource.getBuffer(RenderTypes.debugFilledBox());

			double x0 = aabb.minX;
			double y0 = aabb.minY;
			double z0 = aabb.minZ;
			double x1 = aabb.maxX;
			double y1 = aabb.maxY;
			double z1 = aabb.maxZ;

			// Draws an inverted box, has better visibility of the outline
			renderQuad(boxBuilder, new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), new Vec3(x1, y1, z0), color);
			renderQuad(boxBuilder, new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), new Vec3(x0, y1, z1), new Vec3(x0, y0, z1), color);
			renderQuad(boxBuilder, new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), new Vec3(x0, y1, z0), color);
			renderQuad(boxBuilder, new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), new Vec3(x1, y1, z1), new Vec3(x1, y0, z1), color);
			renderQuad(boxBuilder, new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), color);
			renderQuad(boxBuilder, new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y0, z0), color);
		}

		private void renderQuad(VertexConsumer builder, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
			builder.addVertex((float) a.x(), (float) a.y(), (float) a.z()).setColor(color);
			builder.addVertex((float) b.x(), (float) b.y(), (float) b.z()).setColor(color);
			builder.addVertex((float) c.x(), (float) c.y(), (float) c.z()).setColor(color);
			builder.addVertex((float) d.x(), (float) d.y(), (float) d.z()).setColor(color);
		}

		private static void logError(Throwable error, long sectionNode) {
			RenderSectionPos sectionPos = new RenderSectionPos(sectionNode);
			if (LOG_EXCEPTION) {
				LOGGER.error("Failed to compile meshes for section [{}] centered at [{}]", sectionPos.toShortString(), sectionPos.center().toShortString(), error);
			} else {
				LOGGER.error("Failed to compile meshes for section [{}] centered at [{}]", sectionPos.toShortString(), sectionPos.center().toShortString());
			}
		}
	}
}
