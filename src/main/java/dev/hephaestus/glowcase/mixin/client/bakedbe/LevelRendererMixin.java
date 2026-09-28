package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import dev.hephaestus.glowcase.client.render.bakedbe.CachedFrame;
import dev.hephaestus.glowcase.client.render.bakedbe.RenderOffset;
import dev.hephaestus.glowcase.client.render.bakedbe.GpuBuffers;
import dev.hephaestus.glowcase.client.render.bakedbe.Identity;
import dev.hephaestus.glowcase.client.render.bakedbe.RenderSectionPos;
import dev.hephaestus.glowcase.client.render.block.entity.BakedBlockEntityRenderer;
import dev.hephaestus.glowcase.mixinsupport.BakedBuffers;
import dev.hephaestus.glowcase.mixinsupport.BakeryDynamicUniforms;
import dev.hephaestus.glowcase.mixinsupport.BakeryRenderDebug;
import dev.hephaestus.glowcase.mixinsupport.BakingBlockEntityRenderState;
import dev.hephaestus.glowcase.mixinsupport.RenderTypeInfoForBaking;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements BakeryRenderDebug {
	@Unique private final Object2ObjectOpenHashMap<RenderSectionPos, CachedFrame> glowcase$cachedFrames = new Object2ObjectOpenHashMap<>();
	@Unique private Set<GpuBuffer> glowcase$buffersInUse;

	@Shadow private @Final SubmitNodeStorage submitNodeStorage;
	@Shadow private @Final LevelRenderState levelRenderState;
	@Shadow private @Final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
	@Shadow private @Final FeatureRenderDispatcher featureRenderDispatcher;
	@Shadow private @Final RenderBuffers renderBuffers;
	@Shadow private @Final GameRenderer gameRenderer;

	@Shadow protected abstract void checkPoseStack(PoseStack poseStack);

	@Inject(at = @At("RETURN"), method = "<init>")
	private void getBakedBufferMapReference(CallbackInfo ci) {
		BakedBuffers bakedBuffers = (BakedBuffers) renderBuffers.stagedVertexBuffer();
		this.glowcase$buffersInUse = bakedBuffers.glowcase$cachedBuffers();
	}

	@Inject(at = @At("HEAD"), method = "invalidateCompiledGeometry")
	private void invalidateBakedBEs(CallbackInfo ci) {
		this.glowcase$cachedFrames.clear();
	}

	@Definition(id = "profiler", local = @Local(type = ProfilerFiller.class, name = "profiler"))
	@Definition(id = "push", method = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V")
	@Expression("profiler.push('repositionCamera')")
	@Inject(at = @At("MIXINEXTRAS:EXPRESSION"), method = "render")
	private void indexBakedRenderers(CallbackInfo ci, @Local(name = "profiler") ProfilerFiller profiler, @Share("pendingBaking") LocalRef<List<PendingBaking>> pendingSectionsRef) {
		profiler.push("indexBakedRenderers");
		this.glowcase$buffersInUse.clear();

		if (submitNodeStorage.getSubmitsPerOrder().isEmpty()) {
			this.glowcase$cachedFrames.clear();
		}

		var pendingSections = new ObjectArrayList<PendingBaking>();
		pendingSectionsRef.set(pendingSections);

		Map<RenderSectionPos, PendingBaking> identities = new Object2ObjectOpenHashMap<>();
		for (BlockEntityRenderState renderState : levelRenderState.blockEntityRenderStates) {
			var identity = ((BakingBlockEntityRenderState) renderState).glowcase$getIdentity();
			if (identity != null) {
				identities.computeIfAbsent(RenderSectionPos.fromVec3i(renderState.blockPos), PendingBaking::new).add(renderState, identity);
			}
		}

		// Rebuild changed regions and discard ones no longer being used
		if (!glowcase$cachedFrames.isEmpty()) {
			var iterator = glowcase$cachedFrames.object2ObjectEntrySet().fastIterator();
			while (iterator.hasNext()) {
				var entry = iterator.next();
				RenderSectionPos pos = entry.getKey();
				CachedFrame cachedFrame = entry.getValue();

				var pending = identities.remove(pos);
				if (pending == null) {
					// No longer used or within render distance, discard it
					iterator.remove();
				} else if (!cachedFrame.identity.equals(pending.identity)) {
					pendingSections.add(pending);
				}
			}
		}

		pendingSections.addAll(identities.values());

		if (!this.gameRenderer.useImprovedTransparency()) {
			profiler.popPush("bakedRendersTranslucentResort");
			for (CachedFrame frame : glowcase$cachedFrames.values()) {
				frame.resort(this.levelRenderState.cameraRenderState.pos, this.gameRenderer.renderBuffers().stagedVertexBuffer());
			}
		}

		profiler.pop();
	}

	// Must execute before the vanilla submits
	@Definition(id = "profiler", local = @Local(type = ProfilerFiller.class, name = "profiler"))
	@Definition(id = "popPush", method = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V")
	@Expression("profiler.popPush('submitFeatures')")
	@Inject(at = @At("MIXINEXTRAS:EXPRESSION"), method = "render")
	private void submitBakedRenderers(CallbackInfo ci, @Local(name = "profiler") ProfilerFiller profiler, @Share("pendingBaking") LocalRef<List<PendingBaking>> pendingSectionsRef) {
		var pendingSections = pendingSectionsRef.get();
		if (pendingSections.isEmpty()) return;

		profiler.popPush("bakeSections");
		PoseStack poseStack = new PoseStack();

		for (PendingBaking pendingSection : pendingSections) {
			glowcase$cachedFrames.put(pendingSection.pos, glowcase$bakeSection(poseStack, pendingSection, submitNodeStorage));
		}

		this.checkPoseStack(poseStack);
	}

	@Definition(id = "profiler", local = @Local(type = ProfilerFiller.class, name = "profiler"))
	@Definition(id = "popPush", method = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V")
	@Expression("profiler.popPush('prepareFeatures')")
	@Inject(at = @At("MIXINEXTRAS:EXPRESSION"), method = "render")
	private void prepareBakedRenderers(CallbackInfo ci, @Local(name = "profiler") ProfilerFiller profiler) {
		profiler.popPush("prepareBakedRenders");

		// This will be of size one most of the time but 4 should allow it to handle a lot without resizing
		var transforms = new ObjectArrayList<DynamicGpuData.Transform>(4);
		var infosForBaking = new ObjectArrayList<List<RenderTypeInfoForBaking>>(4);
		var fogOffsets = new ObjectArrayList<RenderOffset>(glowcase$cachedFrames.size());

		var fastSet = glowcase$cachedFrames.object2ObjectEntrySet();

		fastSet.fastForEach(entry -> {
			CachedFrame frame = entry.getValue();
			frame.updateCameraPos(this.levelRenderState.cameraRenderState.pos);
			fogOffsets.add(frame.getOffset());

			GpuBuffers buffers = frame.buffers;
			this.glowcase$buffersInUse.add(buffers.vertex());
			if (buffers.hasIndex()) {
				this.glowcase$buffersInUse.add(buffers.index());
			}
		});

		var offsetBuffers = this.glowcase$writeOffsetBuffers(fogOffsets);

		int i = 0;
		var iterator = fastSet.fastIterator();
		while (iterator.hasNext()) {
			CachedFrame frame = iterator.next().getValue();

			// Update the uniform buffers
			frame.prepare(offsetBuffers[i++], transforms, infosForBaking);
		}

		this.glowcase$writeDynamicBuffers(transforms, infosForBaking);
	}

	@Unique
	private void glowcase$writeDynamicBuffers(ObjectArrayList<DynamicGpuData.Transform> transforms, List<List<RenderTypeInfoForBaking>> infosForBaking) {
		var transformsArray = new DynamicGpuData.Transform[transforms.size()];
		System.arraycopy(transforms.elements(), 0, transformsArray, 0, transformsArray.length);
		var transformBuffers = RenderSystem.getDynamicUniforms().writeTransforms(transformsArray);

		for (int i = 0; i < infosForBaking.size(); i++) {
			for (RenderTypeInfoForBaking infoForBaking : infosForBaking.get(i)) {
				infoForBaking.glowcase$setDynamicTransforms(transformBuffers[i]);
			}
		}
	}

	@Unique
	private GpuBufferSlice[] glowcase$writeOffsetBuffers(ObjectArrayList<RenderOffset> offsets) {
		var offsetArray = new RenderOffset[offsets.size()];
		System.arraycopy(offsets.elements(), 0, offsetArray, 0, offsetArray.length);

		return ((BakeryDynamicUniforms) RenderSystem.getDynamicUniforms()).glowcase$writeFogOffsets(offsetArray);
	}

	@Unique
	private CachedFrame glowcase$bakeSection(PoseStack poseStack, PendingBaking pendingSection, SubmitNodeCollector submitNodeCollector) {
		for (BlockEntityRenderState renderState : pendingSection.states) {
			BlockPos sectionPos = RenderSectionPos.maskToSection(renderState.blockPos);

			poseStack.pushPose();
			poseStack.translate(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ());
			glowcase$submitForBaking(renderState, poseStack, submitNodeCollector);
			poseStack.popPose();
		}

		FeatureRenderDispatcher.PreparedFrame frame = this.featureRenderDispatcher.prepareFrame(submitNodeStorage);

		var stagedBuffer = ((StagedVertexBufferAccessor) renderBuffers.stagedVertexBuffer());
		var vertex = stagedBuffer.getCurrentVertexBuffer();
		var index = stagedBuffer.getCurrentIndexBuffer();
		List<StagedVertexBuffer.Draw> draws = index != null ? List.copyOf(stagedBuffer.getDraws()) : null;

		var buffers = new GpuBuffers(vertex, index, draws);
		var cached = new CachedFrame(pendingSection.identity, pendingSection.pos, frame, buffers);

		// Close the frame to avoid issues with the vanilla feature rendering, we also don't need to keep it open
		frame.close();

		return cached;
	}

	@Unique
	public <S extends BlockEntityRenderState> void glowcase$submitForBaking(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
		BlockEntityRenderer<?, S> renderer = this.blockEntityRenderDispatcher.getRenderer(state);
		if (!(renderer instanceof BakedBlockEntityRenderer<?, S> bakedRenderer)) return;

		try {
			bakedRenderer.submitForBaking(state, poseStack, submitNodeCollector);
		} catch (Throwable e) {
			CrashReport report = CrashReport.forThrowable(e, "Baking Block Entity");
			CrashReportCategory category = report.addCategory("Block Entity Details");
			state.fillCrashReportCategory(category);
			throw new ReportedException(report);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeSeeThrough(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"), method = "executeSeeThrough")
	private void executeBakedSeeThrough(CallbackInfo ci, @Local(name = "renderPass") RenderPass renderPass) {
		for (CachedFrame frame : glowcase$cachedFrames.values()) {
			frame.executeSeeThrough(renderPass);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeAlwaysOnTop(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"), method = "executeAlwaysOnTop")
	private void executeBakedAlwaysOnTop(CallbackInfo ci, @Local(name = "renderPass") RenderPass renderPass) {
		for (CachedFrame frame : glowcase$cachedFrames.values()) {
			frame.executeAlwaysOnTop(renderPass);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeSolid(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"), method = "executeSolid")
	private void executeBakedSolid(CallbackInfo ci, @Local(name = "renderPass", argsOnly = true) RenderPass renderPass) {
		for (CachedFrame frame : glowcase$cachedFrames.values()) {
			frame.executeSolid(renderPass);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeOit(Lnet/minecraft/client/renderer/oit/OitStage;Lcom/mojang/renderpearl/api/commands/RenderPass;)V"), method = "executeOit")
	private void executeBakedOit(CallbackInfo ci, @Local(name = "stage") OitStage stage, @Local(name = "renderPass") RenderPass renderPass) {
		for (CachedFrame frame : glowcase$cachedFrames.values()) {
			frame.executeOit(stage, renderPass);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeWaterMask(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"), method = "executeOitWaterMask")
	private void executeBakedWaterMask(CallbackInfo ci, @Local(name = "renderPass") RenderPass renderPass) {
		for (CachedFrame frame : glowcase$cachedFrames.values()) {
			frame.executeWaterMask(renderPass);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeTranslucent(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"), method = "executeClassicTransparency")
	private void executeBakedTranslucent(CallbackInfo ci, @Local(name = "renderPass", argsOnly = true) RenderPass renderPass) {
		for (CachedFrame frame : glowcase$cachedFrames.values()) {
			frame.executeTranslucent(renderPass);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeTranslucentAfterTerrain(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"), method = "executeClassicTransparency")
	private void executeBakedTranslucentAfterTerrain(CallbackInfo ci, @Local(name = "renderPass", argsOnly = true) RenderPass renderPass) {
		for (CachedFrame frame : glowcase$cachedFrames.values()) {
			frame.executeTranslucentAfterTerrain(renderPass);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeOutline(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"), method = "executeOutline")
	private void executeBakedOutline(CallbackInfo ci, @Local(name = "renderPass") RenderPass renderPass) {
		for (CachedFrame frame : glowcase$cachedFrames.values()) {
			frame.executeOutline(renderPass);
		}
	}

	@Override
	public @NonNull List<String> glowcase$getBakeryStats() {
		var result = new ObjectArrayList<String>();
		result.add(ChatFormatting.UNDERLINE + "Baked BE stats:");
		result.add("Count: " + glowcase$cachedFrames.size());
		result.add("Buffer Count: " + glowcase$buffersInUse.size());
		result.add("Sections: ");

		for (CachedFrame frame : glowcase$cachedFrames.values()) {
			result.add(frame.sectionPos.toCommandString());
		}

		return result;
	}

	private record PendingBaking(RenderSectionPos pos, ObjectArrayList<BlockEntityRenderState> states, ObjectArrayList<Identity> identity) {
		public PendingBaking(RenderSectionPos pos) {
			this(pos, new ObjectArrayList<>(), new ObjectArrayList<>());
		}

		void add(BlockEntityRenderState state, Identity identity) {
			this.states.add(state);
			this.identity.add(identity);
		}
	}
}
