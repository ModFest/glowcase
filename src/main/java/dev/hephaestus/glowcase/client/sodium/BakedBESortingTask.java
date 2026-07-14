package dev.hephaestus.glowcase.client.sodium;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.hephaestus.glowcase.client.render.bakedbe.section.GlowcaseRenderSectionInfo;
import dev.hephaestus.glowcase.client.render.bakedbe.section.GlowcaseSectionRenderDispatcher;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.util.collections.Pool;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation.MeshTaskSizeEstimator;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Unique;

public class BakedBESortingTask extends ChunkBuilderTask<BakedBESortingTask.Output> {
	// If this runs out, something is horribly wrong, this is acquired and reclaimed in the same task, and can't possibly have more than the available threads acquiring it
	public static final @Unique Pool<ByteBufferBuilder> indexBufferPool = new Pool<>(
		() -> new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE),
		ByteBufferBuilder::close,
		Runtime.getRuntime().availableProcessors(),
		true
	);
	private final GlowcaseRenderSectionInfo sectionInfo;
	private final GlowcaseLevelRenderer levelRenderer;
	private final long sectionNode;

	public BakedBESortingTask(RenderSection section, int time, Vector3dc absoluteCameraPos, GlowcaseRenderSectionInfo sectionInfo) {
		super(section, time, absoluteCameraPos);
		this.levelRenderer = GlowcaseLevelRenderer.getInstance();
		this.sectionInfo = sectionInfo;
		this.sectionNode = this.section.getPosition().asLong();
	}

	@Override
	public Output execute(ChunkBuildContext context, CancellationToken cancellationToken) {
		if (cancellationToken.isCancelled() || isGone()) return null;

		GlowcaseSectionRenderDispatcher renderDispatcher = this.levelRenderer.getSectionRenderDispatcher();
		if (renderDispatcher == null) return null;

		ProfilerFiller profiler = Profiler.get();
		profiler.push("glowcase:bbe_translucency_sort");
		VertexSorting vertexSorting = GlowcaseSectionRenderDispatcher.createVertexSorting(this.getRelativeCameraPos());
		ByteBufferBuilder bufferBuilder = indexBufferPool.acquire();

		int size = 0;
		for (GlowcaseRenderSectionInfo.DrawEntry entry : this.sectionInfo) {
			final var sorter = entry.sorter();
			if (sorter == null) continue;

			ByteBufferBuilder.Result indexBuffer = sorter.buildSortedIndexBuffer(bufferBuilder, vertexSorting);
			if (indexBuffer == null) continue;

			boolean success = false;

			while (!success) {
				if (cancellationToken.isCancelled()) {
					indexBuffer.close();
					bufferBuilder.clear();
					indexBufferPool.release(bufferBuilder);
					profiler.pop();
					return null;
				}

				success = renderDispatcher.allocateIndexBuffers(this.sectionNode, entry.renderType(), indexBuffer.byteBuffer());

				if (!success && !RenderSystem.isOnRenderThread()) {
					Thread.onSpinWait();
				}
			}

			size += indexBuffer.byteBuffer().capacity();
			indexBuffer.close();
		}

		this.sectionInfo.setTranslucencyPointOfView(this.absoluteCameraPos.get(new Vector3f()), this.sectionNode);

		// Only clear after. There is no need to do it every iteration, it has 4GiB of capacity.
		// If this runs out, there is a much bigger problem to be solved.
		bufferBuilder.clear();
		indexBufferPool.release(bufferBuilder);
		profiler.pop();

		return new Output(this.section, this.submitTime, size);
	}

	@Override
	public long estimateTaskSizeWith(MeshTaskSizeEstimator estimator) {
		if (this.isGone()) return 0;

		int size = 0;
		for (GlowcaseRenderSectionInfo.DrawEntry entry : this.sectionInfo) {
			size += TranslucentData.quadCountToIndexBytes(entry.indexCount());
		}

		return size;
	}

	private boolean isGone() {
		return !this.levelRenderer.visibleSections().isVisible(this.sectionNode);
	}

	public static BakedBESortingTask create(RenderSection section, int frame, Vector3dc absoluteCameraPos) {
		var visibleSections = GlowcaseLevelRenderer.getInstance().visibleSections();
		if (!visibleSections.isVisible(section.getPosition())) return null;

		return new BakedBESortingTask(section, frame, absoluteCameraPos, visibleSections.get(section.getPosition()));
	}

	public static class Output extends BuilderTaskOutput {
		private final int size;
		public Output(RenderSection render, int buildTime, int size) {
			super(render, buildTime);
			this.size = size;
		}

		@Override
		protected long calculateResultSize() {
			return this.size;
		}
	}
}
