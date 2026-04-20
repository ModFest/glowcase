package dev.hephaestus.glowcase.mixin.client.bakedbe;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.resources.model.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FeatureRenderDispatcher.class)
public interface FeatureRenderDispatcherAccessor {
	@Accessor ModelManager getModelManager();
	@Accessor Font getFont();
}
