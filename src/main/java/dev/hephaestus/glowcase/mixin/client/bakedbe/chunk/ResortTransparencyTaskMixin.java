package dev.hephaestus.glowcase.mixin.client.bakedbe.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.hephaestus.glowcase.client.render.bakedbe.section.GlowcaseRenderSectionInfo;
import dev.hephaestus.glowcase.client.render.bakedbe.section.GlowcaseSectionRenderDispatcher;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$ResortTransparencyTask")
public abstract class ResortTransparencyTaskMixin implements CompileTaskAccessor {
	@Shadow @Final SectionRenderDispatcher.RenderSection this$1;
	@Unique SectionRenderDispatcher this$0 = ((RenderSectionAccessor) this$1).getThis$0();
	@Unique GlowcaseLevelRenderer levelRenderer = GlowcaseLevelRenderer.getInstance(((SectionRenderDispatcherAccessor) this$0).getRenderer());

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/CompiledSectionMesh;getTransparencyState()Lcom/mojang/blaze3d/vertex/MeshData$SortState;"), method = "doTask", cancellable = true)
	private void handleResort(SectionBufferBuilderPack buffers, CallbackInfoReturnable<SectionRenderDispatcher.RenderSection.CompileTask.SectionTaskResult> cir) {
		final long sectionNode = this$1.getSectionNode();
		if (!levelRenderer.visibleSections().isVisible(sectionNode)) return;
		Vec3 cameraPos = ((SectionRenderDispatcherAccessor) this$0).getCameraPosition().get();
		VertexSorting vertexSorting = GlowcaseSectionRenderDispatcher.createVertexSorting(SectionPos.of(sectionNode), cameraPos);

		AtomicBoolean isCancelled = this.getIsCancelled();
		GlowcaseSectionRenderDispatcher renderDispatcher = levelRenderer.getSectionRenderDispatcher();
		if (renderDispatcher == null) return;

		GlowcaseRenderSectionInfo sectionInfo = levelRenderer.visibleSections().get(sectionNode);
		for (GlowcaseRenderSectionInfo.DrawEntry entry : sectionInfo) {
			final var sorter = entry.sorter();
			if (sorter == null) continue;

			ByteBufferBuilder.Result indexBuffer = sorter.buildSortedIndexBuffer(buffers.buffer(ChunkSectionLayer.TRANSLUCENT), vertexSorting);
			if (indexBuffer == null) continue;

			boolean success = false;

			while (!success) {
				if (isCancelled.get()) {
					indexBuffer.close();
					cir.setReturnValue(SectionRenderDispatcher.RenderSection.CompileTask.SectionTaskResult.CANCELLED);
					return;
				}

				success = renderDispatcher.allocateIndexBuffers(sectionNode, entry.renderType(), indexBuffer.byteBuffer());

				if (!success && !RenderSystem.isOnRenderThread()) {
					Thread.onSpinWait();
				}
			}

			indexBuffer.close();
		}
	}
}
