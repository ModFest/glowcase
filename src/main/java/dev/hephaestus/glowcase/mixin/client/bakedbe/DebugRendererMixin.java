package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.llamalad7.mixinextras.sugar.Local;
import dev.hephaestus.glowcase.client.GlowcaseClient;
import dev.hephaestus.glowcase.client.render.bakedbe.RenderSectionBorderRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugRenderer.class)
public class DebugRendererMixin {
	@Shadow private @Final List<DebugRenderer.SimpleDebugRenderer> renderers;

	@Inject(at = @At("RETURN"), method = "refreshRendererList")
	private void addDebugRenderers(CallbackInfo ci, @Local(name = "minecraft") Minecraft minecraft) {
		if (minecraft.debugEntries.isCurrentlyEnabled(GlowcaseClient.RENDER_SECTION_BORDERS)) {
			this.renderers.add(new RenderSectionBorderRenderer(minecraft));
		}
	}
}
