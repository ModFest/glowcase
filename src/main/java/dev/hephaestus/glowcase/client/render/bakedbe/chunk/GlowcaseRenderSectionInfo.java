package dev.hephaestus.glowcase.client.render.bakedbe.chunk;

import com.mojang.blaze3d.vertex.MeshData;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh.SectionDraw;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;

import java.util.Map;

// This is mostly to keep section specific data that is not attached to coords
// This should not have values attached to coords as vanilla reuses it's sections and I don't want to add an extra hook for that - Luna
public class GlowcaseRenderSectionInfo {
	private final Map<RenderType, SectionDraw> draws = new Object2ObjectOpenHashMap<>();

	public void setSectionDraws(Map<RenderType, MeshData> sectionDraws) {
		reset();
		sectionDraws.forEach((renderType, mesh) -> draws.put(
			renderType,
			new SectionDraw(
				mesh.drawState().indexCount(),
				mesh.drawState().indexType(),
				mesh.indexBuffer() != null
			)
		));
	}

	public SectionDraw getSectionDraw(RenderType renderType) {
		return this.draws.get(renderType);
	}

	public void reset() {
		draws.clear();
	}
}
