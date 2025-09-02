package dev.hephaestus.glowcase.client.render.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.concurrent.atomic.AtomicBoolean;

public class TextBlockEntityRenderer extends BakedBlockEntityRenderer<TextBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/text_block.png");
	private boolean wasOutOfRange = false;

	public TextBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public boolean shouldBake(TextBlockEntity entity) {
		return !entity.lines.isEmpty();
	}

	@Override
	public void renderUnbaked(TextBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
		Entity camera = MinecraftClient.getInstance().getCameraEntity();
		if (camera != null && entity.viewDistance >= 0) {
			double dx = camera.getX() - (entity.getPos().getX() + 0.5);
			double dy = camera.getY() - (entity.getPos().getY() + 0.5);
			double dz = camera.getZ() - (entity.getPos().getZ() + 0.5);

			if ((dx * dx + dy * dy + dz * dz) > (entity.viewDistance * entity.viewDistance)) {
				if (!wasOutOfRange) {
                    entity.renderDirty = true;
                    wasOutOfRange = true;
                }
			} else {
				if (wasOutOfRange) {
					entity.renderDirty = true;
				}

				wasOutOfRange = false;
			}
		}

		if (entity.renderDirty) {
			entity.renderDirty = false;
			BakedBlockEntityRenderer.Manager.markForRebuild(entity.getPos());
		}

		if (entity.getWorld() == null || entity.getWorld().getBlockState(entity.getPos()).isAir()) return;
		if (entity.lines.stream().allMatch(t -> t.getString().isBlank()) || BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getPos())) BlockEntityRenderUtil.renderPlaceholderWithBlockRotation(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers, entity.zOffset == TextBlockEntity.ZOffset.CENTER ? 0.01F : entity.zOffset == TextBlockEntity.ZOffset.FRONT ? 0.4F : -0.4F);
	}

	@Override
	public void renderBaked(TextBlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		Entity camera = MinecraftClient.getInstance().getCameraEntity();
		if (camera != null && entity.viewDistance >= 0) {
			double dx = camera.getX() - (entity.getPos().getX() + 0.5);
			double dy = camera.getY() - (entity.getPos().getY() + 0.5);
			double dz = camera.getZ() - (entity.getPos().getZ() + 0.5);
			
			if ((dx * dx + dy * dy + dz * dz) > (entity.viewDistance * entity.viewDistance)) {
				if (!wasOutOfRange) {
                    entity.renderDirty = true;
                    wasOutOfRange = true;
                }

				return;
			} else {
                if (wasOutOfRange) {
					entity.renderDirty = true;
				}

				wasOutOfRange = false;
            }
		}
		
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
			Text line = entity.lines.get(i);
			double width = textRenderer.getWidth(line);
			if (width == 0) continue;

			double dX = switch (entity.textAlignment) {
				case LEFT -> -maxLength / 2D;
				case CENTER -> (maxLength - width) / 2D - maxLength / 2D;
				case CENTER_LEFT -> - (50D / entity.scale) - (width / 2D);
				case CENTER_RIGHT -> (50D / entity.scale) - (width / 2D);
				case RIGHT -> maxLength - width - maxLength / 2D;
			};

			matrices.push();
			matrices.translate(dX, 0, 0);

			TextRenderer.Drawer drawer = (TextRenderer.Drawer) textRenderer.prepare(line.asOrderedText(), 0, i * 12, entity.color, entity.shadow, entity.backgroundColor);

			// TODO: use the light param and add a toggle to make it glow (use LightmapTextureManager.MAX_LIGHT_COORDINATE)
			TextRenderer.GlyphDrawer glyphDrawer = getGlyphDrawer(matrices, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE);

			drawer.draw(glyphDrawer);

			matrices.pop();
		}

		matrices.pop();
	}

	private static TextRenderer.@NotNull GlyphDrawer getGlyphDrawer(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		return new TextRenderer.GlyphDrawer() {
			public void drawGlyph(BakedGlyph.DrawnGlyph glyph) {
				BakedGlyph bakedGlyph = glyph.glyph();
				VertexConsumer vertexConsumer = vertexConsumers.getBuffer(bakedGlyph.getLayer(TextLayerType.NORMAL));
				bakedGlyph.draw(glyph, matrices.peek().getPositionMatrix(), vertexConsumer, light, false);
			}

			public void drawRectangle(BakedGlyph bakedGlyph, BakedGlyph.Rectangle rect) {
				rect = new BakedGlyph.Rectangle(rect.minX() - 4, rect.minY() - 2, rect.maxX() + 4, rect.maxY() + 2, rect.zIndex(), rect.color(), rect.shadowColor(), rect.shadowOffset());
				VertexConsumer vertexConsumer = vertexConsumers.getBuffer(bakedGlyph.getLayer(TextLayerType.NORMAL));
				bakedGlyph.drawRectangle(rect, matrices.peek().getPositionMatrix(), vertexConsumer, light, false);
			}
		};
	}

	@SuppressWarnings("SameParameterValue")
	private void drawFillRect(MatrixStack matrices, VertexConsumerProvider vcp, int x1, int y1, int x2, int y2, int color) {
		float red = (float) (color >> 16 & 255) / 255.0F;
		float green = (float) (color >> 8 & 255) / 255.0F;
		float blue = (float) (color & 255) / 255.0F;
		float alpha = (float) (color >> 24 & 255) / 255.0F;

		// Horrible up to no good hack to get proper translucency sorting :3
		/*final GlyphRenderer renderer = ((TextRendererAccessor) MinecraftClient.getInstance().textRenderer)
			.invokeGetFontStorage(Style.DEFAULT_FONT_ID).getRectangleRenderer();

		final RenderLayer renderLayer = renderer.getLayer(TextLayerType.NORMAL);
		final VertexConsumer consumer = vcp.getBuffer(renderLayer);
		final Matrix4f matrix = matrices.peek().getPositionMatrix();

		renderer.drawRectangle(new GlyphRenderer.Rectangle(
			x1, y1, x2, y2, 0.2f, red, green, blue, alpha
		), matrix, consumer, LightmapTextureManager.MAX_LIGHT_COORDINATE);*/
	}
}
