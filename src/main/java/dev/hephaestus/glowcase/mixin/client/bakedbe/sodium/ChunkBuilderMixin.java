package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.render.bakedbe.section.GlowcaseSectionRenderDispatcher;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.time.Duration;

@Mixin(ChunkBuilder.class)
public class ChunkBuilderMixin {
	@WrapOperation(at = @At(value = "INVOKE", target = "Ljava/lang/Thread;join()V"), method = "shutdownThreads")
	private void fixDeadlock(Thread instance, Operation<Void> original) {
		boolean success = false;
		while (!success) {
			try {
				success = instance.join(Duration.ofMillis(50));
			} catch (InterruptedException _) {}

			GlowcaseSectionRenderDispatcher dispatcher = GlowcaseLevelRenderer.getInstance().getSectionRenderDispatcher();
			if (dispatcher != null) dispatcher.uploadGlobalGeomBuffersToGPU();
		}
	}
}
