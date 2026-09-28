package dev.hephaestus.glowcase.client.render.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record RenderStateFlag(
	ScreenRectangle bounds,
	RenderPipeline pipeline,
	TextureSetup textureSetup
) implements GuiElementRenderState {
	public RenderStateFlag(ScreenRectangle bounds, GpuTextureView texture) {
	    this(
			bounds,
			RenderPipelines.MOJANG_LOGO,
			TextureSetup.singleTexture(texture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
		);
	}

	@Override
	public void buildVertices(VertexConsumer vertexConsumer) {}

	@Override
	public @Nullable ScreenRectangle scissorArea() {
		return null;
	}
}
