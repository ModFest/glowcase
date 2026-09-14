package dev.hephaestus.glowcase.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.client.render.item.ItemHandRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer.class)
public class FirstPersonHandsAndItemsRendererMixin {
	@Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
	void glowcase$renderFirstPersonTablet(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, ItemStack itemStack, boolean mainHand, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
		if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return;

		@Nullable ItemHandRenderer renderer = ItemHandRenderer.getRenderer(itemStack);
		if (renderer == null) return;

		renderer.render(poseStack, submitNodeCollector, lightCoords, itemStack);
		ci.cancel();
	}

	@ModifyExpressionValue(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z", ordinal = 0))
	private boolean glowcase$enableFirstPersonTabletRendering(boolean original, final @Local(argsOnly = true) ItemStack stack) {
		@Nullable ItemHandRenderer renderer = ItemHandRenderer.getRenderer(stack);
		return original || (renderer != null && renderer.visible(stack));
	}
}
