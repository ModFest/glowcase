package dev.hephaestus.glowcase.client.render.bakedbe;

import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh;
import dev.hephaestus.glowcase.mixin.client.bakedbe.RenderTypeAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NonNull;

import java.io.Closeable;
import java.util.*;

public class BakedMeshes implements Closeable, Iterable<BakedMeshes.Entry> {
	private final Set<RenderType> renderTypes = new ObjectArraySet<>();
	private final List<Entry> entries = new ObjectArrayList<>();

	public BakedMeshes(Map<RenderType, CompiledMesh> solid, Map<RenderType, CompiledMesh> translucent) {
	    solid.forEach((renderType, meshData) -> {
			if (!renderTypes.add(renderType)) {
				String renderTypeName = ((RenderTypeAccessor) renderType).getName();
				throw new IllegalStateException("Same render type (" + renderTypeName + ") used for solid and translucent rendering");
			}
			entries.add(new Entry(renderType, meshData, false));
		});

	    translucent.forEach((renderType, meshData) -> {
			if (!renderTypes.add(renderType)) {
				String renderTypeName = ((RenderTypeAccessor) renderType).getName();
				throw new IllegalStateException("Same render type (" + renderTypeName + ") used for solid and translucent rendering");
			}
			entries.add(new Entry(renderType, meshData, true));
		});
	}

	public Set<RenderType> renderTypes() {
		return renderTypes;
	}

	@Override
	public void close() {
		for (Entry entry : entries) entry.close();
	}

	@Override
	public @NonNull Iterator<Entry> iterator() {
		return entries.iterator();
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
