package dev.hephaestus.glowcase.client.render.bakedbe.section;

import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedMeshes;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.render.bakedbe.level.VisibleSections;
import dev.hephaestus.glowcase.mixin.client.bakedbe.RenderTypeAccessor;
import dev.hephaestus.glowcase.util.DataFlow;
import dev.hephaestus.glowcase.util.DefaultedMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSectionBufferSlice;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import org.joml.Vector3fc;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static dev.hephaestus.glowcase.util.SizeConstants.Mi;

@NullMarked
public class GlowcaseSectionRenderDispatcher implements Closeable {
	private final UberBuffers layerBuffers;
	private final ReentrantLock copyLock = new ReentrantLock();
	private final GlowcaseLevelRenderer levelRenderer;
	private transient boolean closed;

	public GlowcaseSectionRenderDispatcher(GlowcaseLevelRenderer levelRenderer) {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		GraphicsWorkarounds workarounds = GraphicsWorkarounds.get(gpuDevice);
		this.layerBuffers = new UberBuffers(renderType -> createUberBuffers(renderType, gpuDevice, workarounds));
		this.levelRenderer = levelRenderer;
	}

	public static VertexSorting createVertexSorting(final SectionPos sectionPos, final Vec3 cameraPos) {
		return VertexSorting.byDistance(
			(float)(cameraPos.x - sectionPos.minBlockX()), (float)(cameraPos.y - sectionPos.minBlockY()), (float)(cameraPos.z - sectionPos.minBlockZ())
		);
	}

	public static VertexSorting createVertexSorting(final SectionPos sectionPos, final Vector3dc cameraPos) {
		return VertexSorting.byDistance(
			(float)(cameraPos.x() - sectionPos.minBlockX()), (float)(cameraPos.y() - sectionPos.minBlockY()), (float)(cameraPos.z() - sectionPos.minBlockZ())
		);
	}

	public static VertexSorting createVertexSorting(final Vector3fc relativePos) {
		// There is a method that takes Vector3fc but for some reason sodium doesn't handle it like it does for the 3 param one
		return VertexSorting.byDistance(relativePos.x(), relativePos.y(), relativePos.z());
	}

	public void update() {
		layerBuffers.addPending();
	}

	public void uploadGlobalGeomBuffersToGPU() {
		if (closed) return;

		CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
		boolean performedBufferResize = false;

		for (SectionUberBuffers buffers : this.layerBuffers.values()) {
			var vertexBuffer = buffers.vertexBuffer;
			if (performedBufferResize) {
				break;
			}

			performedBufferResize = vertexBuffer.uploadStagedAllocations(RenderSystem.getDevice(), commandEncoder);
			var indexBuffer = buffers.indexBuffer;
			if (indexBuffer != null) {
				indexBuffer.uploadStagedAllocations(RenderSystem.getDevice(), commandEncoder);
			}
		}
	}

	public @Nullable RenderSectionBufferSlice getRenderSectionSlice(final long section, final RenderType renderType) {
		SectionUberBuffers uberBuffers = this.layerBuffers.getValue(renderType);
		TlsfAllocator.Allocation vertexSlice = uberBuffers.vertexBuffer.getAllocation(section);
		if (vertexSlice == null) {
			return null;
		} else {
			long vertexBufferOffset = vertexSlice.getOffsetFromHeap();
			TlsfAllocator.Allocation indexSlice = uberBuffers.indexBuffer != null ? uberBuffers.indexBuffer.getAllocation(section) : null;
			long indexBufferOffset = 0L;
			GpuBuffer indexBuffer = null;
			if (indexSlice != null) {
				indexBufferOffset = indexSlice.getOffsetFromHeap();
				indexBuffer = uberBuffers.indexBuffer.getGpuBuffer(indexSlice);
			}

			return new RenderSectionBufferSlice(uberBuffers.vertexBuffer.getGpuBuffer(vertexSlice), vertexBufferOffset, indexBuffer, indexBufferOffset);
		}
	}

	public boolean allocateIndexBuffers(long sectionNode, RenderType renderType, ByteBuffer indexBuffer) {
		return allocateBuffers(sectionNode, renderType, null, indexBuffer, null);
	}

	public boolean allocateMeshBuffers(long sectionPos, RenderType renderType, MeshData meshData, @Nullable UberBufferCallbacks callbacks) {
		return allocateBuffers(sectionPos, renderType, meshData.vertexBuffer(), meshData.indexBuffer(), callbacks);
	}

