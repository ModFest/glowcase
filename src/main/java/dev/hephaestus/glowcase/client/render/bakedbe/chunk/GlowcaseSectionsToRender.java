package dev.hephaestus.glowcase.client.render.bakedbe.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hephaestus.glowcase.mixin.client.bakedbe.RenderTypeAccessor;
import dev.hephaestus.glowcase.util.DefaultedMapBase;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.NullMarked;

import java.util.*;

@NullMarked
public record GlowcaseSectionsToRender(
	RenderTypeGroups renderTypeGroups,
	DefaultedMapBase<RenderType, Map<Integer, List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroupsPerType,
	int maxIndicesRequired,
	GpuBufferSlice[] sectionTransforms
) {
	public void renderGroup(final boolean sorted) {
		final String name = sorted ? "translucent" : "solid";
		Map<RenderSystem.AutoStorageIndexBuffer, GpuBuffer> indexBuffers = new Object2ObjectArrayMap<>();
		List<RenderType> renderTypes = sorted ? renderTypeGroups.sorted : renderTypeGroups.unsorted;
		if (renderTypes.isEmpty()) return;

		ProfilerFiller profiler = Profiler.get();
		profiler.push("render_" + name); // +0

		List<RenderTask> renderTasks = new ArrayList<>(renderTypes.size());

		profiler.push("setup_textures"); // +1
		for (int i = 0, len = renderTypes.size(); i < len; i++) {
			RenderType renderType = renderTypes.get(i);

			var layerTextures = ((RenderTypeAccessor) renderType).getState().getTextures();
			List<String> textureNames = List.copyOf(layerTextures.keySet());

			RenderTask lastRenderTask = i == 0 ? null : renderTasks.get(i - 1);

			List<String> toRemove;
			if (lastRenderTask == null) {
				toRemove = List.of();
			} else  {
				toRemove = new ArrayList<>(lastRenderTask.textureNames);
				toRemove.removeAll(textureNames);
			}

			List<Texture> textures = new ArrayList<>();
			layerTextures.forEach((textureName, textureAndSampler) -> {
				textures.add(new Texture(textureName, textureAndSampler.textureView(), textureAndSampler.sampler()));
			});

			if (lastRenderTask != null) {
				textures.removeAll(lastRenderTask.textures);
			}

			renderTasks.add(new RenderTask(renderType, toRemove, textureNames, textures));
		}
		profiler.pop(); // -1

		RenderTarget renderTarget = outputTarget(sorted);
		assert renderTarget.getColorTextureView() != null;

		try (
			RenderPass renderPass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(
					() -> "Section layers for " + name + " types",
					renderTarget.getColorTextureView(),
					OptionalInt.empty(),
					renderTarget.getDepthTextureView(),
					OptionalDouble.empty()
				)
		) {
			RenderSystem.bindDefaultUniforms(renderPass);

			for (RenderTask renderTask : renderTasks) {
				RenderType renderType = renderTask.renderType;
				final String renderName = ((RenderTypeAccessor) renderType).getName();

				profiler.push(renderName); // +1
				renderPass.pushDebugGroup(() -> renderName);

				profiler.push("tex_remove"); // +2
				for (String textureName : renderTask.texturesToRemove) {
					renderPass.bindTexture(textureName, null, null);
				}

				profiler.popPush("auto_indices"); // 2
				RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(renderType.mode());
				GpuBuffer defaultIndexBuffer = indexBuffers.computeIfAbsent(autoIndices, _ -> {
						profiler.push("new_auto_indices"); // +3
						GpuBuffer buffer = this.maxIndicesRequired == 0 ? null : autoIndices.getBuffer(this.maxIndicesRequired);
						profiler.pop(); // -3
						return buffer;
					}
				);
				VertexFormat.IndexType indexType = this.maxIndicesRequired == 0 ? null : autoIndices.type();

				profiler.popPush("tex_bind"); // 2
				for (Texture texture : renderTask.textures) {
					renderPass.bindTexture(texture.name, texture.textureView, texture.sampler);
				}
				profiler.pop(); // -2

				renderPass.setPipeline(renderType.pipeline());

				var drawGroup = drawGroupsPerType.getValue(renderType);
				profiler.push("draw"); // +2
				for (var draws : drawGroup.values()) {
					if (draws.isEmpty()) continue;

					if (sorted) {
						draws = draws.reversed();
					}

					//noinspection DataFlowIssue
					renderPass.drawMultipleIndexed(draws, defaultIndexBuffer, indexType, List.of("DynamicTransforms"), sectionTransforms);
				}
				profiler.pop(); // -2

				renderPass.popDebugGroup();
				profiler.pop(); // -1
			}
		}

		profiler.pop(); // -0
	}

	public RenderTarget outputTarget(boolean translucent) {
		Minecraft minecraft = Minecraft.getInstance();

		RenderTarget renderTarget;
		if (translucent) {
			renderTarget = minecraft.levelRenderer.getTranslucentTarget();
		} else {
			renderTarget = minecraft.getMainRenderTarget();
		}

		return renderTarget != null ? renderTarget : minecraft.getMainRenderTarget();
	}

	private record RenderTask(RenderType renderType, List<String> texturesToRemove, List<String> textureNames, List<Texture> textures) {

	}

	private record Texture(String name, GpuTextureView textureView, GpuSampler sampler) {}
}
