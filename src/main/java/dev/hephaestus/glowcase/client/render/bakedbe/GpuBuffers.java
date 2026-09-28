package dev.hephaestus.glowcase.client.render.bakedbe;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
public record GpuBuffers(GpuBuffer vertex, @Nullable GpuBuffer index, @Nullable List<StagedVertexBuffer.Draw> draws) {
	public boolean hasIndex() {
		return index != null;
	}
}
