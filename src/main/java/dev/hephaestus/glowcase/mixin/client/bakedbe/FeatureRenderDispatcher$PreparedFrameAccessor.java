package dev.hephaestus.glowcase.mixin.client.bakedbe;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public interface FeatureRenderDispatcher$PreparedFrameAccessor {
	@Accessor Map<FeatureRenderPhase<?>, List<FeatureRenderDispatcher.PreparedGroup<?>>> getGroupsByPhase();
	@Accessor @Nullable SubmitNodeStorage getSubmitNodeStorage();
	@Accessor @Nullable FeatureFrameContext getContext();
	@Accessor List<SubmitNode> getAllSubmits();
	@Accessor FeatureRenderDispatcher getThis$0();
}
