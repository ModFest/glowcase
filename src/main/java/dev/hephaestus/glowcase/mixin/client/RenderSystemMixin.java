package dev.hephaestus.glowcase.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.hephaestus.glowcase.client.render.font.GlyphBakeQueue;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
	@Inject(at = @At("HEAD"), method = "executePendingTasks")
	private static void bakeFontGlyphs(CallbackInfo ci) {
		try (Zone _ = Profiler.get().zone("Bake pending glyphs")) {
			GlyphBakeQueue.bake();
		}
	}
}
