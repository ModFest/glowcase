package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.render.bakedbe.section.GlowcaseSectionRenderDispatcher;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkJobCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Mixin(ChunkJobCollector.class)
public class ChunkJobCollectorMixin {
	@WrapOperation(at = @At(value = "INVOKE", target = "Ljava/util/concurrent/Semaphore;acquireUninterruptibly(I)V"), method = "awaitCompletion")
	private void fixDeadlock(Semaphore instance, int _permits, Operation<Void> original) {
		boolean success = false;
		while (!success) {
			try {
				success = instance.tryAcquire(_permits, 50, TimeUnit.MILLISECONDS);
			} catch (InterruptedException _) {}

			GlowcaseSectionRenderDispatcher dispatcher = GlowcaseLevelRenderer.getInstance().getSectionRenderDispatcher();
			if (dispatcher != null) dispatcher.uploadGlobalGeomBuffersToGPU();
		}
	}
}
