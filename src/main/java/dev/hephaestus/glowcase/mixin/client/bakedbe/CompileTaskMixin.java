package dev.hephaestus.glowcase.mixin.client.bakedbe;

import dev.hephaestus.glowcase.mixinsupport.CompileTaskFields;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(SectionRenderDispatcher.RenderSection.CompileTask.class)
public class CompileTaskMixin implements CompileTaskFields {
	@Shadow @Final protected AtomicBoolean isCancelled;

	@Override
	public AtomicBoolean glowcase$isCancelled() {
		return isCancelled;
	}
}
