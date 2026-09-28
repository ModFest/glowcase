package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import dev.hephaestus.glowcase.mixinsupport.BakeryDynamicUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
	@Inject(at = @At("RETURN"), method = "bindDefaultUniforms")
	private static void bindFogOffset(RenderPass renderPass, CallbackInfo ci) {
		var fogOffset = ((BakeryDynamicUniforms) RenderSystem.getDynamicUniforms()).glowcase$noFogOffset();
		if (fogOffset != null) {
			renderPass.setUniform("GlowcaseFogOffset", fogOffset);
		}
	}
}
