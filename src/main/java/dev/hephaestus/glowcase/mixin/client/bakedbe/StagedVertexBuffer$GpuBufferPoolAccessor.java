package dev.hephaestus.glowcase.mixin.client.bakedbe;

import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StagedVertexBuffer.GpuBufferPool.class)
public interface StagedVertexBuffer$GpuBufferPoolAccessor {
	@Accessor List<StagedVertexBuffer.GpuBufferPool.PendingRecycle> getPendingRecycle();
}