	public boolean allocateBuffers(long sectionPos, RenderType renderType, @Nullable ByteBuffer vertexBuffer, @Nullable ByteBuffer indexBuffer, @Nullable UberBufferCallbacks callbacks) {
		if (closed) return true;

		lock();

		boolean success = true;

		try {
			SectionUberBuffers sectionBuffers = layerBuffers.getWithoutDefault(renderType);
			if (sectionBuffers == null) {
				layerBuffers.queueBuffer(renderType);
				return false;
			}

			if (vertexBuffer != null) {
				var callback = DataFlow.nullable(callbacks, ubCallbacks -> ubCallbacks.vertexCallback(renderType));
				success &= sectionBuffers.vertexBuffer.addAllocation(sectionPos, callback, vertexBuffer);
			}

			var callback = DataFlow.nullable(callbacks, ubCallbacks -> ubCallbacks.indexCallback(renderType));
			if (indexBuffer != null && sectionBuffers.indexBuffer != null) {
				success &= sectionBuffers.indexBuffer.addAllocation(sectionPos, callback, indexBuffer);
			} else if (callback != null) {
				callback.bufferHasBeenUploaded(sectionPos);
			}

			if (!success && RenderSystem.isOnRenderThread()) {
				uploadGlobalGeomBuffersToGPU();
			}
		} finally {
			unlock();
		}

		return success;
	}

	public void releaseSection(long section) {
		lock();
		try {
			for (SectionUberBuffers buffers : layerBuffers.values()) {
				var vertexBuffer = buffers.vertexBuffer;
				vertexBuffer.removeAllocation(section);
				var indexBuffer = buffers.indexBuffer;
				if (indexBuffer != null) {
					indexBuffer.removeAllocation(section);
				}
			}
		} finally {
			unlock();
		}
	}

	public void lock() {
		this.copyLock.lock();
	}

	public void unlock() {
		this.copyLock.unlock();
	}

	public void close() {
		closed = true;

		lock();

		try {
			for (SectionUberBuffers buffers : this.layerBuffers.values()) {
				buffers.vertexBuffer.close();
				if (buffers.indexBuffer != null) {
					buffers.indexBuffer.close();
				}
			}
		} finally {
			unlock();
		}
	}

	private static SectionUberBuffers createUberBuffers(RenderType renderType, GpuDevice gpuDevice, GraphicsWorkarounds workarounds) {
		String renderTypeName = ((RenderTypeAccessor) renderType).getName();
		VertexFormat vertexFormat = renderType.pipeline().getVertexFormat();
		UberGpuBuffer<Long> vertexUberBuffer = new UberGpuBuffer<>(
			renderTypeName,
			GpuBuffer.USAGE_VERTEX,
			128 * Mi,
			vertexFormat.getVertexSize(),
			gpuDevice,
			32 * Mi,
			workarounds
		);

		UberGpuBuffer<Long> indexUberBuffer = renderType.hasBlending() ?
			new UberGpuBuffer<>(renderTypeName, GpuBuffer.USAGE_INDEX, 128 * Mi, 8, gpuDevice, 16 * Mi, workarounds) :
			null;

		return new SectionUberBuffers(vertexUberBuffer, indexUberBuffer);
	}

	private record SectionUberBuffers(UberGpuBuffer<Long> vertexBuffer, @Nullable UberGpuBuffer<Long> indexBuffer) {}

	private static class UberBuffers extends DefaultedMap<Object2ObjectOpenHashMap<RenderType, SectionUberBuffers>, RenderType, SectionUberBuffers> {
		private final ConcurrentLinkedDeque<RenderType> pending = new ConcurrentLinkedDeque<>();

		public UberBuffers(Function<RenderType, SectionUberBuffers> defaultValue) {
			super(new Object2ObjectOpenHashMap<>(), defaultValue);
		}

		public void addPending() {
			RenderSystem.assertOnRenderThread();
			while (!pending.isEmpty()) assertPresent(pending.poll());
		}

		public void queueBuffer(RenderType renderType) {
			if (RenderSystem.isOnRenderThread()) {
				this.assertPresent(renderType);
			} else {
				pending.add(renderType);
			}
		}
	}

	public static class UberBufferCallbacks {
		private final VisibleSections visibleSections;
		private final BakedMeshes bakedMeshes;
		private final Set<RenderType> pendingVertex = new HashSet<>();
		private final Set<RenderType> pendingIndex = new HashSet<>();

		public UberBufferCallbacks(VisibleSections visibleSections, BakedMeshes bakedMeshes) {
			this.visibleSections = visibleSections;
			this.bakedMeshes = bakedMeshes;
			for (BakedMeshes.Entry entry : bakedMeshes) {
				pendingVertex.add(entry.renderType());
				if (entry.hasCustomIndexBuffer()) {
					pendingIndex.add(entry.renderType());
				}
			}
		}

		public UberGpuBuffer.UploadCallback<Long> vertexCallback(RenderType renderType) {
			return node -> {
				pendingVertex.remove(renderType);
				checkBuffers(node);
			};
		}

		public UberGpuBuffer.UploadCallback<Long> indexCallback(RenderType renderType) {
			return node -> {
				pendingIndex.remove(renderType);
				checkBuffers(node);
			};
		}

		private void checkBuffers(long sectionNode) {
			if (!pendingVertex.isEmpty() || !pendingIndex.isEmpty()) return;
			visibleSections.setSectionDraws(sectionNode, bakedMeshes);
		}
	}
}
