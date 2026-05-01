package dev.hephaestus.glowcase.client.render.bakedbe.section;

import com.mojang.blaze3d.vertex.MeshData;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedMeshes;
import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.chunk.SectionMesh.SectionDraw;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

// This should not have values that have to be closed, such as buffers
@NullMarked
public class GlowcaseRenderSectionInfo implements Iterable<GlowcaseRenderSectionInfo.DrawEntry> {
	private final TranslucencyPointOfView translucencyPointOfView = new TranslucencyPointOfView();
	private final ObjectArrayList<DrawEntry> draws = new ObjectArrayList<>();
	private final RenderSectionPos sectionPos;
	private boolean error;

	public GlowcaseRenderSectionInfo(long sectionNode) {
		this.sectionPos = new RenderSectionPos(sectionNode);
	}

	public void setSectionDraws(BakedMeshes meshes) {
		this.draws.clear();
		this.error = meshes.isFallback();

		for (BakedMeshes.Entry entry : meshes) {
			draws.add(DrawEntry.create(entry));
		}
	}

	public void setTranslucencyPointOfView(final Vector3f cameraPos, final long sectionPos) {
		translucencyPointOfView.set(new Vec3(cameraPos), sectionPos);
	}

	public boolean isDifferentPointOfView(TranslucencyPointOfView pointOfView) {
		return !translucencyPointOfView.equals(pointOfView);
	}

	public RenderSectionPos sectionPos() {
		return sectionPos;
	}

	public boolean isError() {
		return error;
	}

	@Override
	public FastIterator iterator() {
		return new FastIterator();
	}

	public record DrawEntry(RenderType renderType, SectionDraw draw, CompiledMesh.@Nullable Sorter sorter) {
		private static DrawEntry create(BakedMeshes.Entry entry) {
			MeshData mesh = entry.mesh().meshData();
			return new DrawEntry(
				entry.renderType(),
				new SectionDraw(
					mesh.drawState().indexCount(),
					mesh.drawState().indexType(),
					entry.hasCustomIndexBuffer()
				),
				entry.mesh().sorter()
			);
		}

		public int indexCount() {
			return sorter == null ? 0 : sorter.centroids().size();
		}
	}

	public class FastIterator implements Iterator<DrawEntry> {
		private final Object[] draws = GlowcaseRenderSectionInfo.this.draws.elements();
		private final int size = GlowcaseRenderSectionInfo.this.draws.size();
		private int index = 0;

		@Override
		public boolean hasNext() {
			return index < size;
		}

		@Override
		public DrawEntry next() {
			return (DrawEntry) draws[index++];
		}
	}
}
