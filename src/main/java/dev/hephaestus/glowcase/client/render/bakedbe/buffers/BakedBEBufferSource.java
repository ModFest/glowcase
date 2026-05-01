package dev.hephaestus.glowcase.client.render.bakedbe.buffers;

import com.mojang.blaze3d.vertex.*;
import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh;
import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh.Sorter;
import dev.hephaestus.glowcase.util.collections.ObjectPairArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.caffeinemc.mods.sodium.client.util.sorting.VertexSortingExtended;
import net.caffeinemc.mods.sodium.mixin.features.render.immediate.buffer_builder.sorting.MeshDataAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;

import static dev.hephaestus.glowcase.util.SizeConstants.Mi;

@NullMarked
public class BakedBEBufferSource extends MultiBufferSource.BufferSource {
	private static final boolean HAS_SODIUM = FabricLoader.getInstance().isModLoaded("sodium");
	private static final DummyByteBufferBuilder sharedBuffer = new DummyByteBufferBuilder();
	protected final Map<RenderType, ByteBufferBuilder> startedBuffers = new Object2ObjectLinkedOpenHashMap<>();
	protected final Map<RenderType, BufferBuilder> startedBuilders = new Object2ObjectLinkedOpenHashMap<>();
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

	public ObjectPairArrayList<RenderType, CompiledMesh> buildAllBatches(@Nullable VertexSorting vertexSorting) throws MeshTooComplex {
		ObjectPairArrayList<RenderType, CompiledMesh> meshData = new ObjectPairArrayList<>();

		for (Map.Entry<RenderType, BufferBuilder> entry : this.startedBuilders.entrySet()) {
			RenderType renderType = entry.getKey();
			BufferBuilder bufferBuilder = entry.getValue();

			try {
				CompiledMesh mesh = this.buildBatch(renderType, bufferBuilder, vertexSorting);
				if (mesh == null) continue;

				int meshSize = mesh.meshData().drawState().vertexCount() * renderType.format().getVertexSize();
				if (meshSize > 32 * Mi) {
					mesh.close();
					throw abort(meshData);
				}

				meshData.add(renderType, mesh);
			} catch (IllegalArgumentException e) {
				if (!e.getStackTrace()[0].getClassName().equals(ByteBufferBuilder.class.getName())) throw e;
				throw abort(e, meshData);
			}
		}

		this.startedBuilders.clear();

		return meshData;
	}

	public @Nullable CompiledMesh buildBatch(RenderType renderType, BufferBuilder bufferBuilder, @Nullable VertexSorting vertexSorting) {
		MeshData meshData = bufferBuilder.build();
		if (meshData == null) return null;

		Sorter sortState = null;

		if (renderType.hasBlending() && vertexSorting != null) {
			ByteBufferBuilder buffer = this.startedBuffers.get(renderType);
			assert buffer != null;
			sortState = HAS_SODIUM ? useSodiumQuadSort(meshData, buffer, vertexSorting) : (Sorter) (Object) meshData.sortQuads(buffer, vertexSorting);
		}

		return new CompiledMesh(meshData, sortState);
	}

	// Having sodium classes here is fine, as it's only called when sodium is present
	@Deprecated
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

	@SafeVarargs
	public final MeshTooComplex abort(@Nullable ObjectPairArrayList<RenderType, CompiledMesh>... built) {
		return abort(null, built);
	}

	@SafeVarargs
	public final MeshTooComplex abort(@Nullable Exception cause, @Nullable ObjectPairArrayList<RenderType, CompiledMesh>... built) {
		for (var meshes : built) {
			if (meshes != null) meshes.forEachRight(CompiledMesh::close);
		}

		this.startedBuffers.values().forEach(ByteBufferBuilder::discard);
		this.startedBuilders.clear();

		if (cause == null) return new MeshTooComplex();
		if (cause instanceof MeshTooComplex e) return e;
		return new MeshTooComplex(cause);
	}
}
