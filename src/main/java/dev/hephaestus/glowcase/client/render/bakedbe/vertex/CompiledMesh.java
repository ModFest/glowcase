package dev.hephaestus.glowcase.client.render.bakedbe.vertex;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.ByteBufferBuilder.Result;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import dev.hephaestus.glowcase.mixinsupport.sodium.SodiumCompatShortcuts;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@NullMarked
public record CompiledMesh(MeshData meshData, @Nullable Sorter sorter) implements Closeable {
	@Override
	public void close() {
		meshData.close();
	}

	@Deprecated
	public interface Sorter {
		@Nullable Result buildSortedIndexBuffer(final ByteBufferBuilder target, final VertexSorting sorting);
		CompactVectorArray centroids();
	}

	@Deprecated
	public record SodiumSortState(CompactVectorArray centroids, IndexType indexType) implements Sorter {
		public static @Nullable SodiumSortState create(MeshData meshData) {
			MeshData.DrawState drawState = meshData.drawState();
			if (drawState.mode() != VertexFormat.Mode.QUADS) return null;

			CompactVectorArray centroids = unpackQuadCentroids(meshData.vertexBuffer(), drawState.vertexCount(), drawState.format());

			return new SodiumSortState(centroids, drawState.indexType());
		}

		private static CompactVectorArray unpackQuadCentroids(final ByteBuffer vertexBuffer, final int vertices, final VertexFormat format) {
			final int positionOffset = format.getOffset(VertexFormatElement.POSITION);
			if (positionOffset == -1) {
				throw new IllegalArgumentException("Cannot identify quad centers with no position element");
			}

			final FloatBuffer floatBuffer = vertexBuffer.asFloatBuffer();
			final int quadStride = format.getVertexSize();
			final int vertexStride = quadStride / 4;
			final int quads = vertices / 4;
			final CompactVectorArray sortingPoints = new CompactVectorArray(quads);

			for (
				int i = 0, firstPosOffset = positionOffset, secondPosOffset = positionOffset + (vertexStride * 2);
				i < quads;
				i++, firstPosOffset += quadStride, secondPosOffset += quadStride
			) {
				float x0 = floatBuffer.get(firstPosOffset);
				float y0 = floatBuffer.get(firstPosOffset + 1);
				float z0 = floatBuffer.get(firstPosOffset + 2);
				float x1 = floatBuffer.get(secondPosOffset);
				float y1 = floatBuffer.get(secondPosOffset + 1);
				float z1 = floatBuffer.get(secondPosOffset + 2);
				float xMid = (x0 + x1) * 0.5F;
				float yMid = (y0 + y1) * 0.5F;
				float zMid = (z0 + z1) * 0.5F;
				sortingPoints.set(i, xMid, yMid, zMid);
			}

			return sortingPoints;
		}

		@Override
		public @Nullable Result buildSortedIndexBuffer(ByteBufferBuilder target, VertexSorting sorting) {
			int[] sortedIds = sorting.sort(centroids);
			return SodiumCompatShortcuts.buildSortedIndexBuffer(indexType, target, sortedIds);
		}
	}
}
