package dev.hephaestus.glowcase.mixin.client;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CompactVectorArray.class)
public class CompactVectorArrayMixin {
	@Shadow @Final private float[] contents;

	/**
	 * @author Awakened Redstone (Luna)
	 * @reason Fix bug on vanilla method
	 */
	@Overwrite
	public float getZ(final int index) {
		return this.contents[3 * index + 2];
	}
}
