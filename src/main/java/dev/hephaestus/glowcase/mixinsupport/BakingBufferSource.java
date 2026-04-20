package dev.hephaestus.glowcase.mixinsupport;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Map;

public interface BakingBufferSource {
    Map<RenderType, MeshData> glowcase$bakeAllBatches(VertexSorting vertexSorting);
    Map<RenderType, MeshData> glowcase$bakeBatch(VertexSorting vertexSorting);
}
