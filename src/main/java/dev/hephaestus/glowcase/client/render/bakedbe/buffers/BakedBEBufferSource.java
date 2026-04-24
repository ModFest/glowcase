package dev.hephaestus.glowcase.client.render.bakedbe.buffers;

import com.mojang.blaze3d.vertex.*;
import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh;
import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh.Sorter;
import net.caffeinemc.mods.sodium.client.util.sorting.VertexSorters;
import net.caffeinemc.mods.sodium.client.util.sorting.VertexSortingExtended;
import net.caffeinemc.mods.sodium.mixin.features.render.immediate.buffer_builder.sorting.MeshDataAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;

@NullMarked
public class BakedBEBufferSource extends MultiBufferSource.BufferSource {
	private static final boolean HAS_SODIUM = FabricLoader.getInstance().isModLoaded("sodium");
	private static final DummyByteBufferBuilder sharedBuffer = new DummyByteBufferBuilder();
	protected final Map<RenderType, ByteBufferBuilder> startedBuffers = new HashMap<>();
	private final Function<RenderType, ByteBufferBuilder> bufferSupplier;

	protected BakedBEBufferSource(Function<RenderType, ByteBufferBuilder> bufferSupplier, SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers) {
		super(sharedBuffer, fixedBuffers);
		this.bufferSupplier = bufferSupplier;
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		return this.startedBuilders.computeIfAbsent(renderType, this::createBuilder);
	}

	private BufferBuilder createBuilder(RenderType renderType) {
		final ByteBufferBuilder buffer = createBuffer(renderType);
		this.startedBuffers.put(renderType, buffer);
		return new BufferBuilder(buffer, renderType.mode(), renderType.format());
	}

	private ByteBufferBuilder createBuffer(RenderType renderType) {
		ByteBufferBuilder buffer = this.fixedBuffers.get(renderType);
		if (buffer == null) {
			buffer = bufferSupplier.apply(renderType);
		}

		return buffer;
	}

	@Override
	protected void endBatch(RenderType type, BufferBuilder builder) {
		// We don't want to render here so just build and close the mesh
		MeshData mesh = builder.build();
		if (mesh != null) {
			mesh.close();
		}
	}

	public Map<RenderType, CompiledMesh> buildAllBatches(VertexSorting vertexSorting, boolean sort) {
		Map<RenderType, CompiledMesh> meshData = new HashMap<>();
		this.startedBuilders.forEach((renderType, bufferBuilder) -> {
			CompiledMesh mesh = this.buildBatch(renderType, bufferBuilder, vertexSorting, sort);
			if (mesh != null) meshData.put(renderType, mesh);
		});

		this.startedBuilders.clear();

		return meshData;
	}

	public @Nullable CompiledMesh buildBatch(RenderType renderType, BufferBuilder bufferBuilder, VertexSorting vertexSorting, boolean sort) {
		MeshData meshData = bufferBuilder.build();
		if (meshData == null) return null;

		Sorter sortState = null;

		if (sort) {
			ByteBufferBuilder buffer = this.startedBuffers.get(renderType);
			assert buffer != null;
			sortState = HAS_SODIUM ? useSodiumQuadSort(meshData, buffer, vertexSorting) : (Sorter) (Object) meshData.sortQuads(buffer, vertexSorting);
		}

		return new CompiledMesh(meshData, sortState);
	}

	// Having sodium classes here is fine, as it's only called when sodium is present
	private @Nullable Sorter useSodiumQuadSort(final MeshData meshData, final ByteBufferBuilder indexBufferTarget, final VertexSorting sorting) {
		if (sorting instanceof VertexSortingExtended sortingExtended) {
			CompiledMesh.SodiumSortState sortState = CompiledMesh.SodiumSortState.create(meshData);
			if (sortState != null) {
				((MeshDataAccessor) meshData).sodium$setIndexBuffer(sortState.buildSortedIndexBuffer(indexBufferTarget, sortingExtended));
			}
			return sortState;
		}

		return (Sorter) (Object) meshData.sortQuads(indexBufferTarget, sorting);
	}
}
