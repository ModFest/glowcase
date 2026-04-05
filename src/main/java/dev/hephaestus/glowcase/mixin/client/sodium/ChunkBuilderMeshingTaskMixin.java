package dev.hephaestus.glowcase.mixin.client.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedRendererUtil;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.SectionCompileQueue;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.BakedBlockEntityRenderer;
import dev.hephaestus.glowcase.mixinsupport.BakingBlockEntityRenderDispatcher;
import dev.hephaestus.glowcase.mixinsupport.BakingRendererExtension;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings({"FieldMayBeFinal", "AmbiguousMixinReference"})
@Pseudo
@Mixin(ChunkBuilderMeshingTask.class)
public abstract class ChunkBuilderMeshingTaskMixin extends ChunkBuilderTask<ChunkBuildOutput> {
	@Unique private @Final BakingBlockEntityRenderDispatcher bakingBlockEntityRenderer;

	public ChunkBuilderMeshingTaskMixin(RenderSection render, int time, Vector3dc absoluteCameraPos) {
		super(render, time, absoluteCameraPos);
	}

	{
		this.bakingBlockEntityRenderer = (BakingBlockEntityRenderDispatcher) Minecraft.getInstance().getBlockEntityRenderDispatcher();
	}

	@Inject(at = @At("HEAD"), method = "execute")
	private void createValues(
		CallbackInfoReturnable<SectionCompiler.Results> cir,
		@Share("poseStack") LocalRef<PoseStack> poseStackRef
	) {
		poseStackRef.set(new PoseStack());
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/data/BuiltSectionInfo$Builder;addBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Z)V"), method = "execute")
	private <E extends BlockEntity, B extends BlockEntityRenderState> void submitBakedRenderers(
		ChunkBuildContext buildContext,
		CancellationToken cancellationToken,
		CallbackInfoReturnable<ChunkBuildOutput> cir,
		@Local(name = "entity") E blockEntity,
		@Local(name = "blockPos") BlockPos.MutableBlockPos blockPos,
		@Local(name = "blockState") BlockState blockState,
		@Local(name = "renderer") BlockEntityRenderer<E, ?> baseRenderer,
		@Local(name = "profiler") ProfilerFiller profiler,
		@Share("poseStack") LocalRef<PoseStack> poseStackRef,
		@Share("nodeStorage") LocalRef<SubmitNodeStorage> nodeStorageRef
	) {
		profiler.push("glowcase:baked_be/submit");
		B renderState = null;
		try {
			BakingRendererExtension rendererExtension = (BakingRendererExtension) baseRenderer;
			if (rendererExtension != null && rendererExtension.glowcase$isBakingRenderer()) {
				BakedBlockEntityRenderer<E, ?, B> renderer = (BakedBlockEntityRenderer<E, ?, B>) baseRenderer;
				renderState = bakingBlockEntityRenderer.glowcase$tryExtractBakingRenderState(blockEntity);
				if (renderState != null) {
					SubmitNodeStorage nodeStorage = nodeStorageRef.get();
					if (nodeStorage == null) {
						nodeStorageRef.set(nodeStorage = SectionCompileQueue.getNodeStorage());
					}

					BakedRendererUtil.submitForBaking(renderer, blockPos, renderState, poseStackRef.get(), nodeStorage);
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
				category.setDetail("Position", blockPos);
				category.setDetail("Block state", blockState::toString);
			}
			throw new ReportedException(report);
		}

		profiler.pop();
	}

	@Inject(at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Reference2ReferenceOpenHashMap;<init>()V"), method = "execute")
	private void queueCompilation(
		CallbackInfoReturnable<SectionCompiler.Results> cir,
		@Share("nodeStorage") LocalRef<SubmitNodeStorage> nodeStorageRef
	) {
		GlowcaseLevelRenderer.getInstance().queueCompilation(this.render.getPosition().asLong(), nodeStorageRef.get());
	}

	@Inject(at = @At(value = "RETURN"), slice = @Slice(to = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Reference2ReferenceOpenHashMap;<init>()V")), method = "execute")
	private void releaseNodeStorage(CallbackInfoReturnable<SectionCompiler.Results> cir, @Share("nodeStorage") LocalRef<SubmitNodeStorage> nodeStorageRef) {
		if (nodeStorageRef.get() == null) return;

		SectionCompileQueue.returnNodeStorage(nodeStorageRef.get());
	}
}
