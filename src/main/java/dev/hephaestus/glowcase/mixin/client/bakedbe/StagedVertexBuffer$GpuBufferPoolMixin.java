package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.renderpearl.api.device.GpuDevice;
import dev.hephaestus.glowcase.mixinsupport.CachedBuffersReferencer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.StagedVertexBuffer.GpuBufferPool;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(GpuBufferPool.class)
public class StagedVertexBuffer$GpuBufferPoolMixin implements CachedBuffersReferencer {
	@Unique private @Nullable Set<GpuBuffer> glowcase$cachedBuffers;

	@Shadow private @Final List<GpuBuffer> usedThisFrame;
	@Shadow private @Final List<GpuBufferPool.PendingRecycle> pendingRecycle;

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/StagedVertexBuffer$GpuBufferPool$PendingRecycle;tryRecycle()Ljava/util/List;"), method = "lambda$tryRecycleBuffers$0", cancellable = true)
	private void checkIfCached(GpuBufferPool.PendingRecycle buffer, CallbackInfoReturnable<Boolean> cir) {
		if (buffer.buffers().size() != 1 || this.glowcase$cachedBuffers == null) return;

		GpuBuffer gpuBuffer = buffer.buffers().getFirst();
		if (this.glowcase$cachedBuffers.contains(gpuBuffer)) {
			cir.setReturnValue(false);
		}
	}

	@Override
	public void glowcase$setCachedBuffersRef(@NonNull Set<GpuBuffer> ref) {
		this.glowcase$cachedBuffers = ref;
	}

	@Inject(at = @At("HEAD"), method = "endFrame")
	private void splitCachedBuffers(GpuDevice device, CallbackInfo ci) {
		if (this.usedThisFrame.isEmpty() || this.glowcase$cachedBuffers == null) return;

		List<GpuBuffer> splits = new ObjectArrayList<>();
		this.usedThisFrame.removeIf(buffer -> {
			if (!glowcase$cachedBuffers.contains(buffer)) return false;
			splits.add(buffer);
			return true;
		});

		for (GpuBuffer buffer : splits) {
			GpuFence fence = device.createCommandEncoder().createFence();
			this.pendingRecycle.add(new GpuBufferPool.PendingRecycle(List.of(buffer), fence));
		}
	}
}
