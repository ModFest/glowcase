package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import net.minecraft.client.renderer.BindGroupLayouts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Set;

@Mixin(RenderPipeline.Builder.class)
public class RenderPipeline$BuilderMixin {
	@Unique private static final BindGroupLayout glowcase$FOG_OFFSET = BindGroupLayout.builder().withUniform("GlowcaseFogOffset", UniformType.UNIFORM_BUFFER).build();
	@Shadow private Optional<Set<BindGroupLayout>> bindGroupLayouts;

	@Inject(at = @At("RETURN"), method = "withBindGroupLayout")
	private void addFogOffset(BindGroupLayout bindGroupLayout, CallbackInfoReturnable<RenderPipeline.Builder> cir) {
		if (bindGroupLayout == BindGroupLayouts.FOG) {
			this.bindGroupLayouts.get().add(glowcase$FOG_OFFSET);
		}
	}
}
