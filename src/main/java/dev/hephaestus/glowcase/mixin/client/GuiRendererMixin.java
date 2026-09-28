package dev.hephaestus.glowcase.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import dev.hephaestus.glowcase.mixin.client.bakedbe.TextureSetupAccessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Supplier;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
	@Shadow private @Final List<GuiRenderer.Draw> draws;

	@Shadow protected abstract void executeDrawRange(Supplier<String> label, RenderTarget mainRenderTarget, GpuBufferSlice dynamicTransforms, int startIndex, int endIndex);

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;", shift = At.Shift.AFTER), method = "draw")
	private void findBlurDraw(CallbackInfo ci, @Share("suggestionBlurLayer") LocalRef<Integer> suggestionBlurLayer, @Share("viewsToReplace") LocalRef<ObjectArrayList<GuiRenderer.Draw>> viewsToReplace) {
		suggestionBlurLayer.set(Integer.MAX_VALUE);
		var views = new ObjectArrayList<GuiRenderer.Draw>();
		viewsToReplace.set(views);

		if ((float) Minecraft.getInstance().options.getMenuBackgroundBlurriness() < 1.0F) {
			return;
		}

		boolean layerFound = false;
		for (int i = 0; i < this.draws.size(); i++) {
			GuiRenderer.Draw draw = draws.get(i);
			if (draw.textureSetup().texure0() != SuggestionListWidget.BLUR_TEXTURE.getColorTextureView()) continue;

			if (layerFound || draw.pipeline() != RenderPipelines.MOJANG_LOGO) {
				views.add(draw);
				continue;
			}

			layerFound = true;
			suggestionBlurLayer.set(i);
		}

		if (layerFound) {
			this.draws.remove(suggestionBlurLayer.get().intValue());
		}
	}

	@WrapOperation(at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", ordinal = 0), method = "draw")
	private int splitForSuggestionBlur(int a, int b, Operation<Integer> original, @Share("suggestionBlurLayer") LocalRef<Integer> suggestionBlurLayer, @Share("beforeBlurLimit") LocalRef<Integer> beforeBlurLimit) {
		Integer i = original.call(a, b);
		beforeBlurLimit.set(i); // Save it for mod compat (in case another mod also changes it)
		return Math.min(suggestionBlurLayer.get(), i);
	}

	@WrapOperation(at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 2), method = "draw")
	private int splitForSuggestionBlur(List<GuiRenderer.Draw> instance, Operation<Integer> original, @Share("suggestionBlurLayer") LocalRef<Integer> suggestionBlurLayer, @Share("afterBlurLimit") LocalRef<Integer> afterBlurLimit) {
		Integer i = original.call(instance);
		afterBlurLimit.set(i); // Save it for mod compat (in case another mod also changes it)
		return Math.min(suggestionBlurLayer.get(), i);
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;II)V", ordinal = 0, shift = At.Shift.AFTER), method = "draw")
	private void processSuggestionsBlurBeforeBlur(
		CallbackInfo ci,
		@Local(name = "mainRenderTarget") RenderTarget mainRenderTarget,
		@Local(name = "dynamicTransforms") GpuBufferSlice dynamicTransforms,
		@Share("suggestionBlurLayer") LocalRef<Integer> suggestionBlurLayer,
		@Share("beforeBlurLimit") LocalRef<Integer> beforeBlurLimit,
		@Share("viewsToReplace") LocalRef<ObjectArrayList<GuiRenderer.Draw>> viewsToReplace
	) {
		Integer limit = beforeBlurLimit.get();
		Integer layer = suggestionBlurLayer.get();
		if (limit > layer) {
			glowcase$processSuggestionsBlur(() -> "GUI before blur", mainRenderTarget, dynamicTransforms, layer, limit, viewsToReplace.get());
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;II)V", ordinal = 1, shift = At.Shift.AFTER), method = "draw")
	private void processSuggestionsBlurAfterBlur(
		CallbackInfo ci,
		@Local(name = "mainRenderTarget") RenderTarget mainRenderTarget,
		@Local(name = "dynamicTransforms") GpuBufferSlice dynamicTransforms,
		@Share("suggestionBlurLayer") LocalRef<Integer> suggestionBlurLayer,
		@Share("afterBlurLimit") LocalRef<Integer> afterBlurLimit,
		@Share("viewsToReplace") LocalRef<ObjectArrayList<GuiRenderer.Draw>> viewsToReplace
	) {
		Integer limit = afterBlurLimit.get();
		Integer layer = suggestionBlurLayer.get();
		if (limit > layer) {
			glowcase$processSuggestionsBlur(() -> "GUI after blur", mainRenderTarget, dynamicTransforms, layer, limit, viewsToReplace.get());
		}
	}

	@Unique
	private void glowcase$processSuggestionsBlur(
		Supplier<String> nameSupplier,
		RenderTarget mainRenderTarget,
		GpuBufferSlice dynamicTransforms,
		int startIndex,
		int endIndex,
		ObjectArrayList<GuiRenderer.Draw> viewsToReplace
	) {
		RenderTarget blurRenderTarget = SuggestionListWidget.BLUR_TEXTURE;
		if (blurRenderTarget.width != mainRenderTarget.width || blurRenderTarget.height != mainRenderTarget.height || blurRenderTarget.getColorTextureView().isClosed()) {
			blurRenderTarget.resize(mainRenderTarget.width, mainRenderTarget.height);

			for (var draw : viewsToReplace) {
				((TextureSetupAccessor) (Object) draw.textureSetup()).setTexure0(blurRenderTarget.getColorTextureView());
			}
		}

		blurRenderTarget.copyColorFrom(mainRenderTarget);

		glowcase$processBlur(blurRenderTarget);

		this.executeDrawRange(nameSupplier, mainRenderTarget, dynamicTransforms, startIndex, endIndex);
	}

	@Inject(at = @At("RETURN"), method = "render")
	private void decrementPool(CallbackInfo ci) {
		SuggestionListWidget.POOL.endFrame();
	}

	@Unique
	public void glowcase$processBlur(RenderTarget framebuffer) {
		PostChain postEffectProcessor = Minecraft.getInstance().getShaderManager().getPostChain(SuggestionListWidget.BLUR_ID, LevelTargetBundle.MAIN_TARGETS);
		if (postEffectProcessor != null) {
			postEffectProcessor.process(framebuffer, SuggestionListWidget.POOL);
		}
	}
}
