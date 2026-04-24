package dev.hephaestus.glowcase.client.render.bakedbe.level;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedBERenderDispatcher;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedMeshes;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.*;
import dev.hephaestus.glowcase.mixin.client.bakedbe.LevelRendererAccessor;
import dev.hephaestus.glowcase.mixin.client.bakedbe.RenderTypeAccessor;
import dev.hephaestus.glowcase.mixinsupport.LevelRendererExtension;
import dev.hephaestus.glowcase.util.DefaultedMap;
import dev.hephaestus.glowcase.util.DefaultedMapBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NullMarked
public class GlowcaseLevelRenderer implements Closeable {
	public static final Logger LOGGER = LoggerFactory.getLogger(GlowcaseLevelRenderer.class);
	private final VisibleSections visibleSections = new VisibleSections();
	private final LevelRenderer levelRenderer;
	private final ThreadLocal<BakedBERenderDispatcher> renderDispatchers = ThreadLocal.withInitial(BakedBERenderDispatcher.Builder::createBakedBERenderDispatcher);
	private @Nullable GlowcaseSectionRenderDispatcher sectionRenderDispatcher;
	private @Nullable GlowcaseSectionsToRender sectionsToRender;

	public GlowcaseLevelRenderer(LevelRenderer levelRenderer) {
		this.levelRenderer = levelRenderer;
	}

	public static GlowcaseLevelRenderer getInstance() {
		return ((LevelRendererExtension) Minecraft.getInstance().levelRenderer).glowcase$getLevelRenderer();
	}

	public static GlowcaseLevelRenderer getInstance(LevelRenderer levelRenderer) {
		return ((LevelRendererExtension) levelRenderer).glowcase$getLevelRenderer();
	}

	public void setLevel(@Nullable final ClientLevel level) {
		if (level != null) return;

		if (sectionRenderDispatcher != null) {
			sectionRenderDispatcher.close();
		}

		sectionRenderDispatcher = null;
		this.visibleSections.reset();
	}

	public void allChanged() {
		if (sectionRenderDispatcher == null) {
			this.sectionRenderDispatcher = new GlowcaseSectionRenderDispatcher();
		}

		sectionRenderDispatcher.clearCompileQueue();
		this.visibleSections.reset();
	}

	public void applyPendingSectionMapChanges() {
		visibleSections.update();
	}

	public void prepareRenders(final Matrix4fc modelViewMatrix, final Camera camera) {
		if (sectionRenderDispatcher == null) {
			this.sectionsToRender = null;
			return;
		}

		var iterator = this.visibleSections.iterator();
		int largestIndexCount = 0;
		List<DynamicUniforms.Transform> transforms = new ObjectArrayList<>();
		RenderTypeGroups.Builder renderGroups = new RenderTypeGroups.Builder();
		DefaultedMapBase<RenderType, Map<Integer, List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroups = DefaultedMap.openHashMap(
			_ -> new DefaultedMap<>(
				new Int2ObjectOpenHashMap<>(),
				_ -> new ArrayList<>()
			)
		);

		this.sectionRenderDispatcher.lock();

		try {
			try (Zone ignored = Profiler.get().zone("Upload Global Buffers")) {
				this.sectionRenderDispatcher.uploadGlobalGeomBuffersToGPU();
			}

			while (iterator.hasNext()) {
				final var sectionEntry = iterator.next();
				final long sectionNode = sectionEntry.sectionNode();
				final RenderSectionPos sectionPos = new RenderSectionPos(sectionNode);
				final var sectionInfo = sectionEntry.sectionInfo();
				int uboIndex = -1;

				for (GlowcaseRenderSectionInfo.DrawEntry drawEntry : sectionInfo) {
					RenderType renderType = drawEntry.renderType();
					SectionMesh.SectionDraw draw = drawEntry.draw();
					boolean translucent = drawEntry.translucent();
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

					(translucent ? renderGroups.sorted : renderGroups.unsorted).add(renderType);

					int combinedHash = 173;
					VertexFormat vertexFormat = renderType.pipeline().getVertexFormat();
					GpuBuffer vertexBuffer = slice.vertexBuffer();
					if (translucent) {
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
						if (translucent) {
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
		this.sectionsToRender = new GlowcaseSectionsToRender(new RenderTypeGroups(renderGroups), drawGroups, largestIndexCount, sectionTransforms);
	}

	public void renderGroup(final boolean sorted) {
		if (sectionsToRender == null) return;

		ProfilerFiller profiler = Profiler.get();
		profiler.push("glowcase");
		sectionsToRender.renderGroup(sorted);
		profiler.pop();
	}

	public void allocateSectionMeshes(long section, BakedMeshes meshes) {
		if (sectionRenderDispatcher == null) {
			meshes.close();
			return;
		}

		visibleSections.setSectionDraws(section, meshes);
		for (BakedMeshes.Entry entry : meshes) {
			boolean success = sectionRenderDispatcher.allocateMeshBuffers(section, entry.renderType(), entry.mesh().meshData());
			entry.close();
			if (!success) {
				throw new IllegalStateException("Failed to allocate mesh buffers, possible resource leak or the mesh is too complex");
			}
		}
	}

	public void updateIndexBuffer(long sectionNode, RenderType renderType, ByteBuffer indexBuffer) {
		if (sectionRenderDispatcher == null) return;

		boolean success = sectionRenderDispatcher.allocateBuffers(sectionNode, renderType, null, indexBuffer);
		if (!success) {
			throw new IllegalStateException("Failed to allocate mesh buffers, possible resource leak or the mesh is too complex");
		}
	}

	public void releaseSection(long section) {
		if (!visibleSections.isVisible(section)) return;

		if (sectionRenderDispatcher != null) {
			sectionRenderDispatcher.releaseSection(section);
		}

		// Remove from visible sections after removing from the dispatcher to make sure the allocation queue doesn't crash
		visibleSections.remove(section);
	}

	public VisibleSections visibleSections() {
		return visibleSections;
	}

	public void compilePendingSections() {
		if (sectionRenderDispatcher == null) return;
		sectionRenderDispatcher.compileQueue.allocatePending();
	}

	@Contract("_, !null, null -> fail; _, _, _ -> _")
	public void queueCompilation(long sectionPos, @Nullable SubmitNodeStorage nodeStorage, @Nullable VertexSorting vertexSorting) {
		if (sectionRenderDispatcher == null) return;

		if (nodeStorage == null) {
			releaseSection(sectionPos);
			return;
		}

		assert vertexSorting != null;

		visibleSections.addIfAbsent(sectionPos);

		try {
			sectionRenderDispatcher.compileQueue.compile(sectionPos, nodeStorage, vertexSorting);
		} catch (Exception e) {
			throw new ReportedException(CrashReport.forThrowable(e, "Compiling baked BE"));
		}
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

	public BakedBERenderDispatcher renderDispatcher() {
		return renderDispatchers.get();
	}
}
