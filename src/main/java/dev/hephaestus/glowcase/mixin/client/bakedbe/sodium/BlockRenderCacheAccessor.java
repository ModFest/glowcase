package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import net.caffeinemc.mods.sodium.client.model.light.data.ArrayLightDataCache;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockRenderCache.class)
public interface BlockRenderCacheAccessor {
	@Accessor ArrayLightDataCache getLightDataCache();
}
