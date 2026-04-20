package dev.hephaestus.glowcase.client.render.bakedbe.vertex;

import com.mojang.blaze3d.vertex.MeshData;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;

@NullMarked
public record CompiledMesh(MeshData meshData, MeshData.@Nullable SortState sortState) implements Closeable {
	@Override
	public void close() {
		meshData.close();
	}
}
