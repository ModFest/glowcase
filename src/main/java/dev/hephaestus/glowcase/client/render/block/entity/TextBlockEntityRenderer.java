package dev.hephaestus.glowcase.client.render.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.GlowcaseRenderLayers;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class TextBlockEntityRenderer extends BakedBlockEntityRenderer<TextBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/text_block.png");

	public TextBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public boolean shouldBake(TextBlockEntity entity) {
		return !entity.lines.isEmpty();
	}

	@Override
	public void renderUnbaked(TextBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		if (entity.renderDirty) {
			entity.renderDirty = false;
			Manager.markForRebuild(entity.getPos());
		}
		if (entity.getWorld() == null || entity.getWorld().getBlockState(entity.getPos()).isAir()) return;
		if (entity.lines.stream().allMatch(t -> t.getString().isBlank()) || BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getPos())) BlockEntityRenderUtil.renderPlaceholderWithBlockRotation(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers, entity.zOffset == TextBlockEntity.ZOffset.CENTER ? 0F : entity.zOffset == TextBlockEntity.ZOffset.FRONT ? 0.4F : -0.4F);
	}

	@Override
	public void renderBaked(TextBlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		matrices.push();
		matrices.translate(0.5D, 0.5D, 0.5D);

		float rotation = -(entity.getCachedState().get(Properties.ROTATION) * 360) / 16.0F;
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

		switch (entity.zOffset) {
			case FRONT -> matrices.translate(0D, 0D, 0.4D);
			case BACK -> matrices.translate(0D, 0D, -0.4D);
		}

		float scale = 0.010416667F * entity.scale;
		matrices.scale(scale, -scale, scale);
		TextRenderer textRenderer = this.context.getTextRenderer();

		double maxLength = 0;
		double minLength = Double.MAX_VALUE;
		for (int i = 0; i < entity.lines.size(); ++i) {
			maxLength = Math.max(maxLength, textRenderer.getWidth(entity.lines.get(i)));
			minLength = Math.min(minLength, textRenderer.getWidth(entity.lines.get(i)));
		}

		matrices.translate(0, -((entity.lines.size() - 0.25) * 12) / 2D, 0D);
		for (int i = 0; i < entity.lines.size(); ++i) {
			double width = textRenderer.getWidth(entity.lines.get(i));
			double dX = switch (entity.textAlignment) {
				case LEFT -> -maxLength / 2D;
				case CENTER -> (maxLength - width) / 2D - maxLength / 2D;
				case CENTER_LEFT -> (-maxLength / 2D) - (width / 2D);
				case CENTER_RIGHT -> (maxLength / 2D) - (width / 2D);
				case RIGHT -> maxLength - width - maxLength / 2D;
			};

			matrices.push();
			matrices.translate(dX, 0, 0);

			if (entity.shadowType == TextBlockEntity.ShadowType.PLATE && width > 0) {
				matrices.translate(0, 0, -0.025D);
				drawFillRect(matrices, vertexConsumers, (int) width + 5, (i + 1) * 12 - 2, -5, i * 12 - 2, 0x44000000);
				matrices.translate(0, 0, 0.025D);
			}

			textRenderer.draw(entity.lines.get(i), 0, i * 12, entity.color, entity.shadowType == TextBlockEntity.ShadowType.DROP, matrices.peek().getPositionMatrix(), vertexConsumers, TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);

			matrices.pop();
		}

		matrices.pop();
	}

	@SuppressWarnings("SameParameterValue")
	private void drawFillRect(MatrixStack matrices, VertexConsumerProvider vcp, int x1, int y1, int x2, int y2, int color) {
		float red = (float) (color >> 16 & 255) / 255.0F;
		float green = (float) (color >> 8 & 255) / 255.0F;
		float blue = (float) (color & 255) / 255.0F;
		float alpha = (float) (color >> 24 & 255) / 255.0F;
		VertexConsumer consumer = vcp.getBuffer(GlowcaseRenderLayers.TEXT_PLATE);
		Matrix4f matrix = matrices.peek().getPositionMatrix();
		consumer.vertex(matrix, x1, y2, 0.0f)
			.color(red, green, blue, alpha);
		consumer.vertex(matrix, x2, y2, 0.0f)
			.color(red, green, blue, alpha);
		consumer.vertex(matrix, x2, y1, 0.0f)
			.color(red, green, blue, alpha);
		consumer.vertex(matrix, x1, y1, 0.0f)
			.color(red, green, blue, alpha);
	}
}
