package dev.hephaestus.glowcase.client.render.bakedbe;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import dev.hephaestus.glowcase.mixin.client.bakedbe.FeatureRenderDispatcher$PreparedFrameAccessor;
import dev.hephaestus.glowcase.mixin.client.bakedbe.FeatureRenderDispatcher$PreparedGroupAccessor;
import dev.hephaestus.glowcase.mixin.client.bakedbe.FeatureRenderDispatcherAccessor;
import dev.hephaestus.glowcase.mixin.client.bakedbe.FeatureRendererMapAccessor;
import dev.hephaestus.glowcase.mixin.client.bakedbe.StagedVertexBuffer$DrawAccessor;
import dev.hephaestus.glowcase.mixin.client.bakedbe.StagedVertexBufferAccessor;
import dev.hephaestus.glowcase.mixinsupport.BakeryDrawInfo;
import dev.hephaestus.glowcase.mixinsupport.RenderTypeInfoForBaking;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CachedFrame {
	public final List<Identity> identity;
	public final RenderSectionPos sectionPos;
	private final List<SubmitNodeCollection> submitsPerOrder = new ObjectArrayList<>();
	private final Map<FeatureRenderPhase<?>, List<FeatureRenderDispatcher.PreparedGroup<?>>> groupsByPhase = new IdentityHashMap<>();
	private final FeatureRendererMap featureRenderers = new FeatureRendererMap();
	private final TranslucentFeatureRenderPhase seeThrough;
	private final List<SubmitNode> allSubmits;
	private final FeatureFrameContext context;
	public final GpuBuffers buffers;
	private RenderOffset offset;

	public CachedFrame(
		List<Identity> identity,
		RenderSectionPos sectionPos,
		FeatureRenderDispatcher.PreparedFrame frame,
		GpuBuffers buffers
	) {
		this.identity = identity;
		this.sectionPos = sectionPos;
		this.buffers = buffers;

		var frameAccessor = (FeatureRenderDispatcher$PreparedFrameAccessor) frame;
		var submitNodeStorage = Objects.requireNonNull(frameAccessor.getSubmitNodeStorage());
		this.submitsPerOrder.addAll(submitNodeStorage.getSubmitsPerOrder().values());
		this.seeThrough = submitNodeStorage.seeThrough();
		this.allSubmits = new ArrayList<>(frameAccessor.getAllSubmits());

		var frameContext = Objects.requireNonNull(frameAccessor.getContext());
		var stagedBuffer = new StagedVertexBuffer(() -> "Cached frame dummy buffer", 0);
		var stagedBufferAccessor = (StagedVertexBufferAccessor) stagedBuffer;
		stagedBufferAccessor.setCurrentVertexBuffer(buffers.vertex());
		stagedBufferAccessor.setCurrentIndexBuffer(buffers.index());
		if (buffers.hasIndex()) {
			stagedBufferAccessor.setDraws(buffers.draws());
		}

		frameAccessor.getGroupsByPhase().forEach((phase, groups) -> groupsByPhase.put(phase, new ArrayList<>(groups)));

		this.context = new FeatureFrameContext(
			frameContext.options(),
			frameContext.font(),
			frameContext.blockStateModelSet(),
			frameContext.blockColors(),
			frameContext.textureManager(),
			frameContext.atlasManager(),
			frameContext.lightmap(),
			stagedBuffer
		);

		FeatureRendererMap frameFeatureRenderers = ((FeatureRenderDispatcherAccessor) frameAccessor.getThis$0()).getFeatureRenderers();
		FeatureRenderer<?>[] frameRenderers = ((FeatureRendererMapAccessor) frameFeatureRenderers).getRenderers();
		FeatureRenderer<?>[] renderers = new FeatureRenderer[frameRenderers.length];
		((FeatureRendererMapAccessor) this.featureRenderers).setRenderers(renderers);

		for (int i = 0; i < frameRenderers.length; i++) {
			FeatureRenderer<?> renderer = frameRenderers[i];
			if (!(renderer instanceof RenderTypeFeatureRenderer<?> renderTypeRenderer)) continue;

			renderers[i] = new BakeryRenderTypeFeatureRenderer(renderTypeRenderer.groups);
		}
	}

	public void updateCameraPos(Vec3 cameraPos) {
		if (offset != null && cameraPos.equals(offset.cameraPos())) return;

		var renderOffset = new Vec3(sectionPos.origin()).subtract(cameraPos);
		this.offset = new RenderOffset(cameraPos, renderOffset);
	}

	public void prepare(GpuBufferSlice fogOffset, List<DynamicGpuData.Transform> transforms, List<List<RenderTypeInfoForBaking>> infosForBaking) {
		for (FeatureRenderer<?> renderer : this.featureRenderers.values()) {
			((BakeryRenderTypeFeatureRenderer<?>) renderer).prepare(this.offset, fogOffset, transforms, infosForBaking);
		}
	}

	public RenderOffset getOffset() {
		return offset;
	}

	public void resort(Vec3 cameraPos, StagedVertexBuffer vanillaVertexBuffer) {
		if (!buffers.hasIndex()) return;

		var vanillaBuffer = (StagedVertexBufferAccessor) vanillaVertexBuffer;
		var draws = ((StagedVertexBufferAccessor) this.context.stagedVertexBuffer()).getDraws();

		int indexBufferSize = 0;
		for (StagedVertexBuffer.Draw draw : draws) {
			MeshData.SortState sortState = ((BakeryDrawInfo) draw).glowcase$getSortState();
			if (sortState == null) continue;
			StagedVertexBuffer$DrawAccessor drawAccessor = (StagedVertexBuffer$DrawAccessor) draw;
			indexBufferSize = drawAccessor.getIndexOffset() + drawAccessor.getIndexCount() * drawAccessor.callIndexType().bytes;
		}

		if (indexBufferSize == 0) return;

		GpuDevice device = RenderSystem.getDevice();
		CommandEncoder commandEncoder = device.createCommandEncoder();

		var relativePos = cameraPos.subtract(new Vec3(sectionPos.origin())).toVector3f();
		VertexSorting vertexSorting = VertexSorting.byDistance(relativePos);

		var stagingBuffer = vanillaBuffer.getStagingGpuBufferPool().acquire(device, indexBufferSize);
		try (GpuBufferSlice.MappedView view = stagingBuffer.slice().map(false, true)) {
			ByteBuffer buffer = view.data();

			for (StagedVertexBuffer.Draw draw : draws) {
				int indexOffset = ((StagedVertexBuffer$DrawAccessor) draw).getIndexOffset();
				buffer.position(indexOffset);
				MeshData.SortState sortState = ((BakeryDrawInfo) draw).glowcase$getSortState();
				if (sortState != null) {
					sortState.writeSortedIndexBuffer(buffer, vertexSorting);
				}
			}
		}

		assert buffers.index() != null;
		commandEncoder.copyToBuffer(stagingBuffer.slice(0, indexBufferSize), buffers.index().slice(0, indexBufferSize));

	}

	public void executeSolid(final RenderPass renderPass) {
		FeatureFrameContext context = Objects.requireNonNull(this.context);

		for (SubmitNodeCollection collection : submitsPerOrder) {
			this.executePhase(collection.solid, context, renderPass);
		}
	}

	public void executeTranslucent(final RenderPass renderPass) {
		FeatureFrameContext context = Objects.requireNonNull(this.context);

		for (SubmitNodeCollection collection : submitsPerOrder) {
			this.executePhase(collection.shadows, context, renderPass);
			this.executePhase(collection.translucentModels, context, renderPass);
			this.executePhase(collection.nameTags, context, renderPass);
			this.executePhase(collection.texts, context, renderPass);
			this.executePhase(collection.translucentCustomGeometry, context, renderPass);
		}

		for (SubmitNodeCollection collection : submitsPerOrder) {
			this.executePhase(collection.shapeOutlines, context, renderPass);
			this.executePhase(collection.translucentGizmos, context, renderPass);
		}

		for (SubmitNodeCollection collection : submitsPerOrder) {
			this.executePhase(collection.translucentBlocksAndItems, context, renderPass);
			this.executePhase(collection.breakingOverlay, context, renderPass);
			this.executePhase(collection.waterMask, context, renderPass);
		}
	}

	public void executeWaterMask(final RenderPass renderPass) {
		FeatureFrameContext context = Objects.requireNonNull(this.context);

		for (SubmitNodeCollection collection : submitsPerOrder) {
			this.executePhase(collection.waterMask, context, renderPass);
		}
	}

	public void executeOit(final OitStage stage, final RenderPass renderPass) {
		FeatureFrameContext context = Objects.requireNonNull(this.context);

		for (SubmitNodeCollection collection : submitsPerOrder) {
			this.executePhase(collection.oitTranslucent, context, stage, renderPass);
		}
	}

	public void executeOutline(final RenderPass renderPass) {
		FeatureFrameContext context = Objects.requireNonNull(this.context);

		for (SubmitNodeCollection collection : submitsPerOrder) {
			this.executePhase(collection.outline, context, renderPass);
		}
	}

	public void executeTranslucentAfterTerrain(final RenderPass renderPass) {
		FeatureFrameContext context = Objects.requireNonNull(this.context);

		for (SubmitNodeCollection collection : submitsPerOrder) {
			this.executePhase(collection.afterTerrain, context, renderPass);
		}
	}

	public void executeSeeThrough(final RenderPass renderPass) {
		FeatureFrameContext context = Objects.requireNonNull(this.context);
		this.executePhase(this.seeThrough, context, renderPass);
	}

	public void executeAlwaysOnTop(final RenderPass renderPass) {
		FeatureFrameContext context = Objects.requireNonNull(this.context);

		for (SubmitNodeCollection collection : submitsPerOrder) {
			this.executePhase(collection.alwaysOnTopGizmos, context, renderPass);
		}
	}

	private void executePhase(final FeatureRenderPhase<?> phase, final FeatureFrameContext context, final RenderPass renderPass) {
		this.executePhase(phase, context, null, renderPass);
	}

	private void executePhase(final FeatureRenderPhase<?> phase, final FeatureFrameContext context, @Nullable final OitStage stage, final RenderPass renderPass) {
		ProfilerFiller profiler = Profiler.get();

		for (FeatureRenderDispatcher.PreparedGroup<?> group : this.groupsByPhase.getOrDefault(phase, List.of())) {
			var accessor = (FeatureRenderDispatcher$PreparedGroupAccessor<?>) group;

			String featureTypeName = accessor.getFeatureType().toString();
			profiler.push("Baked BE " + featureTypeName);
			renderPass.pushDebugGroup(() -> "Baked BE " + featureTypeName);
			group.execute(context, stage, renderPass, this.featureRenderers, this.allSubmits);
			renderPass.popDebugGroup();
			profiler.pop();
		}
	}
}
