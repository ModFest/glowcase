package dev.hephaestus.glowcase.mixin.client;

import com.llamalad7.mixinextras.expression.Definition;import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.CompactVectorArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CompactVectorArray.class)
public class CompactVectorArrayMixin {
	/**
	 * @author Awakened Redstone (Luna)
	 * @reason Fix bug on vanilla method
	 */
	@Definition(id = "contents", field = "Lcom/mojang/blaze3d/vertex/CompactVectorArray;contents:[F")
	@Expression("this.contents[? + @(1)]")
	@ModifyExpressionValue(method = "getZ", at = @At("MIXINEXTRAS:EXPRESSION"))
	public int getZ(int original) {
		return 2;
	}
}
