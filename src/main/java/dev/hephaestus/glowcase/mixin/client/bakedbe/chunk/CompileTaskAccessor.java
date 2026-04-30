package dev.hephaestus.glowcase.mixin.client.bakedbe.chunk;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(SectionRenderDispatcher.RenderSection.CompileTask.class)
public interface CompileTaskAccessor {
	@Accessor AtomicBoolean getIsCancelled();
}
