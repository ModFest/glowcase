package dev.hephaestus.glowcase.mixin.client.bakedbe;

import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FeatureRendererMap.class)
public interface FeatureRendererMapAccessor {
	@Accessor FeatureRenderer<?>[] getRenderers();
	@Accessor void setRenderers(FeatureRenderer<?>[] renderers);
}
