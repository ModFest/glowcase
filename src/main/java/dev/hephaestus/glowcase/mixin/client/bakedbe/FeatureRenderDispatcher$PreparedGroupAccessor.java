package dev.hephaestus.glowcase.mixin.client.bakedbe;

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FeatureRenderDispatcher.PreparedGroup.class)
public interface FeatureRenderDispatcher$PreparedGroupAccessor<Submit extends SubmitNode> {
	@Accessor FeatureRendererType<Submit> getFeatureType();
}
