package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hephaestus.glowcase.mixinsupport.RenderTypeInfoForBaking;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@NullMarked
@Mixin(RenderType.class)
public class RenderTypeMixin {
	@Shadow private @Final RenderSetup state;

	@ModifyReturnValue(at = @At("RETURN"), method = "prepare")
	private PreparedRenderType addInfoForBaking(PreparedRenderType original) {
		var bakery = (RenderTypeInfoForBaking) (Object) original;
		bakery.glowcase$setState(this.state);
		return original;
	}
}
