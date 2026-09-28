package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StagedVertexBuffer.class)
public interface StagedVertexBufferAccessor {
	@Accessor StagedVertexBuffer.GpuBufferPool getStagingGpuBufferPool();
	@Accessor GpuBuffer getCurrentVertexBuffer();
	@Accessor GpuBuffer getCurrentIndexBuffer();
	@Accessor List<StagedVertexBuffer.Draw> getDraws();

	@Accessor void setCurrentVertexBuffer(GpuBuffer buffer);
	@Accessor void setCurrentIndexBuffer(GpuBuffer buffer);
	@Accessor void setDraws(List<StagedVertexBuffer.Draw> draws);
}
