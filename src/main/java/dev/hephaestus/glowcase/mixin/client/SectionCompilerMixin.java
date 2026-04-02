package dev.hephaestus.glowcase.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedRendererUtil;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.SectionCompileQueue;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.BakedBlockEntityRenderer;
import dev.hephaestus.glowcase.mixinsupport.BakedBECompilerResultExtension;
import dev.hephaestus.glowcase.mixinsupport.BakingBlockEntityRenderDispatcher;
import dev.hephaestus.glowcase.mixinsupport.BakingRendererExtension;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NullUnmarked;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("FieldMayBeFinal")
@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {
	@Shadow @Final private BlockEntityRenderDispatcher blockEntityRenderer;
	@Unique private @Final BakingBlockEntityRenderDispatcher bakingBlockEntityRenderer;

	{
		this.bakingBlockEntityRenderer = (BakingBlockEntityRenderDispatcher) this.blockEntityRenderer;
	}

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
		}

		profiler.pop();
	}

	@Inject(at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"), method = "compile")
	private void queueCompilation(
		CallbackInfoReturnable<SectionCompiler.Results> cir,
		@Local(argsOnly = true, name = "sectionPos") SectionPos sectionPos,
		@Share("nodeStorage") LocalRef<SubmitNodeStorage> nodeStorageRef
	) {
		GlowcaseLevelRenderer.getInstance().queueCompilation(sectionPos.asLong(), nodeStorageRef.get());
	}
}
