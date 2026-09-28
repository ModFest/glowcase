package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.device.GpuDevice;
import dev.hephaestus.glowcase.client.render.bakedbe.CachedFrame;
import dev.hephaestus.glowcase.client.render.bakedbe.GpuBuffers;
import dev.hephaestus.glowcase.mixinsupport.BakeryDrawInfo;
import dev.hephaestus.glowcase.mixinsupport.CachedBuffersReferencer;
import dev.hephaestus.glowcase.mixinsupport.BakedBuffers;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(StagedVertexBuffer.class)
public class StagedVertexBufferMixin implements BakedBuffers {
	@Unique private final Set<GpuBuffer> glowcase$cachedBuffers = new ObjectOpenHashSet<>();

	@Shadow private @Final StagedVertexBuffer.GpuBufferPool vertexBufferPool;
	@Shadow private @Final StagedVertexBuffer.GpuBufferPool indexBufferPool;

	@Inject(at = @At("RETURN"), method = "<init>")
	private void setReferences(CallbackInfo ci) {
		((CachedBuffersReferencer) vertexBufferPool).glowcase$setCachedBuffersRef(this.glowcase$cachedBuffers);
		((CachedBuffersReferencer) indexBufferPool).glowcase$setCachedBuffersRef(this.glowcase$cachedBuffers);
	}

	@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/MeshData$SortState;writeSortedIndexBuffer(Ljava/nio/ByteBuffer;Lcom/mojang/blaze3d/vertex/VertexSorting;)V"), method = "uploadDrawsToBuffers")
	private void saveIndexBufferState(CallbackInfo ci, @Local(name = "sortState") MeshData.SortState sortState, @Local(name = "draw") StagedVertexBuffer.Draw draw) {
		((BakeryDrawInfo) draw).glowcase$setSortState(sortState);
	}

	@Override
	public Set<GpuBuffer> glowcase$cachedBuffers() {
		return this.glowcase$cachedBuffers;
	}
}
