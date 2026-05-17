package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import dev.hephaestus.glowcase.mixinsupport.BufferOOMRecovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ByteBufferBuilder.class)
public abstract class ByteBufferBuilderMixin implements BufferOOMRecovery {
	@Shadow private long writeOffset;
	@Shadow private long nextResultOffset;

	@Shadow protected abstract void checkOpen();

	@Override
	public void glowcase$freeUnbuilt() {
		this.checkOpen();
		if (this.writeOffset - this.nextResultOffset > 0) {
			this.writeOffset = this.nextResultOffset;
		}
	}
}
