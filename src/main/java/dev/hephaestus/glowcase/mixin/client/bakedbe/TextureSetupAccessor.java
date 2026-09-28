package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.render.TextureSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureSetup.class)
public interface TextureSetupAccessor {
	@Accessor void setTexure0(GpuTextureView textureView);
}
