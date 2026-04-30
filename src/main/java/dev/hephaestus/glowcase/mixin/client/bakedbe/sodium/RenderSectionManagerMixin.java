package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.sodium.BakedBESortingTask;
import dev.hephaestus.glowcase.mixinsupport.sodium.RenderSectionManagerExtension;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkUpdateTypes;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation.JobDurationEstimator;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation.MeshTaskSizeEstimator;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation.UploadDurationEstimator;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderSortingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(RenderSectionManager.class)
public abstract class RenderSectionManagerMixin implements RenderSectionManagerExtension {
	// The first 4 bits are used by sodium, the next 4 are untouched for safety
	private static final @Unique int BAKED_BE_SORT = 1 << 8;

	@Shadow private static @Final float NEARBY_SORT_DISTANCE;

	@Shadow private @Final JobDurationEstimator jobDurationEstimator;
	@Shadow private @Final MeshTaskSizeEstimator meshTaskSizeEstimator;
	@Shadow private @Final UploadDurationEstimator jobUploadDurationEstimator;
	@Shadow private @Final Long2ReferenceMap<RenderSection> sectionByPosition;
	@Shadow private @Nullable Vector3dc cameraPosition;
	@Shadow private int frame;

	@Shadow protected abstract boolean shouldPrioritizeTask(RenderSection section, float distance);
	@Shadow protected abstract boolean upgradePendingUpdate(RenderSection section, int updateType);

	@Inject(at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobResult;successfully(Ljava/lang/Object;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobResult;"), method = "submitSectionTask")
	private void clearEmptySection(CallbackInfo ci, @Local(argsOnly = true, name = "section") RenderSection section) {
		GlowcaseLevelRenderer.getInstance().updateAndCompile(section.getPosition().asLong(), null, null);
	}

	public void glowcase$scheduleBakedBESort(long sectionPos) {
		RenderSection section = this.sectionByPosition.get(sectionPos);

		if (section != null) {
			int pendingUpdate = BAKED_BE_SORT;
			if (this.shouldPrioritizeTask(section, NEARBY_SORT_DISTANCE)) {
				pendingUpdate = ChunkUpdateTypes.join(pendingUpdate, ChunkUpdateTypes.IMPORTANT);
			}

			this.upgradePendingUpdate(section, pendingUpdate);
		}
	}

	@WrapOperation(at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;createSortTask(Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;I)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/tasks/ChunkBuilderSortingTask;"), method = "submitSectionTask")
	private ChunkBuilderSortingTask checkIfBakedBEResort(
		RenderSectionManager instance,
		RenderSection render,
		int frame,
		Operation<ChunkBuilderSortingTask> original,
		@Local(argsOnly = true, name = "type") int type
	) {
		return ChunkUpdateTypes.isSort(type) ? original.call(instance, render, frame) : null;
	}

	@Definition(id = "createSortTask", method = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;createSortTask(Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;I)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/tasks/ChunkBuilderSortingTask;")
	@Expression("? = ?.createSortTask(?, ?)")
	@Inject(at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER), method = "submitSectionTask")
	private void createBakedBESortTask(
		CallbackInfo ci,
		@Local(argsOnly = true, name = "type") int type,
		@Local(argsOnly = true, name = "section") RenderSection section,
		@Local(name = "task") LocalRef<ChunkBuilderTask<? extends BuilderTaskOutput>> task
	) {
		if (task.get() != null || (type & BAKED_BE_SORT) == 0) return;

		task.set(createBakedBESortTask(section, this.frame));
	}

	@Unique
	public BakedBESortingTask createBakedBESortTask(RenderSection render, int frame) {
		var task = BakedBESortingTask.create(render, frame, this.cameraPosition);
		if (task != null) {
			task.calculateEstimations(this.jobDurationEstimator, this.meshTaskSizeEstimator, this.jobUploadDurationEstimator);
		}
		return task;
	}
}
