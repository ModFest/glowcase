package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.hephaestus.glowcase.client.render.bakedbe.section.GlowcaseRenderSectionInfo;
import dev.hephaestus.glowcase.client.render.bakedbe.section.GlowcaseSectionRenderDispatcher;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.sodium.BakedBESortingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkSortOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderSortingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ChunkBuilderSortingTask.class)
public abstract class ChunkBuilderSortingTaskMixin extends ChunkBuilderTask<ChunkSortOutput> {
	public ChunkBuilderSortingTaskMixin(RenderSection render, int time, Vector3dc absoluteCameraPos) {
		super(render, time, absoluteCameraPos);
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/data/DynamicSorter;writeIndexBuffer(Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/data/CombinedCameraPos;)V"), method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkSortOutput;", cancellable = true)
	private void handleResorting(
		ChunkBuildContext context,
		CancellationToken cancellationToken,
		CallbackInfoReturnable<ChunkSortOutput> cir,
		@Local(name = "profiler") ProfilerFiller profiler
	) {
		GlowcaseLevelRenderer levelRenderer = GlowcaseLevelRenderer.getInstance();
		var visibleSections = levelRenderer.visibleSections();
		long sectionNode = this.section.getPosition().asLong();
		if (!visibleSections.isVisible(sectionNode)) return;
		profiler.push("baked_be");

		VertexSorting vertexSorting = GlowcaseSectionRenderDispatcher.createVertexSorting(this.getRelativeCameraPos());
		ByteBufferBuilder bufferBuilder = BakedBESortingTask.indexBufferPool.acquire();

		GlowcaseSectionRenderDispatcher renderDispatcher = levelRenderer.getSectionRenderDispatcher();
		if (renderDispatcher == null) return;

		GlowcaseRenderSectionInfo sectionInfo = visibleSections.get(sectionNode);
		for (GlowcaseRenderSectionInfo.DrawEntry entry : sectionInfo) {
			final var sorter = entry.sorter();
			if (sorter == null) continue;

			ByteBufferBuilder.Result indexBuffer = sorter.buildSortedIndexBuffer(bufferBuilder, vertexSorting);
			if (indexBuffer == null) continue;

			boolean success = false;

			while (!success) {
				if (cancellationToken.isCancelled()) {
					// In theory, this would never be reached - SkyNotTheLimit
					indexBuffer.close();
					bufferBuilder.clear();
					BakedBESortingTask.indexBufferPool.release(bufferBuilder);
					profiler.pop();

					cir.setReturnValue(null);
					return;
				}

				success = renderDispatcher.allocateIndexBuffers(sectionNode, entry.renderType(), indexBuffer.byteBuffer());

				if (!success && !RenderSystem.isOnRenderThread()) {
					Thread.onSpinWait();
				}
			}

			indexBuffer.close();
		}

		bufferBuilder.clear();
		BakedBESortingTask.indexBufferPool.release(bufferBuilder);
		profiler.pop();
	}
}
