package dev.hephaestus.glowcase.mixinsupport;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Set;

public interface CachedBuffersReferencer {
	void glowcase$setCachedBuffersRef(@NonNull Set<GpuBuffer> ref);
}
