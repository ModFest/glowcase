package dev.hephaestus.glowcase.client.render.bakedbe.level;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.GlowcaseRenderSectionInfo;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.GlowcaseSectionRenderDispatcher;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.GlowcaseSectionsToRender;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.RenderSectionPos;
import dev.hephaestus.glowcase.mixin.client.FeatureRenderDispatcherAccessor;
import dev.hephaestus.glowcase.mixin.client.LevelRendererAccessor;
import dev.hephaestus.glowcase.mixin.client.RenderTypeAccessor;
import dev.hephaestus.glowcase.util.DefaultedMap;
import dev.hephaestus.glowcase.util.DefaultedMapBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@NullMarked
public class GlowcaseLevelRenderer implements Closeable {
	public static final Logger LOGGER = LoggerFactory.getLogger(GlowcaseLevelRenderer.class);
	@SuppressWarnings("NotNullFieldNotInitialized")
	private static GlowcaseLevelRenderer instance;
	private final RenderBuffers renderBuffers = new RenderBuffers(0);
	// Use the section count for render distance 12
	// https://minecraft.wiki/w/Options#Video_Settings
	private final Long2ObjectOpenHashMap<GlowcaseRenderSectionInfo> visibleSections = new Long2ObjectOpenHashMap<>(10_000);
	private final Minecraft client;
	private final LevelRenderer levelRenderer;
	private @Nullable GlowcaseSectionRenderDispatcher sectionRenderDispatcher;
	private @Nullable GlowcaseSectionsToRender sectionsToRender;
	private int lastRenderDistance;

	public GlowcaseLevelRenderer() {
		this.client = Minecraft.getInstance();
		this.levelRenderer = client.levelRenderer;
		instance = this;
	}

	public static GlowcaseLevelRenderer getInstance() {
		return instance;
	}

	/// Call only from mixin into LevelRenderer#setLevel
	@ApiStatus.Internal
	public void setLevel(@Nullable final ClientLevel level) {
		if (level != null) return;

		if (sectionRenderDispatcher != null) {
			sectionRenderDispatcher.close();
		}

		sectionRenderDispatcher = null;
		this.visibleSections.clear();
	}

	public void allChanged() {
		if (sectionRenderDispatcher == null) {
			this.sectionRenderDispatcher = new GlowcaseSectionRenderDispatcher();
		}

		sectionRenderDispatcher.clearCompileQueue();

		this.visibleSections.clear();
		int renderDistance = this.client.options.getEffectiveRenderDistance();
		if (renderDistance != lastRenderDistance) {
			// radius = render distance
			// diameter = radius * 2 + 1
			// total chunks = diameter^2
			// total sections = total chunks * 16
			// https://minecraft.wiki/w/Options#Video_Settings
			// To overflow this you need a radius of 5793 chunks. Good luck getting that to even load to RAM
			int totalSections = Math.powExact(renderDistance * 2 + 1, 2) * 16;
			if (lastRenderDistance < renderDistance) {
				this.visibleSections.ensureCapacity(totalSections);
			} else {
				this.visibleSections.trim(totalSections);
			}

			this.lastRenderDistance = renderDistance;
		}
	}

	@SuppressWarnings("resource")
	public void prepareRenders(final Matrix4fc modelViewMatrix, final Camera camera) {
		if (sectionRenderDispatcher == null) {
			this.sectionsToRender = null;
			return;
		}

		ProfilerFiller profiler = Profiler.get();
		profiler.push("glowcase");
		ObjectIterator<Long2ObjectMap.Entry<GlowcaseRenderSectionInfo>> iterator = this.visibleSections.long2ObjectEntrySet().fastIterator();
		DefaultedMapBase<RenderType, Map<Integer, List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroups = DefaultedMap.openHashMap(
			_ -> new DefaultedMap<>(
				new Int2ObjectOpenHashMap<>(),
				_ -> new ArrayList<>()
			)
		);
		int largestIndexCount = 0;
		List<DynamicUniforms.Transform> transforms = new ObjectArrayList<>();

		this.sectionRenderDispatcher.lock();

		try {

			try (Zone ignored = Profiler.get().zone("Upload Global Buffers")) {
				this.sectionRenderDispatcher.uploadGlobalGeomBuffersToGPU();
			}

			while (iterator.hasNext()) {
				final var entry = iterator.next();
				final long sectionNode = entry.getLongKey();
				final RenderSectionPos sectionPos = new RenderSectionPos(sectionNode);
				final var sectionInfo = entry.getValue();
				int uboIndex = -1;

				for (RenderType renderType : sectionRenderDispatcher.renderTypes()) {
					SectionMesh.SectionDraw draw = sectionInfo.getSectionDraw(renderType);
					SectionRenderDispatcher.RenderSectionBufferSlice slice = this.sectionRenderDispatcher.getRenderSectionSlice(sectionNode, renderType);
					if (slice == null || draw == null || (draw.hasCustomIndexBuffer() && slice.indexBuffer() == null)) continue;
					if (uboIndex == -1) {
						uboIndex = transforms.size();

						Vector3f renderOffset = camera.position()
							.subtract(Vec3.atLowerCornerOf(sectionPos.origin()))
							.toVector3f()
							.mul(-1);

						transforms.add(
							new DynamicUniforms.Transform(
								new Matrix4f(modelViewMatrix).translate(renderOffset),
								new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
								new Vector3f(),
								((RenderTypeAccessor) renderType).getState().textureTransform.getMatrix()
							)
						);
					}

					int combinedHash = 173;
					VertexFormat vertexFormat = renderType.pipeline().getVertexFormat();
					GpuBuffer vertexBuffer = slice.vertexBuffer();
					if (renderType.sortOnUpload()) {
						combinedHash = 31 * combinedHash + vertexBuffer.hashCode();
					}

					int firstIndex = 0;
					GpuBuffer indexBuffer;
					VertexFormat.IndexType indexType;
					if (!draw.hasCustomIndexBuffer()) {
						if (draw.indexCount() > largestIndexCount) {
							largestIndexCount = draw.indexCount();
						}

						indexBuffer = null;
						indexType = null;
					} else {
						indexBuffer = slice.indexBuffer();
						indexType = draw.indexType();
						if (renderType.sortOnUpload()) {
							combinedHash = 31 * combinedHash + indexBuffer.hashCode();
							combinedHash = 31 * combinedHash + indexType.hashCode();
						}

						firstIndex = (int) (slice.indexBufferOffset() / indexType.bytes);
					}

					int baseVertex = (int) (slice.vertexBufferOffset() / vertexFormat.getVertexSize());
					var draws = drawGroups.getValue(renderType).computeIfAbsent(combinedHash, _ -> new ArrayList<>());

					int finalUboIndex = uboIndex;
					draws.add(new RenderPass.Draw<>(
						0,
						vertexBuffer,
						indexBuffer,
						indexType,
						firstIndex,
						draw.indexCount(),
						baseVertex,
						(transformUbo, uploader) -> uploader.upload("DynamicTransforms", transformUbo[finalUboIndex])
					));
				}
			}
		} finally {
			this.sectionRenderDispatcher.unlock();
		}

		GpuBufferSlice[] sectionTransforms = RenderSystem.getDynamicUniforms().writeTransforms(transforms.toArray(new DynamicUniforms.Transform[0]));
		this.sectionsToRender = new GlowcaseSectionsToRender(drawGroups, largestIndexCount, sectionTransforms);
		profiler.pop();
	}

