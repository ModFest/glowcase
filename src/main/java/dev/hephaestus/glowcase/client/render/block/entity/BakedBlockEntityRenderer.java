package dev.hephaestus.glowcase.client.render.block.entity;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import dev.hephaestus.glowcase.mixin.client.GameRendererAccessor;
import dev.hephaestus.glowcase.mixin.client.CompositeRenderTypeAccessor;
import dev.hephaestus.glowcase.mixin.client.CompositeStateAccessor;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelTerrainRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;

public abstract class BakedBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
	protected final BlockEntityRendererProvider.Context context;

	protected BakedBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		this.context = context;
	}

	/**
	 * Handles invalidation and passing of rendered vertices to the baking system.
	 * Override {@link #renderBaked(BlockEntity, PoseStack, MultiBufferSource, int, int)} and
	 * {@link #renderBaked(BlockEntity, PoseStack, MultiBufferSource, int, int)} instead of this method.
	 */
	@Override
	public final void render(T entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
		renderUnbaked(entity, tickDelta, matrices, vertexConsumers, light, overlay, cameraPos);
		Manager.activateRegion(entity.getBlockPos());
	}

	/**
	 * Render vertices to be baked into the render region. This method will be called every time the render region is rebuilt - so
	 * you should only render vertices that don't move here. You can call {@link Manager#markForRebuild(BlockPos)} to
	 * cause the render region to be rebuilt, but do not call this too frequently as it will affect performance.
	 * You must use the provided VertexConsumerProvider and MatrixStack to render your vertices - any use of Tessellator
	 * or RenderSystem here will not work. If you need custom rendering settings, you can use a custom RenderLayer.
	 */
	public abstract void renderBaked(T entity, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay);

	/**
	 * Render vertices immediately. This works exactly the same way as a normal BER render method, and can be used for dynamic
	 * rendering that changes every frame. In this method you can also check for render invalidation and call {@link Manager#markForRebuild(BlockPos)}
	 * as appropriate.
	 */
	public abstract void renderUnbaked(T entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos);

	public abstract boolean shouldBake(T entity);

	private record RenderRegionPos(int x, int z, @NotNull BlockPos origin) {
		public RenderRegionPos(int x, int z) {
			this(x, z, new BlockPos(x << Manager.REGION_SHIFT, 0, z << Manager.REGION_SHIFT));
		}

		public RenderRegionPos(BlockPos pos) {
			this(pos.getX() >> Manager.REGION_SHIFT, pos.getZ() >> Manager.REGION_SHIFT);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			RenderRegionPos that = (RenderRegionPos) o;
			return x == that.x &&
				   z == that.z;
		}

		@Override
		public int hashCode() {
			return Objects.hash(x, z);
		}
	}

	public static class Manager {
		// 2x2 chunks size for regions
		public static final int REGION_FROMCHUNK_SHIFT = 1;
		public static final int REGION_SHIFT = 4 + REGION_FROMCHUNK_SHIFT;
		public static final int MAX_XZ_IN_REGION = (16 << REGION_FROMCHUNK_SHIFT) - 1;
		public static final int VIEW_RADIUS = 3;

		private static final Object2ReferenceMap<RenderRegionPos, RegionBuffer> regions = new Object2ReferenceOpenHashMap<>();
		private static final Set<RenderRegionPos> needsRebuild = Sets.newHashSet();

		private static class CachedVertexConsumerProvider implements MultiBufferSource {
			private final Reference2ReferenceMap<RenderType, ByteBufferBuilder> allocators = new Reference2ReferenceOpenHashMap<>();
			private final Reference2ReferenceMap<RenderType, BufferBuilder> builders = new Reference2ReferenceOpenHashMap<>();

			@Override
			public VertexConsumer getBuffer(RenderType layer) {
				return builders.computeIfAbsent(layer, l1 -> new BufferBuilder(
					allocators.computeIfAbsent(layer, l2 -> new ByteBufferBuilder(layer.bufferSize())),
					layer.mode(),
					layer.format()));
			}

			/**
			 * Resets the provider so another scene can be rendered
			 */
			public void reset() {
				allocators.forEach((layer, allocator) -> allocator.discard());
				builders.clear();
			}
		}

		private static final CachedVertexConsumerProvider vcp = new CachedVertexConsumerProvider();

		private static final Logger LOGGER = LogUtils.getLogger();

		private static class RegionBuffer {
			private final Map<RenderType, SectionBuffers> layerBuffers = new Reference2ReferenceOpenHashMap<>();
			private final Set<RenderType> uploadedLayers = new ObjectOpenHashSet<>();

			@SuppressWarnings("DataFlowIssue")
			public void render(RenderType layer, PoseStack matrices) {
				RenderTarget framebuffer;
				if (layer instanceof RenderType.CompositeRenderType) {
					framebuffer = ((CompositeStateAccessor) (Object) ((CompositeRenderTypeAccessor) layer).getPhases()).getTarget().getRenderTarget();
				} else {
					framebuffer = Minecraft.getInstance().getMainRenderTarget();
				}

				RenderPipeline pipeline;
				if (layer instanceof RenderType.CompositeRenderType) {
					pipeline = ((CompositeRenderTypeAccessor) layer).getPipeline();
				} else {
					pipeline = RenderPipelines.SOLID;
				}

				layer.setupRenderState();
				GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
					.writeTransform(
						matrices.last().pose(),
						new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
						RenderSystem.getModelOffset(),
						RenderSystem.getTextureMatrix(),
						RenderSystem.getShaderLineWidth()
					);

				try (RenderPass renderPass = RenderSystem.getDevice()
					.createCommandEncoder()
					.createRenderPass(
						() -> "Glowcase baked BER section layers",
						framebuffer.getColorTextureView(),
						OptionalInt.empty(),
						framebuffer.getDepthTextureView(),
						OptionalDouble.empty()
					)) {
					SectionBuffers buffers = layerBuffers.get(layer);

					GpuBuffer indexBuffer;
					VertexFormat.IndexType indexType;
					if (buffers.getIndexBuffer() == null) {
						RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(layer.mode());
						indexBuffer = shapeIndexBuffer.getBuffer(buffers.getIndexCount());
						indexType = shapeIndexBuffer.type();
					} else {
						indexBuffer = buffers.getIndexBuffer();
						indexType = buffers.getIndexType();
					}

					ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
					if (scissorState.enabled()) {
						renderPass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
					}

					for (int j = 0; j < 12; j++) {
						GpuTextureView gpuTextureView3 = RenderSystem.getShaderTexture(j);
						if (gpuTextureView3 != null) {
							renderPass.bindSampler("Sampler" + j, gpuTextureView3);
						}
					}

					renderPass.setPipeline(pipeline);
					renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
					renderPass.setVertexBuffer(0, buffers.getVertexBuffer());
					renderPass.setIndexBuffer(indexBuffer, indexType);
					RenderSystem.bindDefaultUniforms(renderPass);

					renderPass.drawIndexed(0, 0, buffers.getIndexCount(), 1);
				}
				layer.clearRenderState();

				//VertexBuffer buf = layerBuffers.get(layer);
				//buf.bind();
				//layer.startDrawing();
				//buf.draw(matrices.peek().getPositionMatrix(), projectionMatrix, RenderSystem.getShader());
				//layer.endDrawing();
				//VertexBuffer.unbind();
			}

			public void upload(RenderType layer, BufferBuilder newBuf) {
				try (MeshData buffer = newBuf.build()) {
					if (buffer == null) return;

					CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
					SectionBuffers oldBuffers = this.layerBuffers.get(layer);
					if (oldBuffers != null) {
						if (oldBuffers.getVertexBuffer().size() < buffer.vertexBuffer().remaining()) {
							oldBuffers.getVertexBuffer().close();
							oldBuffers.setVertexBuffer(
								RenderSystem.getDevice()
									.createBuffer(
										() -> "Glowcase Region vertex buffer - layer: " + layer.getName(),
										GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
										buffer.vertexBuffer()
									)
							);
						} else if (!oldBuffers.getVertexBuffer().isClosed()) {
							commandEncoder.writeToBuffer(oldBuffers.getVertexBuffer().slice(), buffer.vertexBuffer());
						}

						ByteBuffer byteBuffer = buffer.indexBuffer();
						if (byteBuffer != null) {
							if (oldBuffers.getIndexBuffer() != null && oldBuffers.getIndexBuffer().size() >= byteBuffer.remaining()) {
								if (!oldBuffers.getIndexBuffer().isClosed()) {
									commandEncoder.writeToBuffer(oldBuffers.getIndexBuffer().slice(), byteBuffer);
								}
							} else {
								if (oldBuffers.getIndexBuffer() != null) {
									oldBuffers.getIndexBuffer().close();
								}

								oldBuffers.setIndexBuffer(
									RenderSystem.getDevice()
										.createBuffer(
											() -> "Glowcase Region index buffer - layer: " + layer.getName(),
											GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
											byteBuffer
										)
								);
							}
						} else if (oldBuffers.getIndexBuffer() != null) {
							oldBuffers.getIndexBuffer().close();
							oldBuffers.setIndexBuffer(null);
						}

						oldBuffers.setIndexCount(buffer.drawState().indexCount());
						oldBuffers.setIndexType(buffer.drawState().indexType());
					} else {
						GpuBuffer vertexBuffer = RenderSystem.getDevice()
							.createBuffer(
								() -> "Glowcase Region vertex buffer - layer: " + layer.getName(),
								GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
								buffer.vertexBuffer()
							);
						ByteBuffer sortedBuffer = buffer.indexBuffer();
						GpuBuffer indexBuffer = sortedBuffer != null
							? RenderSystem.getDevice()
							.createBuffer(
								() -> "Glowcase Region index buffer - layer: " + layer.getName(),
								GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
								sortedBuffer
							)
							: null;
						this.layerBuffers.put(layer, new SectionBuffers(vertexBuffer, indexBuffer, buffer.drawState().indexCount(), buffer.drawState().indexType()));
					}

					this.uploadedLayers.add(layer);
				}
			}

			public void reset() {
				layerBuffers.values().forEach(SectionBuffers::close);
				layerBuffers.clear();
				uploadedLayers.clear();
			}
		}

		/**
		 * Causes the render region containing this BlockEntity to be rebuilt -
		 * do not call this too frequently as it will affect performance.
		 * An invalidation will not immediately cause the next frame to contain an updated view (and call to renderBaked)
		 * as all render region rebuilds must call every BER that is to be rendered, otherwise they will be missing from the
		 * vertex buffer.
		 */
		public static void markForRebuild(BlockPos pos) {
			needsRebuild.add(new RenderRegionPos(pos));
		}

		// TODO: move chunk baking off-thread?

		private static boolean isVisiblePos(RenderRegionPos rrp, Vec3 cam) {
			return Math.abs(rrp.x - ((int) cam.x() >> REGION_SHIFT)) <= VIEW_RADIUS && Math.abs(rrp.z - ((int) cam.z() >> REGION_SHIFT)) <= VIEW_RADIUS;
		}

		@SuppressWarnings("unchecked")
		public static void render(LevelTerrainRenderContext wrc) {
			ProfilerFiller profiler = Profiler.get();
			profiler.push("glowcase:baked_block_entity_rendering");

			Vec3 cam = wrc.camera().getPosition();

			if (!needsRebuild.isEmpty()) {
				profiler.push("rebuild");

				// Make builders for regions that are marked for rebuild, render and upload to RegionBuffers
				Set<RenderRegionPos> removing = Sets.newHashSet();
				List<BlockEntity> blockEntities = new ArrayList<>();
				PoseStack bakeMatrices = new PoseStack();
				for (RenderRegionPos rrp : needsRebuild) {
					if (isVisiblePos(rrp, cam)) {
						// For the current region, rebuild each render layer using the buffer builders
						// Find all block entities in this region
						for (int chunkX = rrp.x << REGION_FROMCHUNK_SHIFT; chunkX < (rrp.x + 1) << REGION_FROMCHUNK_SHIFT; chunkX++) {
							for (int chunkZ = rrp.z << REGION_FROMCHUNK_SHIFT; chunkZ < (rrp.z + 1) << REGION_FROMCHUNK_SHIFT; chunkZ++) {
								blockEntities.addAll(wrc.world().getChunk(chunkX, chunkZ).getBlockEntities().values());
							}
						}

						if (!blockEntities.isEmpty()) {
							boolean bakedAnything = false;

							for (BlockEntity be : blockEntities) {
								if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be) instanceof BakedBlockEntityRenderer renderer && renderer.shouldBake(be)) {
									BlockPos pos = be.getBlockPos();
									bakeMatrices.pushPose();
									bakeMatrices.translate(pos.getX() & MAX_XZ_IN_REGION, pos.getY(), pos.getZ() & MAX_XZ_IN_REGION);
									try {
										renderer.renderBaked(be, bakeMatrices, vcp, LevelRenderer.getLightColor(wrc.world(), pos), OverlayTexture.NO_OVERLAY);
										bakedAnything = true;
									} catch (Throwable t) {
										LOGGER.error("Block entity renderer threw exception during baking: ", t);
									}
									bakeMatrices.popPose();
								}
							}

							blockEntities.clear();

							if (bakedAnything) {
								RegionBuffer buf = regions.computeIfAbsent(rrp, k -> new RegionBuffer());
								buf.reset();
								vcp.builders.forEach(buf::upload);
								vcp.reset();
							} else {
								removing.add(rrp);
							}
						} else {
							removing.add(rrp);
						}
					}
				}
				// We've processed all pending rebuilds now
				needsRebuild.clear();
				// These regions no longer contain anything
				removing.forEach(rrp -> {
					RegionBuffer buf = regions.get(rrp);
					if (buf != null) {
						buf.reset();
						regions.remove(rrp, buf);
					}
				});

				profiler.pop();
			}

			if (!regions.isEmpty()) {
				profiler.push("render");

				/*
				 * Set the fog end to an extremely high value, this is a total hack but.
				 * It's needed to make fog not bleed into text blocks
				 */
				GpuBufferSlice originalFog = RenderSystem.getShaderFog();
				RenderSystem.setShaderFog(((GameRendererAccessor) wrc.gameRenderer()).getFogRenderer().getBuffer(FogRenderer.FogMode.NONE));
				// Iterate over all RegionBuffers, render visible and remove non-visible RegionBuffers
				PoseStack matrices = wrc.matrixStack();
				matrices.pushPose();
				matrices.mulPose(wrc.positionMatrix());
				matrices.translate(-cam.x, -cam.y, -cam.z);
				var iter = regions.object2ReferenceEntrySet().iterator();
				while (iter.hasNext()) {
					var entry = iter.next();
					RenderRegionPos rrp = entry.getKey();
					RegionBuffer regionBuffer = entry.getValue();
					if (isVisiblePos(entry.getKey(), cam)) {
						// Iterate over used render layers in the region, render them
						matrices.pushPose();
						matrices.translate(rrp.origin.getX(), rrp.origin.getY(), rrp.origin.getZ());
						for (RenderType l : regionBuffer.uploadedLayers) {
							regionBuffer.render(l, matrices);
						}
						matrices.popPose();
					} else {
						regionBuffer.reset();
						iter.remove();
					}
				}
				RenderSystem.setShaderFog(originalFog);
				matrices.popPose();

				profiler.pop();
			}

			//RenderSystem.setShaderColor(1, 1, 1, 1);

			profiler.pop();
		}

		public static void activateRegion(BlockPos pos) {
			RenderRegionPos rrp = new RenderRegionPos(pos);
			if (!regions.containsKey(rrp)) {
				markForRebuild(pos);
			}
		}

		public static void reset() {
			regions.values().forEach(RegionBuffer::reset);
			regions.clear();
			needsRebuild.clear();
		}
	}
}
