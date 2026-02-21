package dev.hephaestus.glowcase.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
	@Unique private final GpuBuffer texColorBuffer = RenderSystem.getDevice().createBuffer(() -> "TexColorQuad", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, 16 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize());

	@Shadow @Final private List<GuiRenderer.Draw> draws;
	@Shadow @Final private ByteBufferBuilder byteBufferBuilder;

	@Shadow protected abstract void executeDrawRange(Supplier debugGroup, RenderTarget renderTarget, GpuBufferSlice fog, GpuBufferSlice dynamicTransforms, GpuBuffer buffer, VertexFormat.IndexType indexType, int start, int end);

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;", shift = At.Shift.AFTER), method = "draw")
	private void findBlurDraw(GpuBufferSlice fogBuffer, CallbackInfo ci, @Share("suggestionBlurLayer") LocalRef<Integer> suggestionBlurLayer) {
		suggestionBlurLayer.set(Integer.MAX_VALUE);
		for (int i = 0; i < this.draws.size(); i++) {
			GuiRenderer.Draw draw = draws.get(i);
			if (draw.textureSetup().texure0() == SuggestionListWidget.FRAMEBUFFER.getColorTextureView() && draw.pipeline() == RenderPipelines.MOJANG_LOGO) {
				suggestionBlurLayer.set(i);
				this.draws.remove(draw);
				break;
			}
		}
	}

	@WrapOperation(at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 2), method = "draw")
	private int renderBeforeSuggestionBlurAfterBlur(List<GuiRenderer.Draw> instance, Operation<Integer> original, @Share("suggestionBlurLayer") LocalRef<Integer> suggestionBlurLayer, @Share("afterBlurLimit") LocalRef<Integer> afterBlurLimit) {
		Integer i = original.call(instance);
		afterBlurLimit.set(i); // Save it for mod compat (in case another mod also changes it)
		return Math.min(suggestionBlurLayer.get(), i);
	}

	@WrapOperation(at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0), method = "draw")
	private int renderBeforeSuggestionBlurBeforeBlur(List<GuiRenderer.Draw> instance, Operation<Integer> original, @Share("suggestionBlurLayer") LocalRef<Integer> suggestionBlurLayer, @Share("beforeBlurLimit") LocalRef<Integer> beforeBlurLimit) {
		Integer i = original.call(instance);
		beforeBlurLimit.set(i); // Save it for mod compat (in case another mod also changes it)
		return Math.min(suggestionBlurLayer.get(), i);
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V", ordinal = 0, shift = At.Shift.AFTER), method = "draw")
	private void renderSuggestionsBlurBeforeBlur(GpuBufferSlice fogBuffer, CallbackInfo ci, @Local GpuBuffer gpuBuffer, @Local VertexFormat.IndexType indexType, @Local(ordinal = 1) GpuBufferSlice gpuBufferSlice, @Share("suggestionBlurLayer") LocalRef<Integer> suggestionBlurLayer, @Share("beforeBlurLimit") LocalRef<Integer> beforeBlurLimit) {
		Integer layer = suggestionBlurLayer.get();
		if (this.draws.size() > layer) {
			renderSuggestionsBlur(() -> "GUI before blur", fogBuffer, gpuBuffer, indexType, gpuBufferSlice, layer, beforeBlurLimit.get());
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V", ordinal = 1, shift = At.Shift.AFTER), method = "draw")
	private void renderSuggestionsBlurAfterBlur(GpuBufferSlice fogBuffer, CallbackInfo ci, @Local GpuBuffer gpuBuffer, @Local VertexFormat.IndexType indexType, @Local(ordinal = 1) GpuBufferSlice gpuBufferSlice, @Share("suggestionBlurLayer") LocalRef<Integer> suggestionBlurLayer, @Share("afterBlurLimit") LocalRef<Integer> afterBlurLimit) {
		Integer layer = suggestionBlurLayer.get();
		if (this.draws.size() > layer) {
			renderSuggestionsBlur(() -> "GUI after blur", fogBuffer, gpuBuffer, indexType, gpuBufferSlice, layer, afterBlurLimit.get());
		}
	}

	@Unique
	private void renderSuggestionsBlur(Supplier<String> nameSupplier, GpuBufferSlice fogBuffer, GpuBuffer indexBuffer, VertexFormat.IndexType indexType, GpuBufferSlice dynamicTransformsBuffer, int from, int to) {
		RenderSystem.getDevice().createCommandEncoder().clearColorTexture(SuggestionListWidget.FRAMEBUFFER.getColorTexture(), 0);

		Minecraft client = Minecraft.getInstance();
		RenderTarget framebuffer = SuggestionListWidget.FRAMEBUFFER;
		RenderTarget clientFramebuffer = client.getMainRenderTarget();
		if (framebuffer.width != clientFramebuffer.width || framebuffer.height != clientFramebuffer.height) {
			framebuffer.resize(clientFramebuffer.width, clientFramebuffer.height);

			int width = client.getWindow().getGuiScaledWidth();
			int height = client.getWindow().getGuiScaledHeight();

			Matrix3x2f matrices = new Matrix3x2f();
			BufferBuilder bufferBuilder = new BufferBuilder(this.byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			bufferBuilder.addVertexWith2DPose(matrices, 0, 	0).setUv(0, 0).setColor(0XFFFFFFFF);
			bufferBuilder.addVertexWith2DPose(matrices, 0, 	height).setUv(0, 1).setColor(0XFFFFFFFF);
			bufferBuilder.addVertexWith2DPose(matrices, width, 	height).setUv(1, 1).setColor(0XFFFFFFFF);
			bufferBuilder.addVertexWith2DPose(matrices, width, 	0).setUv(1, 0).setColor(0XFFFFFFFF);

			try (MeshData builtBuffer = bufferBuilder.buildOrThrow()) {
				RenderSystem.getDevice().createCommandEncoder().writeToBuffer(texColorBuffer.slice(), builtBuffer.vertexBuffer());
			}
		}

		copyTexture(clientFramebuffer, framebuffer, dynamicTransformsBuffer);
		if ((float) client.options.getMenuBackgroundBlurriness() >= 1.0F) {
			renderBlur(framebuffer);
		}

		this.executeDrawRange(nameSupplier, clientFramebuffer, fogBuffer, dynamicTransformsBuffer, indexBuffer, indexType, from, to);
	}

	@Unique
	public void copyTexture(RenderTarget sourceBuffer, RenderTarget targetBuffer, GpuBufferSlice dynamicTransformsBuffer) {
		RenderSystem.assertOnRenderThread();
		RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		GpuBuffer indexBuffer = shapeIndexBuffer.getBuffer(6);

		try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
			() -> "Copy render target",
			targetBuffer.getColorTextureView(), OptionalInt.empty()
		)) {
			renderPass.setPipeline(RenderPipelines.GUI_TEXTURED);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", dynamicTransformsBuffer);
			renderPass.setIndexBuffer(indexBuffer, shapeIndexBuffer.type());
			renderPass.setVertexBuffer(0, texColorBuffer);
			renderPass.bindSampler("Sampler0", sourceBuffer.getColorTextureView());

			renderPass.drawIndexed(0, 0, 6, 1);
		}
	}

	@Inject(at = @At("RETURN"), method = "draw")
	private void decrementPool(GpuBufferSlice fogBuffer, CallbackInfo ci) {
		SuggestionListWidget.POOL.endFrame();
	}

	@Unique
	public void renderBlur(RenderTarget framebuffer) {
		PostChain postEffectProcessor = Minecraft.getInstance().getShaderManager().getPostChain(SuggestionListWidget.BLUR_ID, LevelTargetBundle.MAIN_TARGETS);
		if (postEffectProcessor != null) {
			postEffectProcessor.process(framebuffer, SuggestionListWidget.POOL);
		}
	}
}
