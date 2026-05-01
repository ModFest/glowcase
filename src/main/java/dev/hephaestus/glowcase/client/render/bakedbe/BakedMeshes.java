package dev.hephaestus.glowcase.client.render.bakedbe;

import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh;
import dev.hephaestus.glowcase.util.collections.ObjectPairArrayList;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ReferenceReferencePair;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.util.*;

@NullMarked
public class BakedMeshes implements AutoCloseable, Iterable<BakedMeshes.Entry> {
	private final ObjectArrayList<Entry> entries = new ObjectArrayList<>();
	private final boolean hasTranslucency;
	private final boolean isFallback;

	public BakedMeshes(ObjectPairArrayList<RenderType, CompiledMesh> meshes) {
		this(meshes, false);
	}

	BakedMeshes(ObjectPairArrayList<RenderType, CompiledMesh> meshes, boolean isFallback) {
		boolean hasTranslucency = false;
		this.isFallback = isFallback;

		for (var pair : meshes) {
			var renderType = pair.left();
			// Use blending as an indicator as text doesn't mark sortOnUpload due to shadows being weird
			hasTranslucency |= renderType.hasBlending();
			entries.add(new Entry(renderType, pair.right(), renderType.hasBlending()));
		}

		this.hasTranslucency = hasTranslucency;
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public boolean hasTranslucency() {
		return hasTranslucency;
	}

	public boolean isFallback() {
		return isFallback;
	}

	@Override
	public ObjectListIterator<Entry> iterator() {
		return entries.listIterator();
	}

	@Override
	public void close() {
		for (Entry entry : entries) entry.close();
	}

	public record Entry(RenderType renderType, CompiledMesh mesh, boolean hasCustomIndexBuffer) implements Closeable {
		public Entry(RenderType renderType, CompiledMesh mesh) {
		    this(renderType, mesh, mesh.meshData().indexBuffer() != null);
		}

		@Override
		public void close() {
			mesh.close();
		}
	}
}
