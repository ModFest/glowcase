package dev.hephaestus.glowcase.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class GuiGraphicsUtil {
	public static final int[] HUE_GRADIENT_COLORS = new int[]{
		ColorUtil.RED, ColorUtil.YELLOW, ColorUtil.GREEN, ColorUtil.CYAN, ColorUtil.BLUE, ColorUtil.MAGENTA, ColorUtil.RED
	};

	/**
	 * Fill a horizontal gradient between two colors.
	 * @apiNote Do not replace the position floats with integers! {@link GuiGraphicsUtil#extractHueGradient(GuiGraphicsExtractor, int, int, int, int)} relies on float position to ensure its gradients can be fully rendered across any specified integer length, instead of requiring the length to be perfectly divisible by a specific number (7 I think) or else it leaves an empty gap.
	 * @see GuiGraphicsExtractor#fillGradient
	 */
	public static void extractHorizontalGradient(GuiGraphicsExtractor graphics, float x0, float y0, float x1, float y1, int startColor, int endColor) {
		graphics.guiRenderState.addGuiElement(
			new GuiElementRenderState() {
				@Override
				public void buildVertices(@NonNull VertexConsumer vertexConsumer) {
					Matrix3x2fStack matrix = graphics.pose();
					vertexConsumer.addVertexWith2DPose(matrix, x0, y0).setColor(startColor);
					vertexConsumer.addVertexWith2DPose(matrix, x0, y1).setColor(startColor);
					vertexConsumer.addVertexWith2DPose(matrix, x1, y1).setColor(endColor);
					vertexConsumer.addVertexWith2DPose(matrix, x1, y0).setColor(endColor);
				}

				@Override
				public @NonNull RenderPipeline pipeline() {
					return RenderPipelines.GUI;
				}

				@Override
				public @NonNull TextureSetup textureSetup() {
					return TextureSetup.noTexture();
				}

				@Override
				public @Nullable ScreenRectangle scissorArea() {
					return null;
				}

				@Override
				public @NonNull ScreenRectangle bounds() {
					return new ScreenRectangle((int) x0, (int) y0, (int) (x1 - x0), (int) (y1 - y0)).transformMaxBounds(graphics.pose());
				}
			}
		);
	}

	/**
	 * Render a hue gradient, starting and ending on red.
	 * @apiNote Do not replace the width float with an integer! See {@link GuiGraphicsUtil#extractHorizontalGradient(GuiGraphicsExtractor, float, float, float, float, int, int)}'s API Note for details.
	 */
	public static void extractHueGradient(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
		float width = x1 - x0;
		int colorCount = HUE_GRADIENT_COLORS.length - 1;
		for (int i = 0; i < colorCount; i++) {
			extractHorizontalGradient(
				graphics,
				x0 + (width / colorCount * i), y0,
				x0 + (width / colorCount * (i + 1)), y1,
				HUE_GRADIENT_COLORS[i], HUE_GRADIENT_COLORS[i + 1]
			);
		}
	}

	/**
	 * Tile a texture across an area with precise texture placement. Allows for better tiling of textures within an integer bounds than otherwise possible.
	 */
	public static void drawPreciseTile(GuiGraphicsExtractor graphics, Identifier spriteId, int x0, int y0, int x1, int y1, float rows, float columns) {
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(spriteId);
		AbstractTexture spriteTexture = Minecraft.getInstance().getTextureManager().getTexture(sprite.atlasLocation());
		float width = x1 - x0;
		float height = y1 - y0;
		float tileWidth = width / columns;
		float tileHeight = height / rows;
		for (int iX = 0; iX < columns; iX++) {
			float tileX = x0 + (tileWidth * iX);
			for (int iY = 0; iY < rows; iY++) {
				float tileY = y0 + (tileHeight * iY);
				drawPreciseTexture(
					graphics, spriteTexture,
					tileX, tileY, tileX + tileWidth, tileY + tileHeight,
					sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1()
				);
			}
		}
	}

	/**
	 * Draw a texture with precise placement beyond the normal integer grid of GUI rendering.
	 */
	public static void drawPreciseTexture(GuiGraphicsExtractor graphics, AbstractTexture texture, float x0, float y0, float x1, float y1, float u0, float u1, float v0, float v1) {
		graphics.guiRenderState.addGuiElement(
			new GuiElementRenderState() {
				@Override
				public void buildVertices(@NonNull VertexConsumer vertexConsumer) {
					Matrix3x2fStack matrix = graphics.pose();
					int color = -1;
					vertexConsumer.addVertexWith2DPose(matrix, x0, y0).setUv(u0, v0).setColor(color);
					vertexConsumer.addVertexWith2DPose(matrix, x0, y1).setUv(u0, v1).setColor(color);
					vertexConsumer.addVertexWith2DPose(matrix, x1, y1).setUv(u1, v1).setColor(color);
					vertexConsumer.addVertexWith2DPose(matrix, x1, y0).setUv(u1, v0).setColor(color);
				}

				@Override
				public @NonNull RenderPipeline pipeline() {
					return RenderPipelines.GUI_TEXTURED;
				}

				@Override
				public @NonNull TextureSetup textureSetup() {
					return TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());
				}

				@Override
				public @Nullable ScreenRectangle scissorArea() {
					return null;
				}

				@Override
				public @NonNull ScreenRectangle bounds() {
					return new ScreenRectangle((int) x0, (int) y0, (int) (x1 - x0), (int) (y1 - y0)).transformMaxBounds(graphics.pose());
				}
			}
		);
	}

}
