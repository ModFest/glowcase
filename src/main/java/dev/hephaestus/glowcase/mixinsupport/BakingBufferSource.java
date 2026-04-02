package dev.hephaestus.glowcase.mixinsupport;

import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Map;

public interface BakingBufferSource {
    Map<RenderType, MeshData> glowcase$bakeAllBatches();
    Map<RenderType, MeshData> glowcase$bakeBatch();
}
