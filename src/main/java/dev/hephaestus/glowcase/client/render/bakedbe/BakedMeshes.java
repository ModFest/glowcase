package dev.hephaestus.glowcase.client.render.bakedbe;

import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh;
import dev.hephaestus.glowcase.mixin.client.bakedbe.RenderTypeAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.util.*;

@NullMarked
public class BakedMeshes implements AutoCloseable, Iterable<BakedMeshes.Entry> {
	private final Set<RenderType> renderTypes = new ObjectArraySet<>();
	private final List<Entry> entries = new ObjectArrayList<>();
	private final boolean hasTranslucency;
	private final boolean isFallback;

	public BakedMeshes(@Nullable Map<RenderType, CompiledMesh> solid, @Nullable Map<RenderType, CompiledMesh> translucent) {
		this(solid, translucent, false);
	}

	public BakedMeshes(@Nullable Map<RenderType, CompiledMesh> solid, @Nullable Map<RenderType, CompiledMesh> translucent, boolean isFallback) {
		this.hasTranslucency = translucent != null && !translucent.isEmpty();
		this.isFallback = isFallback;

	    if (solid != null) {
			solid.forEach((renderType, meshData) -> {
				if (!renderTypes.add(renderType)) {
					String renderTypeName = ((RenderTypeAccessor) renderType).getName();
					throw new IllegalStateException("Same render type (" + renderTypeName + ") used for solid and translucent rendering");
				}
				entries.add(new Entry(renderType, meshData, false));
			});
		}

		if (translucent != null) {
			translucent.forEach((renderType, meshData) -> {
				if (!renderTypes.add(renderType)) {
					String renderTypeName = ((RenderTypeAccessor) renderType).getName();
					throw new IllegalStateException("Same render type (" + renderTypeName + ") used for solid and translucent rendering");
				}
				entries.add(new Entry(renderType, meshData, true));
			});
		}
	}

	public Set<RenderType> renderTypes() {
		return renderTypes;
	}

	public boolean hasTranslucency() {
		return hasTranslucency;
	}

	public boolean isFallback() {
		return isFallback;
	}

	@Override
	public @NonNull Iterator<Entry> iterator() {
		return entries.iterator();
	}

	@Override
	public void close() {
		for (Entry entry : entries) entry.close();
	}

	public record Entry(RenderType renderType, CompiledMesh mesh, boolean hasCustomIndexBuffer, boolean translucent) implements Closeable {
		public Entry(RenderType renderType, CompiledMesh mesh, boolean translucent) {
		    this(renderType, mesh, mesh.meshData().indexBuffer() != null, translucent);
		}

		@Override
		public void close() {
			mesh.close();
		}
	}
}
