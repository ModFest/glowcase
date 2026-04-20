package dev.hephaestus.glowcase.mixin.client.bakedbe;

import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public class RenderSectionMixin {
	@Shadow private volatile long sectionNode;

	@Inject(at = @At(value = "INVOKE", target = "Ljava/util/concurrent/locks/ReentrantLock;lock()V"), method = "reset")
	private void onSectionReset(CallbackInfo ci) {
		GlowcaseLevelRenderer.getInstance().releaseSection(this.sectionNode);
	}
}
