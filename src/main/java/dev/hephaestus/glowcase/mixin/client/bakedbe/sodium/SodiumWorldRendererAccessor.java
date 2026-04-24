package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(SodiumWorldRenderer.class)
public interface SodiumWorldRendererAccessor {
	@Accessor RenderSectionManager getRenderSectionManager();
}
