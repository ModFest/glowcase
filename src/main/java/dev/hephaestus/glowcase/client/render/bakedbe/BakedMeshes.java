package dev.hephaestus.glowcase.client.render.bakedbe;

import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh;
import dev.hephaestus.glowcase.util.collections.ObjectPairArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NullMarked;

import java.io.Closeable;
import java.util.*;

@NullMarked
public class BakedMeshes implements AutoCloseable, Iterable<BakedMeshes.Entry> {
	private final ObjectArrayList<Entry> entries = new ObjectArrayList<>();
	private final Runnable onFinish;
	private final boolean hasTranslucency;
	private final boolean isFallback;

	public BakedMeshes(ObjectPairArrayList<RenderType, CompiledMesh> meshes, Runnable onFinish) {
		this(meshes, onFinish, false);
	}

	BakedMeshes(ObjectPairArrayList<RenderType, CompiledMesh> meshes, Runnable onFinish, boolean isFallback) {
		this.isFallback = isFallback;
		this.onFinish = onFinish;

		boolean hasTranslucency = false;
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

	public void finish() {
		onFinish.run();
	}

	@Override
	public ObjectListIterator<Entry> iterator() {
		return entries.listIterator();
	}

	@Override
	public void close() {
		finish();
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
