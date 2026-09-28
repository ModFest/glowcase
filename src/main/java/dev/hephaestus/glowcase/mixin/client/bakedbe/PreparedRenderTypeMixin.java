package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import dev.hephaestus.glowcase.mixinsupport.RenderTypeInfoForBaking;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@NullMarked
@SuppressWarnings("NotNullFieldNotInitialized")
@Mixin(PreparedRenderType.class)
public class PreparedRenderTypeMixin implements RenderTypeInfoForBaking {
	@Unique private RenderSetup glowcase$state;
	@Unique private @Nullable GpuBufferSlice glowcase$fogOffset;

	@Shadow private @Mutable @Final GpuBufferSlice dynamicTransforms;
	@Shadow private @Mutable @Final List<PreparedRenderType.Texture> textures;

	@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;bindDefaultUniforms(Lcom/mojang/renderpearl/api/commands/RenderPass;)V", shift = At.Shift.AFTER), method = "draw")
	private void setFogOffestUniform(CallbackInfo ci, @Local(argsOnly = true, name = "renderPass") RenderPass renderPass) {
		if (this.glowcase$fogOffset != null) {
			renderPass.setUniform("GlowcaseFogOffset", this.glowcase$fogOffset);
		}
	}

	@Override
	public RenderSetup glowcase$getState() {
		return this.glowcase$state;
	}

	@Override
	public void glowcase$setState(RenderSetup state) {
		this.glowcase$state = state;
	}

	@Override
	public LayeringTransform glowcase$getLayeringTransform() {
		return ((RenderSetupAccessor) (Object) this.glowcase$state).getLayeringTransform();
	}

	@Override
	public TextureTransform glowcase$getTextureTransform() {
		return this.glowcase$state.textureTransform;
	}

	@Override
	public void glowcase$setFogOffset(GpuBufferSlice offset) {
		this.glowcase$fogOffset = offset;
	}

	@Override
	public void glowcase$setDynamicTransforms(GpuBufferSlice transforms) {
		this.dynamicTransforms = transforms;
	}

	@Override
	public void glowcase$setTextures(List<PreparedRenderType.Texture> textures) {
		this.textures = textures;
	}
}
