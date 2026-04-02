package dev.hephaestus.glowcase.client.render.bakedbe.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hephaestus.glowcase.mixin.client.RenderTypeAccessor;
import dev.hephaestus.glowcase.util.DefaultedMapBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

public record GlowcaseSectionsToRender(
	RenderTypeGroups renderTypeGroups,
	DefaultedMapBase<RenderType, Map<Integer, List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroupsPerType,
	int maxIndicesRequired,
	GpuBufferSlice[] sectionTransforms
) {
	public GlowcaseSectionsToRender(DefaultedMapBase<RenderType, Map<Integer, List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroupsPerType, int maxIndicesRequired, GpuBufferSlice[] sectionTransforms) {
	    this(new RenderTypeGroups(drawGroupsPerType.keySet()), drawGroupsPerType, maxIndicesRequired, sectionTransforms);
	}

	public void renderGroup(final boolean sorted) {
		ProfilerFiller profiler = Profiler.get();
		Map<RenderSystem.AutoStorageIndexBuffer, GpuBuffer> indexBuffers = new Object2ObjectArrayMap<>();

		String[] lastTextures = new String[0];
		RenderTarget currentTarget = null;
		RenderPass renderPass = null;
		AtomicInteger i = new AtomicInteger();

		final String name = sorted ? "sorted" : "unsorted";
		for (var renderType : sorted ? renderTypeGroups.sorted : renderTypeGroups.unsorted) {
			final String renderName = ((RenderTypeAccessor) renderType).getName();
			profiler.push(renderName);

			Map<String, RenderSetup.TextureAndSampler> textures;
			//region Change render pass if needed
			try {
				profiler.push("texture");
				textures = ((RenderTypeAccessor) renderType).getState().getTextures();
			} catch (ReportedException e) {
				profiler.popPush("texture_fallback");
				// There is no render pass, the error is something else
				if (renderPass == null) throw e;

				// The method failed as it had to upload the textures,
				// so close the pass and let it upload
				renderPass.close();
				renderPass = null;
				textures = ((RenderTypeAccessor) renderType).getState().getTextures();
			} finally {
				profiler.pop();
			}

			RenderTarget renderTarget = renderType.outputTarget().getRenderTarget();
			if (currentTarget != renderTarget || renderPass == null) {
				profiler.push("new_render_pass");
				if (renderPass != null) renderPass.close();

				currentTarget = renderTarget;
				//noinspection DataFlowIssue
				renderPass = RenderSystem.getDevice()
					.createCommandEncoder()
					.createRenderPass(
						() -> "Section layers for " + name + " types (" + i.getAndIncrement() + ")",
						renderTarget.getColorTextureView(),
						OptionalInt.empty(),
						renderTarget.getDepthTextureView(),
						OptionalDouble.empty()
					);

				RenderSystem.bindDefaultUniforms(renderPass);
				profiler.pop();
			}
			//endregion
			renderPass.pushDebugGroup(() -> renderName);

			//region Clear last textures
			profiler.push("tex_reset");
			for (String lastTexture : lastTextures) {
				renderPass.bindTexture(lastTexture, null, null);
			}

			lastTextures = new String[textures.size()];
			textures.keySet().toArray(lastTextures);
			//endregion

			profiler.popPush("auto_indices");
			RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(renderType.mode());
			GpuBuffer defaultIndexBuffer = indexBuffers.computeIfAbsent(autoIndices, _ -> {
					profiler.push("new_auto_indices");
					GpuBuffer buffer = this.maxIndicesRequired == 0 ? null : autoIndices.getBuffer(this.maxIndicesRequired);
					profiler.pop();
					return buffer;
				}
			);
			VertexFormat.IndexType indexType = this.maxIndicesRequired == 0 ? null : autoIndices.type();

			profiler.popPush("tex_bind");
			for (Map.Entry<String, RenderSetup.TextureAndSampler> entry : textures.entrySet()) {
				renderPass.bindTexture(entry.getKey(), entry.getValue().textureView(), entry.getValue().sampler());
			}
			profiler.pop();

			renderPass.setPipeline(renderType.pipeline());

			var drawGroup = drawGroupsPerType.getValue(renderType);
			profiler.push("draw");
			for (var draws : drawGroup.values()) {
				if (draws.isEmpty()) continue;

				if (renderType.sortOnUpload()) {
					draws = draws.reversed();
				}

				renderPass.drawMultipleIndexed(draws, defaultIndexBuffer, indexType, List.of("DynamicTransforms"), sectionTransforms);
			}

			profiler.pop(); profiler.pop();
			renderPass.popDebugGroup();
		}

		if (renderPass != null) renderPass.close();
	}
}
