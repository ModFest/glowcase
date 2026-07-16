package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedBERenderDispatcher;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedMeshes;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedRendererUtil;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.render.bakedbe.section.GlowcaseSectionRenderDispatcher;
import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh;
import dev.hephaestus.glowcase.client.render.block.entity.BakedBlockEntityRenderer;
import dev.hephaestus.glowcase.mixinsupport.BakingBlockEntityRenderDispatcher;
import dev.hephaestus.glowcase.mixinsupport.BakingRendererExtension;
import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("FieldMayBeFinal")
@Environment(EnvType.CLIENT)
@Mixin(ChunkBuilderMeshingTask.class)
public abstract class ChunkBuilderMeshingTaskMixin extends ChunkBuilderTask<ChunkBuildOutput> {
	@Unique private @Final BakingBlockEntityRenderDispatcher bakingBlockEntityRenderer;

	public ChunkBuilderMeshingTaskMixin(RenderSection render, int time, Vector3dc absoluteCameraPos) {
		super(render, time, absoluteCameraPos);
	}

	{
		this.bakingBlockEntityRenderer = (BakingBlockEntityRenderDispatcher) Minecraft.getInstance().getBlockEntityRenderDispatcher();
	}

	@Inject(at = @At("HEAD"), method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;")
	private void init(
		CallbackInfoReturnable<SectionCompiler.Results> cir,
		@Share("poseStack") LocalRef<PoseStack> poseStackRef,
		@Share("vertexSorting") LocalRef<VertexSorting> vertexSortingRef
	) {
		poseStackRef.set(new PoseStack());
		vertexSortingRef.set(GlowcaseSectionRenderDispatcher.createVertexSorting(cameraPos));
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", ordinal = 0), method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;")
	private void lateValues(
		CallbackInfoReturnable<ChunkBuildOutput> cir,
		@Local(name = "cache") BlockRenderCache cache,
		@Share("lightCache") LocalRef<LightDataAccess> lightCache
	) {
		lightCache.set(((BlockRenderCacheAccessor) cache).getLightDataCache());
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/data/BuiltSectionInfo$Builder;addBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Z)V"), method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;")
	private <E extends BlockEntity, B extends BlockEntityRenderState> void submitBakedRenderers(
		ChunkBuildContext buildContext,
		CancellationToken cancellationToken,
		CallbackInfoReturnable<ChunkBuildOutput> cir,
		@Local(name = "profiler") ProfilerFiller profiler,
		@Local(name = "blockPos") BlockPos.MutableBlockPos blockPos,
		@Local(name = "blockState") BlockState blockState,
		@Local(name = "entity") E entity,
		@Local(name = "renderer") BlockEntityRenderer<E, ?> renderer,
		@Share("poseStack") LocalRef<PoseStack> poseStackRef,
		@Share("lightCache") LocalRef<LightDataAccess> lightCache,
		@Share("nodeStorage") LocalRef<SubmitNodeStorage> nodeStorageRef
	) {
		profiler.push("glowcase:baked_be/submit");
		B renderState = null;
		try {
			BakingRendererExtension rendererExtension = (BakingRendererExtension) renderer;
			if (rendererExtension != null && rendererExtension.glowcase$isBakingRenderer()) {
				BakedBlockEntityRenderer<E, ?, B> bakedRenderer = (BakedBlockEntityRenderer<E, ?, B>) renderer;
				renderState = bakingBlockEntityRenderer.glowcase$tryExtractBakingRenderState(
					entity,
					// Sodium's lightCache is in a Sodium-specific format that's ill-suited for here.
					LevelRenderer.getLightCoords(
						LevelRenderer.BrightnessGetter.DEFAULT,
						lightCache.get().getLevel(),
						blockState,
						blockPos
					)
				);
				if (renderState != null) {
					SubmitNodeStorage nodeStorage = nodeStorageRef.get();
					if (nodeStorage == null) {
						nodeStorageRef.set(nodeStorage = BakedBERenderDispatcher.getNodeStorage());
					}

					BakedRendererUtil.submitForBaking(bakedRenderer, blockPos, renderState, poseStackRef.get(), nodeStorage);
				}
			}
		} catch (Exception e) {
			CrashReport report = CrashReport.forThrowable(e, "Submitting baked Block Entity in world");
			CrashReportCategory category = report.addCategory("Block Entity details");
			category.setDetail("BlockEntity", entity.getClass().getCanonicalName());
			if (renderState != null) {
				renderState.fillCrashReportCategory(category);
			} else {
				category.setDetail("BlockEntityRenderState", "None");
				category.setDetail("Position", blockPos);
				category.setDetail("Block state", blockState::toString);
			}
			throw new ReportedException(report);
		} finally {
			profiler.pop();
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/data/BuiltSectionInfo$Builder;setOcclusionData([Lnet/minecraft/client/renderer/chunk/VisibilitySet;)V", shift = At.Shift.AFTER), method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;")
	private void queueCompilation(
		CallbackInfoReturnable<SectionCompiler.Results> cir,
		@Share("nodeStorage") LocalRef<SubmitNodeStorage> nodeStorageRef,
		@Share("vertexSorting") LocalRef<VertexSorting> vertexSortingRef,
		@Share("bakedMeshes") LocalRef<BakedMeshes> bakedMeshes
	) {
		bakedMeshes.set(GlowcaseLevelRenderer.getInstance().updateAndCompile(this.section.getPosition().asLong(), nodeStorageRef.get(), vertexSortingRef.get()));
	}

	@Expression("return null")
	@Inject(at = @At(value = "MIXINEXTRAS:EXPRESSION"), method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;")
	private void releaseNodeStorage(CallbackInfoReturnable<SectionCompiler.Results> cir, @Share("nodeStorage") LocalRef<SubmitNodeStorage> nodeStorageRef) {
		if (nodeStorageRef.get() == null) return;

		BakedBERenderDispatcher.returnNodeStorage(nodeStorageRef.get());
	}

	@Inject(at = @At("TAIL"), method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;", cancellable = true)
	private void allocateBuffers(
		ChunkBuildContext buildContext,
		CancellationToken cancellationToken,
		CallbackInfoReturnable<ChunkBuildOutput> cir,
		@Local(name = "output") ChunkBuildOutput output,
		@Share("bakedMeshes") LocalRef<BakedMeshes> bakedMeshesRef
	) {
		GlowcaseLevelRenderer levelRenderer = GlowcaseLevelRenderer.getInstance();
		BakedMeshes meshes = bakedMeshesRef.get();
		long sectionNode = this.section.getPosition().asLong();
		if (meshes == null) {
			levelRenderer.releaseSection(sectionNode);
			return;
		}

		GlowcaseSectionRenderDispatcher renderDispatcher = levelRenderer.getSectionRenderDispatcher();
		if (renderDispatcher == null) {
			meshes.close();
			return;
		}

		GlowcaseSectionRenderDispatcher.UberBufferCallbacks callbacks = new GlowcaseSectionRenderDispatcher.UberBufferCallbacks(levelRenderer.visibleSections(), meshes);
		for (BakedMeshes.Entry entry : meshes) {
			CompiledMesh mesh = entry.mesh();
			boolean success = false;

			while (!success) {
				if (cancellationToken.isCancelled()) {
					meshes.close();
					output.destroy();
					cir.setReturnValue(null);
					return;
				}

				success = renderDispatcher.allocateMeshBuffers(sectionNode, entry.renderType(), mesh.meshData(), callbacks);

				if (!success && !RenderSystem.isOnRenderThread()) {
					Thread.onSpinWait();
				}
			}

			mesh.close();
		}

		meshes.finish();
	}
}
