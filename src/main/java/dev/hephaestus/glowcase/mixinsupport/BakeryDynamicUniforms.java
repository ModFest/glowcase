package dev.hephaestus.glowcase.mixinsupport;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.hephaestus.glowcase.client.render.bakedbe.RenderOffset;
import net.minecraft.core.Vec3i;
import org.joml.Vector3f;

public interface BakeryDynamicUniforms {
	GpuBufferSlice glowcase$writeFogOffset(final Vec3i blockPos, final Vector3f offset);
	GpuBufferSlice[] glowcase$writeFogOffsets(RenderOffset... fogTransforms);
	GpuBufferSlice glowcase$noFogOffset();
}
