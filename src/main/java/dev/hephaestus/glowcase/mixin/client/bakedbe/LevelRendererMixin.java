package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.llamalad7.mixinextras.sugar.Local;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.GlowcaseSectionRenderDispatcher;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.render.font.GlyphBakeQueue;
import dev.hephaestus.glowcase.mixinsupport.LevelRendererExtension;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Apply before sodium so things like allChanged don't clear the sections after they are created and causes catastrophic failures
@Mixin(value = LevelRenderer.class, priority = 990)
public class LevelRendererMixin implements LevelRendererExtension {
	@Shadow private @Nullable ClientLevel level;
	@Shadow @Final private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

	private @Unique GlowcaseLevelRenderer levelRenderer;

	@Inject(at = @At("RETURN"), method = "<init>")
	private void init(CallbackInfo ci) {
		this.levelRenderer = new GlowcaseLevelRenderer((LevelRenderer) (Object) this);
	}

	@Override
	public GlowcaseLevelRenderer glowcase$getLevelRenderer() {
		return levelRenderer;
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V", ordinal = 0, shift = At.Shift.AFTER), method = "lambda$addMainPass$0")
	private void renderOpaque(CallbackInfo ci) {
		levelRenderer.renderGroup(false);
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V", ordinal = 1), method = "lambda$addMainPass$0")
	private void renderTranslucent(CallbackInfo ci) {
		levelRenderer.renderGroup(true);
	}

	@Inject(at = @At("RETURN"), method = "setLevel")
	private void onSetLevel(ClientLevel level, CallbackInfo ci) {
		levelRenderer.setLevel(level);
	}

	@Inject(at = @At("RETURN"), method = "allChanged")
	private void onAllChanged(CallbackInfo ci) {
		if (this.level != null) levelRenderer.allChanged();
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;cullTerrain(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Z)V", shift = At.Shift.AFTER), method = "update")
	private void applyPendingSectionMapChanges(CallbackInfo ci) {
		levelRenderer.applyPendingSectionMapChanges();
	}

	@Inject(at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareChunkRenders(Lorg/joml/Matrix4fc;)Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;"), method = "extractLevel")
	private void prepareBakedRenders(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci, @Local(name = "modelViewMatrix") Matrix4f modelViewMatrix, @Local(name = "profiler") ProfilerFiller profiler) {
		profiler.push("glowcase");
		levelRenderer.prepareRenders(modelViewMatrix, camera);
		profiler.pop();
	}

	@Inject(at = @At("RETURN"), method = "compileSections")
	private void compilePendingSections(final Camera camera, CallbackInfo ci) {
		levelRenderer.compilePendingSections();
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SectionOcclusionGraph;addSectionsInFrustum(Lnet/minecraft/client/renderer/culling/Frustum;Ljava/util/List;Ljava/util/List;)V", shift = At.Shift.AFTER), method = "applyFrustum")
	private void resortVisibleSections(CallbackInfo ci) {
		ProfilerFiller profiler = Profiler.get();
		profiler.push("glowcase:baked_be/tree_resort");

		LongArrayList sortedSections = new LongArrayList(this.visibleSections.size());
		for (SectionRenderDispatcher.RenderSection visibleSection : this.visibleSections) {
			sortedSections.add(visibleSection.getSectionNode());
		}
		levelRenderer.visibleSections().finishSorting(sortedSections);

		profiler.pop();
	}
}
