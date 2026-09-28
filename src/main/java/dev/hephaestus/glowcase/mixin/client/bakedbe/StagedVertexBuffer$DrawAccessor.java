package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.mojang.renderpearl.api.pipeline.IndexType;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StagedVertexBuffer.Draw.class)
public interface StagedVertexBuffer$DrawAccessor {
	@Accessor int getIndexOffset();
	@Accessor int getIndexCount();
	@Invoker IndexType callIndexType();
}
