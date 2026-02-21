package dev.hephaestus.glowcase.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.client.render.item.ItemHandRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinHeldItemRenderer {
	@Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
	void glowcase$renderFirstPersonTablet(PoseStack matrices, MultiBufferSource vertexConsumers, int light, ItemStack stack, CallbackInfo ci) {
		if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return;

		@Nullable ItemHandRenderer renderer = ItemHandRenderer.getRenderer(stack);
		if (renderer == null) return;

		renderer.render(matrices, vertexConsumers, light, stack);
		ci.cancel();
	}

	@ModifyExpressionValue(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z", ordinal = 0))
	private boolean glowcase$enableFirstPersonTabletRendering(boolean original, AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
		@Nullable ItemHandRenderer renderer = ItemHandRenderer.getRenderer(stack);
		return original || (renderer != null && renderer.visible(stack));
	}
}
