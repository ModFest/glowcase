package dev.hephaestus.glowcase.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.GlowcaseSectionRenderDispatcher;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Apply before sodium so things like allChanged don't clear the sections after they are created and causes catastrophic failures
@Mixin(value = LevelRenderer.class, priority = 990)
public class LevelRendererMixin {
	@Shadow private @Nullable ClientLevel level;

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V", ordinal = 0, shift = At.Shift.AFTER), method = "lambda$addMainPass$0")
	private void renderOpaque(CallbackInfo ci) {
		GlowcaseLevelRenderer.getInstance().renderGroup(false);
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V", ordinal = 1, shift = At.Shift.AFTER), method = "lambda$addMainPass$0")
	private void renderTranslucent(CallbackInfo ci) {
		GlowcaseLevelRenderer.getInstance().renderGroup(true);
	}

	@Inject(at = @At("RETURN"), method = "allChanged")
	private void onAllChanged(CallbackInfo ci) {
		if (this.level != null) GlowcaseLevelRenderer.getInstance().allChanged();
	}

	@Inject(at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareChunkRenders(Lorg/joml/Matrix4fc;)Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;"), method = "extractLevel")
	private void prepareBakedRenders(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci, @Local(name = "modelViewMatrix") Matrix4f modelViewMatrix) {
		GlowcaseLevelRenderer.getInstance().prepareRenders(modelViewMatrix, camera);
	}

	@Inject(at = @At("RETURN"), method = "compileSections")
	private void compilePendingSections(final Camera camera, CallbackInfo ci) {
		GlowcaseSectionRenderDispatcher renderDispatcher = GlowcaseLevelRenderer.getInstance().getSectionRenderDispatcher();
		if (renderDispatcher != null) {
			renderDispatcher.compileQueue.setCameraPosition(camera.position());
		}

		GlowcaseLevelRenderer.getInstance().compilePendingSections();
	}
}