	public void renderGroup(final boolean sorted) {
		if (sectionsToRender == null) return;

		ProfilerFiller profiler = Profiler.get();
		profiler.push("glowcase");
		sectionsToRender.renderGroup(sorted);
		profiler.pop();
	}

	public void createUberBuffers(Collection<RenderType> renderTypes) {
		if (sectionRenderDispatcher == null) return;

		for (RenderType renderType : renderTypes) {
			sectionRenderDispatcher.assertRenderTypeBuffer(renderType);
		}
	}

	public void allocateSectionMeshes(long section, Map<RenderType, MeshData> meshes) {
		if (sectionRenderDispatcher == null) {
			for (MeshData meshData : meshes.values()) meshData.close();
			return;
		}

		visibleSections.get(section).setSectionDraws(meshes);
		for (Map.Entry<RenderType, MeshData> entry : meshes.entrySet()) {
			RenderType renderType = entry.getKey();
			MeshData meshData = entry.getValue();

			boolean success = sectionRenderDispatcher.allocateMeshBuffers(section, renderType, meshData);
			meshData.close();
			if (!success) {
				throw new IllegalStateException("Failed to allocate mesh buffers, possible resource leak or the mesh is too complex!");
			}
		}
	}

	private void addSection(long section) {
		visibleSections.put(section, new GlowcaseRenderSectionInfo());
	}

	public void releaseSection(long section) {
		if (!visibleSections.containsKey(section)) return;

		if (sectionRenderDispatcher != null) {
			sectionRenderDispatcher.releaseSection(section);
		}

		// Remove from visible sections after removing from the dispatcher to make sure the allocation queue doesn't crash
		visibleSections.remove(section);
	}

	public RenderBuffers getRenderBuffers() {
		return renderBuffers;
	}

	public void compilePendingSections() {
		if (sectionRenderDispatcher == null) return;
		sectionRenderDispatcher.compileQueue.compilePending();
	}

	public void queueCompilation(long sectionPos, @Nullable SubmitNodeStorage nodeStorage) {
		if (sectionRenderDispatcher == null) return;

		if (nodeStorage == null) {
			releaseSection(sectionPos);
			return;
		}

		if (!visibleSections.containsKey(sectionPos)) addSection(sectionPos);

		sectionRenderDispatcher.compileQueue.enqueue(sectionPos, nodeStorage);
	}

	public void setBlockDirty(BlockPos blockPos, boolean playerChanged) {
		((LevelRendererAccessor) levelRenderer).callSetSectionDirty(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getY()), SectionPos.blockToSectionCoord(blockPos.getZ()), playerChanged);
	}

	public @Nullable GlowcaseSectionRenderDispatcher getSectionRenderDispatcher() {
		return sectionRenderDispatcher;
	}

	@Override
	public void close() {
		if (this.sectionRenderDispatcher != null) {
			this.sectionRenderDispatcher.close();
		}
	}

	public FeatureRenderDispatcher createFeatureRenderDispatcher(final SubmitNodeStorage nodeStorage) {
		GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
		FeatureRenderDispatcherAccessor renderDispatcherAccessor = (FeatureRenderDispatcherAccessor) gameRenderer.getFeatureRenderDispatcher();
		return new FeatureRenderDispatcher(
			nodeStorage,
			renderDispatcherAccessor.getModelManager(),
			renderBuffers.bufferSource(),
			Minecraft.getInstance().getAtlasManager(),
			renderBuffers.outlineBufferSource(),
			renderBuffers.crumblingBufferSource(),
			renderDispatcherAccessor.getFont(),
			gameRenderer.getGameRenderState()
		);
	}
}
