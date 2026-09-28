package dev.hephaestus.glowcase.mixinsupport;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public interface RenderTypeInfoForBaking {
	RenderSetup glowcase$getState();
	void glowcase$setState(RenderSetup state);

	LayeringTransform glowcase$getLayeringTransform();

	TextureTransform glowcase$getTextureTransform();

	void glowcase$setFogOffset(GpuBufferSlice offset);

	void glowcase$setDynamicTransforms(GpuBufferSlice transforms);

	void glowcase$setTextures(List<PreparedRenderType.Texture> textures);
}
