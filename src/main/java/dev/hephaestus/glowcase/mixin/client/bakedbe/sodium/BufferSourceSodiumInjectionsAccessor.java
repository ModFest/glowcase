package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder.Result;
import com.mojang.blaze3d.vertex.MeshData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(value = MultiBufferSource.BufferSource.class, priority = 9999)
public interface BufferSourceSodiumInjectionsAccessor {
	@SuppressWarnings("MixinAnnotationTarget") // Ignore MCDev here
	@Invoker("buildSortedIndexBuffer")
	static Result callBuildSortedIndexBuffer(MeshData meshData, ByteBufferBuilder bufferBuilder, int[] primitiveIds) {
		throw new UnsupportedOperationException("Implemented via mixin");
	}
}
