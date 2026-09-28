package dev.hephaestus.glowcase.mixinsupport;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import dev.hephaestus.glowcase.client.render.bakedbe.CachedFrame;
import dev.hephaestus.glowcase.client.render.bakedbe.GpuBuffers;

import java.util.Map;
import java.util.Set;

public interface BakedBuffers {
	Set<GpuBuffer> glowcase$cachedBuffers();
}
