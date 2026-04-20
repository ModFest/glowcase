package dev.hephaestus.glowcase.client.render.bakedbe.chunk;

import com.mojang.blaze3d.vertex.MeshData;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedMeshes;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.chunk.SectionMesh.SectionDraw;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// This is mostly to keep section specific data that is not attached to coords
// This should not have values attached to coords as vanilla reuses it's sections and I don't want to add an extra hook for that - Luna
public class GlowcaseRenderSectionInfo implements Iterable<GlowcaseRenderSectionInfo.Entry> {
	private final List<Entry> draws = new ObjectArrayList<>();

	public void setSectionDraws(BakedMeshes meshes) {
		this.draws.clear();

		for (BakedMeshes.Entry entry : meshes) {
			draws.add(Entry.create(entry));
		}
	}

	@Override
	public @NonNull Iterator<Entry> iterator() {
		return draws.iterator();
	}

	@NullMarked
	public record Entry(RenderType renderType, SectionDraw draw, MeshData.@Nullable SortState sortState, boolean translucent) {
		private static Entry create(BakedMeshes.Entry entry) {
			MeshData mesh = entry.mesh().meshData();
			return new Entry(
				entry.renderType(),
				new SectionDraw(
					mesh.drawState().indexCount(),
					mesh.drawState().indexType(),
					entry.hasCustomIndexBuffer()
				),
				entry.mesh().sortState(),
				entry.translucent()
			);
		}
	}
}
