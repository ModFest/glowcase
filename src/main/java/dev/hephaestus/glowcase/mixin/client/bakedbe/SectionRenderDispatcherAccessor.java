package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.GlowcaseRenderSectionInfo;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.mixinsupport.CompileTaskFields;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(SectionRenderDispatcher.class)
public interface SectionRenderDispatcherAccessor {
	@Accessor LevelRenderer getRenderer();
	@Accessor AtomicReference<Vec3> getCameraPosition();

	@Mixin(SectionRenderDispatcher.RenderSection.class)
	interface RenderSectionAccessor {
		@Accessor SectionRenderDispatcher getThis$0();
		@Invoker VertexSorting callCreateVertexSorting(final SectionPos sectionPos, final Vec3 cameraPos);
	}

	@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$ResortTransparencyTask")
	abstract class ResortTransparencyTaskMixin implements CompileTaskFields {
		@Shadow @Final SectionRenderDispatcher.RenderSection this$1;
		@Unique SectionRenderDispatcher this$0 = ((RenderSectionAccessor) this$1).getThis$0();
		@Unique GlowcaseLevelRenderer levelRenderer = GlowcaseLevelRenderer.getInstance(((SectionRenderDispatcherAccessor) this$0).getRenderer());

		@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/CompiledSectionMesh;getTransparencyState()Lcom/mojang/blaze3d/vertex/MeshData$SortState;"), method = "doTask")
		private void handleResort(SectionBufferBuilderPack buffers, CallbackInfoReturnable<SectionRenderDispatcher.RenderSection.CompileTask.SectionTaskResult> cir) {
			final long sectionNode = this$1.getSectionNode();
			if (!levelRenderer.visibleSections().isVisible(sectionNode)) return;
			Vec3 cameraPos = ((SectionRenderDispatcherAccessor) this$0).getCameraPosition().get();
			VertexSorting vertexSorting = ((RenderSectionAccessor) this$1).callCreateVertexSorting(SectionPos.of(sectionNode), cameraPos);

			GlowcaseRenderSectionInfo sectionInfo = levelRenderer.visibleSections().get(sectionNode);
			for (GlowcaseRenderSectionInfo.Entry entry : sectionInfo) {
				final var sortState = entry.sortState();
				if (sortState == null) continue;

				ByteBufferBuilder.Result indexBuffer = sortState.buildSortedIndexBuffer(buffers.buffer(ChunkSectionLayer.TRANSLUCENT), vertexSorting);
				if (indexBuffer == null) return;

				levelRenderer.updateIndexBuffer(sectionNode, entry.renderType(), indexBuffer.byteBuffer());
				indexBuffer.close();
			}
		}
	}
}
