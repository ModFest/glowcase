package dev.hephaestus.glowcase.mixin.client.bakedbe.chunk;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedRendererUtil;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.SectionCompileQueue;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.BakedBlockEntityRenderer;
import dev.hephaestus.glowcase.mixinsupport.BakingBlockEntityRenderDispatcher;
import dev.hephaestus.glowcase.mixinsupport.BakingRendererExtension;
import dev.hephaestus.glowcase.mixinsupport.ExtendedResults;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@SuppressWarnings("FieldMayBeFinal")
@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {
	@Shadow @Final private BlockEntityRenderDispatcher blockEntityRenderer;

	@Inject(at = @At("HEAD"), method = "compile")
	private void createPoseStack(
		CallbackInfoReturnable<SectionCompiler.Results> cir,
		@Share("poseStack") LocalRef<PoseStack> poseStack,
		@Share("profiler") LocalRef<ProfilerFiller> profiler
	) {
		profiler.set(Profiler.get());
		poseStack.set(new PoseStack());
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionCompiler;handleBlockEntity(Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"), method = "compile")
	private <E extends BlockEntity, B extends BlockEntityRenderState> void submitBakedRenderers(
		SectionPos sectionPos,
		RenderSectionRegion region,
		VertexSorting vertexSorting,
		SectionBufferBuilderPack builders,
		CallbackInfoReturnable<SectionCompiler.Results> cir,
		@Local(name = "blockEntity") E blockEntity,
		@Local(name = "blockState") BlockState blockState,
		@Local(name = "pos") BlockPos pos,
		@Share("poseStack") LocalRef<PoseStack> poseStackRef,
		@Share("profiler") LocalRef<ProfilerFiller> profilerRef,
		@Share("nodeStorage") LocalRef<SubmitNodeStorage> nodeStorageRef
	) {
		ProfilerFiller profiler = profilerRef.get();
		profiler.push("glowcase:baked_be/submit");

		B renderState = null;
		try {
			BakingBlockEntityRenderDispatcher bakingBlockEntityRenderer = (BakingBlockEntityRenderDispatcher) this.blockEntityRenderer;

			BlockEntityRenderer<E, B> baseRenderer = this.blockEntityRenderer.getRenderer(blockEntity);
			BakingRendererExtension rendererExtension = (BakingRendererExtension) baseRenderer;

			if (rendererExtension != null && rendererExtension.glowcase$isBakingRenderer()) {
				BakedBlockEntityRenderer<E, ?, B> renderer = (BakedBlockEntityRenderer<E, ?, B>) baseRenderer;
				renderState = bakingBlockEntityRenderer.glowcase$tryExtractBakingRenderState(blockEntity);
				if (renderState != null) {
					SubmitNodeStorage nodeStorage = nodeStorageRef.get();
					if (nodeStorage == null) {
						nodeStorageRef.set(nodeStorage = SectionCompileQueue.getNodeStorage());
					}

					BakedRendererUtil.submitForBaking(renderer, pos, renderState, poseStackRef.get(), nodeStorage);
				}
			}
		} catch (Exception e) {
			CrashReport report = CrashReport.forThrowable(e, "Submitting baked Block Entity in world");
			CrashReportCategory category = report.addCategory("Block Entity details");
			category.setDetail("BlockEntity", blockEntity.getClass().getCanonicalName());
			if (renderState != null) {
				renderState.fillCrashReportCategory(category);
			} else {
				category.setDetail("BlockEntityRenderState", "None");
				category.setDetail("Position", pos);
				category.setDetail("Block state", blockState::toString);
			}
			throw new ReportedException(report);
		} finally {
			profiler.pop();
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"), method = "compile")
	private void queueCompilation(
		CallbackInfoReturnable<SectionCompiler.Results> cir,
		@Local(argsOnly = true, name = "sectionPos") SectionPos sectionPos,
		@Local(argsOnly = true, name = "vertexSorting") VertexSorting vertexSorting,
		@Local(name = "results") SectionCompiler.Results results,
		@Share("nodeStorage") LocalRef<SubmitNodeStorage> nodeStorageRef
	) {
		if (nodeStorageRef.get() != null) ((ExtendedResults) (Object) results).glowcase$trickMinecraft();
		GlowcaseLevelRenderer.getInstance().queueCompilation(sectionPos.asLong(), nodeStorageRef.get(), vertexSorting);
	}

	@Mixin(SectionCompiler.Results.class)
	private static class ResultsMixin implements ExtendedResults {
		private @Unique boolean shouldTrickMinecraft = false;

		@Override
		public boolean glowcase$shouldTrickMinecraft() {
			return shouldTrickMinecraft;
		}

		@Override
		public void glowcase$trickMinecraft() {
			shouldTrickMinecraft = true;
		}
	}

	@Mixin(CompiledSectionMesh.class)
	private static class CompiledSectionMeshMixin {
		@Shadow @Final private Map<ChunkSectionLayer, SectionMesh.SectionDraw> draws;

		@Inject(at = @At("RETURN"), method = "<init>")
		private void trickMinecraftIntoResorting(TranslucencyPointOfView translucencyPointOfView, SectionCompiler.Results results, CallbackInfo ci) {
			if (!((ExtendedResults) (Object) results).glowcase$shouldTrickMinecraft()) return;

			if (!draws.containsKey(ChunkSectionLayer.TRANSLUCENT)) {
				// This should trick the containsKey but not cause issues as get returns null if the key is missing
				this.draws.put(ChunkSectionLayer.TRANSLUCENT, null);
			}
		}

		@WrapOperation(at = @At(value = "INVOKE", target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z"), method = "isEmpty")
		private boolean checkForNullValue(Map<ChunkSectionLayer, SectionMesh.SectionDraw> instance, Object key, Operation<Boolean> original) {
			return original.call(instance, key) && instance.get(key) != null;
		}
	}
}
