package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ItemDisplayBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public record ItemDisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ItemDisplayBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/item_display_block.png");

	@Override
	public void render(ItemDisplayBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
		if (entity.getLevel() == null || entity.getLevel().getBlockState(entity.getBlockPos()).isAir()) return;

		boolean renderAsBlock = entity.getRenderAsBlock();
		matrices.pushPose();
		
		if (renderAsBlock && entity.getStack().getItem() instanceof BlockItem blockItem) {
			matrices.translate(0.5D, 0.5D, 0.5D);

			matrices.mulPose(Axis.YP.rotationDegrees(180.0F + entity.getYaw()));
			matrices.translate(entity.getOffset().x(), entity.getOffset().y(), entity.getOffset().z());

			matrices.translate(-0.5D, -0.5D, -0.5D);

			matrices.scale(entity.getScale().x(), entity.getScale().y(), entity.getScale().z());
			matrices.mulPose(Axis.XP.rotationDegrees(entity.getPitch()));

			Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockItem.getBlock().defaultBlockState(), matrices, vertexConsumers, light, overlay);
		} else {
			matrices.translate(0.5D, 0D, 0.5D);

			matrices.mulPose(Axis.YP.rotationDegrees(180.0F + entity.getYaw()));
			matrices.translate(entity.getOffset().x(), entity.getOffset().y(), entity.getOffset().z());

			matrices.translate(0D, 0.5D, 0D);

			matrices.scale(entity.getScale().x(), entity.getScale().y(), entity.getScale().z());
			matrices.mulPose(Axis.XP.rotationDegrees(entity.getPitch()));

			context.getItemRenderer().renderStatic(entity.getStack(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, matrices, vertexConsumers, entity.getLevel(), 0);
		}
		
		matrices.popPose();

		if (entity.matchesStack(ItemStack.EMPTY) || BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getBlockPos())) BlockEntityRenderUtil.renderCenteredPlaceholder(entity, ITEM_TEXTURE, 1.0F, Axis.YP.rotationDegrees(entity.getYaw()), matrices, vertexConsumers);
	}
}
