package dev.hephaestus.glowcase.mixin.client.bakedbe.chunk;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedMeshes;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.render.bakedbe.section.GlowcaseSectionRenderDispatcher;
import dev.hephaestus.glowcase.client.render.bakedbe.section.GlowcaseSectionRenderDispatcher.UberBufferCallbacks;
import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh;
import dev.hephaestus.glowcase.mixinsupport.ExtendedResults;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionCompiler.Results;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection.CompileTask.SectionTaskResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask")
public abstract class RebuildTaskMixin implements CompileTaskAccessor {
	@Shadow @Final RenderSection this$1;
	@Unique SectionRenderDispatcher this$0 = ((RenderSectionAccessor) this$1).getThis$0();
	private final @Unique GlowcaseLevelRenderer levelRenderer = GlowcaseLevelRenderer.getInstance(((SectionRenderDispatcherAccessor) this$0).getRenderer());

	@Definition(id = "CompiledSectionMesh", type = CompiledSectionMesh.class)
	@Expression("? = new CompiledSectionMesh(?, ?)")
	@Inject(at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER), method = "doTask", cancellable = true)
	private void handleResults(
		CallbackInfoReturnable<SectionTaskResult> cir,
		@Local(name = "results") Results results,
		@Local(name = "compiledSectionMesh") CompiledSectionMesh compiledSectionMesh
	) {
		ExtendedResults extendedResults = (ExtendedResults) (Object) results;
		BakedMeshes meshes = extendedResults.glowcase$getBakedMeshes();
		if (meshes == null || meshes.isEmpty()) {
			if (meshes != null) meshes.close();
			levelRenderer.releaseSection(this$1.getSectionNode());
			return;
		}

		AtomicBoolean isCancelled = this.getIsCancelled();
		GlowcaseSectionRenderDispatcher renderDispatcher = levelRenderer.getSectionRenderDispatcher();
		if (renderDispatcher == null) {
			meshes.close();
			return;
		}

		UberBufferCallbacks callbacks = new UberBufferCallbacks(levelRenderer.visibleSections(), meshes);
		for (BakedMeshes.Entry entry : meshes) {
			CompiledMesh mesh = entry.mesh();
			boolean success = false;

			while (!success) {
				if (isCancelled.get()) {
					meshes.close();
					results.release();

					this$0.lock();
					try {
						((RenderSectionAccessor) this$1).callReleaseSectionMesh(compiledSectionMesh);
					} finally {
						this$0.unlock();
					}
					cir.setReturnValue(SectionTaskResult.CANCELLED);
					return;
				}

				success = renderDispatcher.allocateMeshBuffers(this$1.getSectionNode(), entry.renderType(), mesh.meshData(), callbacks);

				if (!success && !RenderSystem.isOnRenderThread()) {
					Thread.onSpinWait();
				}
			}

			mesh.close();
		}

		meshes.finish();
	}
}
