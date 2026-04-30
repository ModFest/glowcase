package dev.hephaestus.glowcase.client.render.bakedbe.section;

import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.hephaestus.glowcase.mixin.client.bakedbe.RenderTypeAccessor;
import dev.hephaestus.glowcase.util.DefaultedMap;
import dev.hephaestus.glowcase.util.DefaultedMapBase;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSectionBufferSlice;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import org.joml.Vector3fc;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static dev.hephaestus.glowcase.util.SizeConstants.Mi;

@NullMarked
public class GlowcaseSectionRenderDispatcher implements Closeable {
	private final DefaultedMapBase<RenderType, SectionUberBuffers> layerBuffers;
	private final ReentrantLock copyLock = new ReentrantLock();
	public final SectionCompileQueue compileQueue = new SectionCompileQueue();

	public GlowcaseSectionRenderDispatcher() {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		GraphicsWorkarounds workarounds = GraphicsWorkarounds.get(gpuDevice);
		this.layerBuffers = DefaultedMap.openHashMap(renderType -> createUberBuffers(renderType, gpuDevice, workarounds));
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

	public void uploadGlobalGeomBuffersToGPU() {
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

	public boolean allocateMeshBuffers(long sectionPos, RenderType renderType, MeshData meshData) {
		return allocateBuffers(sectionPos, renderType, meshData.vertexBuffer(), meshData.indexBuffer());
	}

	public boolean allocateBuffers(long sectionPos, RenderType renderType, @Nullable ByteBuffer vertexBuffer, @Nullable ByteBuffer indexBuffer) {
		ProfilerFiller profiler = Profiler.get();
		String renderTypeName = ((RenderTypeAccessor) renderType).getName();
		profiler.push(renderTypeName);
		lock();

		boolean success = true;

		try {
			SectionUberBuffers sectionBuffers = layerBuffers.getValue(renderType);

			if (vertexBuffer != null) {
				success &= sectionBuffers.vertexBuffer.addAllocation(sectionPos, null, vertexBuffer);
			}

			if (indexBuffer != null) {
				success &= sectionBuffers.indexBuffer.addAllocation(sectionPos, null, indexBuffer);
			}

			if (!success && RenderSystem.isOnRenderThread()) {
				uploadGlobalGeomBuffersToGPU();
			}
		} finally {
			unlock();
			profiler.pop();
		}

		return success;
	}

	public void assertRenderTypeBuffer(RenderType renderType) {
		layerBuffers.assertPresent(renderType);
	}

	public boolean hasAllRenderTypes(Set<RenderType> renderTypes) {
		return renderTypes().containsAll(renderTypes);
	}

	public Set<RenderType> renderTypes() {
		return layerBuffers.keySet();
	}

	public void releaseSection(long section) {
		lock();
		try {
			this.compileQueue.remove(section);

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

	public void clearCompileQueue() {
		this.compileQueue.clear();
	}

	public void close() {
		lock();

		try {
			this.compileQueue.close();
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

		Optional<BlendFunction> blendFunction = renderType.pipeline().getColorTargetState().blendFunction();
		boolean isTranslucent = blendFunction.isPresent() && blendFunction.get().equals(BlendFunction.TRANSLUCENT);
		UberGpuBuffer<Long> indexUberBuffer = isTranslucent ?
			new UberGpuBuffer<>(renderTypeName, GpuBuffer.USAGE_INDEX, 128 * Mi, 8, gpuDevice, 2 * Mi, workarounds) :
			null;

		return new SectionUberBuffers(vertexUberBuffer, indexUberBuffer);
	}

	private record SectionUberBuffers(UberGpuBuffer<Long> vertexBuffer, @Nullable UberGpuBuffer<Long> indexBuffer) {}
}
