package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.datafixers.util.Pair;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ScreenBlockEntity;
import dev.hephaestus.glowcase.client.GlowcaseClient;
import dev.hephaestus.glowcase.client.GlowcaseRenderLayers;
import dev.hephaestus.glowcase.client.ScreenImageCache;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;

public record ScreenBlockEntityRenderer(BlockEntityRendererFactory.Context context) implements BlockEntityRenderer<ScreenBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/screen_block.png");

	public static final int COLOR_SCR_OFF = 0xFF111111;
	public static final int COLOR_SCR_ON = 0xFFFFFFFF;
	public static final int COLOR_SCR_BLUE = 0xFF0000CC;

	public static final int COLOR_TXT_NORMAL = 0xFFAAAAAA;
	public static final int COLOR_TXT_CRASH = 0xFFFFFFFF;

	public static final int SCR_MAX_LINES = 19;

	@Override
	public void render(ScreenBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
		if (entity.getWorld() == null || entity.getWorld().getBlockState(entity.getPos()).isAir()) return;
		if (BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getPos()) ||
			(MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.getMainHandStack().isOf(Glowcase.TABLET_ITEM.get())))
			BlockEntityRenderUtil.renderPlaceholderWithBlockRotation(entity, ITEM_TEXTURE, 1f, matrices, vertexConsumers, -0.1F);

		matrices.push();

		// Positioning

		matrices.translate(.5f, .5f, .5f);

		float rotation = -(entity.getCachedState().get(Properties.ROTATION) * 360) / 16.0F;
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
		matrices.translate(entity.getOffset().x(), entity.getOffset().y(), entity.getOffset().z());

		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.yaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.pitch));

		// Gather needed variables

		TextRenderer textRenderer = this.context.getTextRenderer();

		int brightness = entity.eink ? light : LightmapTextureManager.MAX_LIGHT_COORDINATE;

		String url = entity.url;

		boolean renderBackface = entity.renderBackface;

		// Screen width.height
		float width = entity.width;
		float height = entity.height;

		float x1 = -width / 2f;
		float x2 = width / 2f;
		float y1 = -height / 2f;
		float y2 = height / 2f;

		// Start drawing screen

		if (url.isEmpty()) {
			// Blank screen
			renderFilledRectangle(COLOR_SCR_OFF, x1, x2, y1, y2, vertexConsumers, matrices, brightness);
			renderTextCentered(Text.stringifiedTranslatable("gui.glowcase.screen.blank"), COLOR_TXT_NORMAL, width, height, matrices, vertexConsumers, textRenderer, brightness);
		} else {
			ScreenImageCache screenImageCache = GlowcaseClient.screenImageCache;
			ScreenImageCache.ScreenTexture image = screenImageCache.getImage(url, entity.getPos());
			Pair<Integer, Identifier> response = image.getTexture();

			int code = response.getFirst();
			@Nullable Identifier texture = response.getSecond();

			if (texture != null) {
				if (!entity.stretch) {
					Pair<Float, Float> scale = getScale(width, height, image.getWidth(), image.getHeight());

					Float scaled_width = scale.getFirst();
					Float scaled_height = scale.getSecond();

					x1 = -scaled_width / 2f;
					x2 = scaled_width / 2f;
					y1 = -scaled_height / 2f;
					y2 = scaled_height / 2f;
				}

				// Actual picture
				renderPicture(texture, x1, x2, y1, y2, vertexConsumers, matrices, brightness, renderBackface);
			} else if (code / 100 == 1) {
				// Loading screen
				renderFilledRectangle(COLOR_SCR_ON, x1, x2, y1, y2, vertexConsumers, matrices, brightness);

				int frame = (int) (((System.currentTimeMillis() % 4000L) / 250L) % 4);
				String animation = switch (frame) {
					case 0 -> "Ooo";
					case 2 -> "ooO";
					default -> "oOo";
				};
				renderTextCentered(animation, COLOR_TXT_NORMAL, width, height, matrices, vertexConsumers, textRenderer, brightness);
			} else {
				// Bluescreen
				renderFilledRectangle(COLOR_SCR_BLUE, x1, x2, y1, y2, vertexConsumers, matrices, brightness);
				renderErrCode(code, entity, width, height, vertexConsumers, matrices, textRenderer, brightness);
			}
		}

		matrices.pop();
	}

	public static void renderPicture(@NotNull Identifier texture, float x1, float x2, float y1, float y2, VertexConsumerProvider vertexConsumers, MatrixStack matrices, int light, boolean renderBackface) {
		RenderLayer renderLayer = GlowcaseRenderLayers.getScreen(texture, !renderBackface);
		VertexConsumer buffer = vertexConsumers.getBuffer(renderLayer);

		MatrixStack.Entry matrix = matrices.peek();
		Matrix4f matrix4f = matrix.getPositionMatrix();

		buffer.vertex(matrix4f, x1, y1, 0f).color(0xFFFFFFFF).texture(1f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix, 0f, 0f, 1f);
		buffer.vertex(matrix4f, x1, y2, 0f).color(0xFFFFFFFF).texture(1f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix, 0f, 0f, 1f);
		buffer.vertex(matrix4f, x2, y2, 0f).color(0xFFFFFFFF).texture(0f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix, 0f, 0f, 1f);
		buffer.vertex(matrix4f, x2, y1, 0f).color(0xFFFFFFFF).texture(0f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix, 0f, 0f, 1f);
	}

	/**
	 * <p>Responsible for the blue screen of death.
	 * This screen is always being rendered whenever it could not fetch a given image.</p>
	 *
	 * <p>As a replacement for the wanted image, an alternative text description
	 * as well as the reason why it did not show the text is being shown here.</p>
	 *
	 * <p>Note: The code within this method is very messy and someone might want to improve this in the future.</p>
	 *
	 * @param code   Error code
	 * @param width  Width of the screen
	 * @param height Height of the screen
	 */
	public static void renderErrCode(int code, ScreenBlockEntity entity, float width, float height, VertexConsumerProvider vertexConsumers, MatrixStack matrices, TextRenderer textRenderer, int light) {
		// Setup font
		float lineHeight = height / SCR_MAX_LINES;
		float font_scale = lineHeight / textRenderer.fontHeight;

		float txt_width = width * 0.95f;
		float txt_gap = width * 0.05f;

		matrices.translate(txt_width / 2f - (txt_gap / 2f), height / 2f - (lineHeight / 2f), -.1f); // Upper-Left corner
		matrices.scale(-font_scale, -font_scale, -.1f);

		// Alt-Text

		String alt = Text.translatable("gui.glowcase.screen.alt", entity.alt).getString();
		ArrayList<String> lines = wrap(alt, font_scale, txt_width, textRenderer);

		matrices.translate(0, textRenderer.fontHeight * ((int) (SCR_MAX_LINES / 2) - 1), 0f);  // Move to second half of screen

		int moved_lines = 0;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

			// No overflows here
			int limit = (SCR_MAX_LINES / 2 - 1);
			if (i > limit)
				break;
			else if (i == limit && i + 1 != lines.size())
				line = line + "…";

			moved_lines++;
			matrices.translate(0, textRenderer.fontHeight, 0f); // One line down
			textRenderer.draw(line, 0, 0, COLOR_TXT_CRASH, true, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
		}

		// Error message

		matrices.translate(0, -textRenderer.fontHeight * ((int) (SCR_MAX_LINES / 2) - 1), 0f); // Move cursor back to Upper-Left
		matrices.translate(0, -textRenderer.fontHeight * moved_lines, 0f);

		matrices.translate(0, textRenderer.fontHeight * 4, 0f);

		MutableText hint = Text.translatableWithFallback("gui.glowcase.screen.hint." + code, "");
		String error_msg = Text.translatable("gui.glowcase.screen.error", code).append(" ").append(hint).getString();
		lines = wrap(error_msg, font_scale, txt_width, textRenderer);

		moved_lines = 0;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

			// No overflows here
			int limit = (SCR_MAX_LINES / 2) - 7;
			if (i > limit)
				break;
			else if (i == limit && i + 1 != lines.size())
				line = line + "…";

			moved_lines++;
			matrices.translate(0, textRenderer.fontHeight, 0f); // One line down
			textRenderer.draw(line, 0, 0, COLOR_TXT_CRASH, true, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
		}

		// Important face

		matrices.translate(0, -textRenderer.fontHeight * (moved_lines + 3), 0f); // Undo cursor positioning
		matrices.scale(3f, 3f, 1f);
		textRenderer.draw(":3", 0, 0, COLOR_TXT_CRASH, true, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
	}

	@SuppressWarnings("SameParameterValue")
	public static void renderTextCentered(String text, int color, float scr_width, float scr_height, MatrixStack matrices, VertexConsumerProvider vertexConsumers, TextRenderer textRenderer, int light) {
		renderTextCentered(Text.literal(text), color, scr_width, scr_height, matrices, vertexConsumers, textRenderer, light);
	}

	public static void renderTextCentered(MutableText text, int color, float scr_width, float scr_height, MatrixStack matrices, VertexConsumerProvider vertexConsumers, TextRenderer textRenderer, int light) {
		// Scale font
		float max_font_width = scr_width / textRenderer.getWidth(text);
		float max_font_height = scr_height / textRenderer.fontHeight;

		float font_scale_factor = Math.min(max_font_width, max_font_height) * .6f;

		// Apply
		matrices.scale(-font_scale_factor, -font_scale_factor, -0.5f);
		matrices.translate(-textRenderer.getWidth(text) / 2f, -textRenderer.fontHeight / 2f, .1f); // Remove offset of string

		textRenderer.draw(text, 0, 0, color, true, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
	}

	/**
	 * Returns the scale factors needed to ensure a picture does not go out of bounds of the given width/height.
	 */
	public static Pair<Float, Float> getScale(float width, float height, int img_width, int img_height) {
		float width_scale = width / img_width;
		float height_scale = height / img_height;
		float final_scale = Math.min(width_scale, height_scale);

		float scaled_width = (img_width * final_scale);
		float scaled_height = (img_height * final_scale);

		return new Pair<>(scaled_width, scaled_height);
	}

	/**
	 * Used to trim off the string to fit the given width in a way where words are not broken apart.
	 * (aka Word wrapping)
	 *
	 * @param font_scale Size of the font in relation to the screens sizes.
	 * @param txt_width  Available width of the screen for the text.
	 * @return A list of strings where all fit in the expected width.
	 */
	private static ArrayList<String> wrap(String text, float font_scale, float txt_width, TextRenderer textRenderer) {
		ArrayList<String> result = new ArrayList<>();

		StringBuilder lineBuilder = new StringBuilder();

		for (String word : text.split(" ")) {
			if ((textRenderer.getWidth(lineBuilder + word) * font_scale) >= txt_width) {
				result.add(lineBuilder.toString().trim());
				lineBuilder = new StringBuilder();
			}
			lineBuilder.append(word).append(" ");
		}

		if (!lineBuilder.isEmpty())
			result.add(lineBuilder.toString().trim());

		return result;
	}

	private static void renderFilledRectangle(int color, float x1, float x2, float y1, float y2, VertexConsumerProvider vertexConsumers, MatrixStack matrices, int light) {
		VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getTextBackground());
		Matrix4f matrix4f = matrices.peek().getPositionMatrix();

		buffer.vertex(matrix4f, x1, y1, 0f).color(color).light(light);
		buffer.vertex(matrix4f, x1, y2, 0f).color(color).light(light);
		buffer.vertex(matrix4f, x2, y2, 0f).color(color).light(light);
		buffer.vertex(matrix4f, x2, y1, 0f).color(color).light(light);
	}

	@Override
	public boolean rendersOutsideBoundingBox() {
		return true;
	}

	@Override
	public boolean isInRenderDistance(ScreenBlockEntity blockEntity, Vec3d pos) {
		return true;
	}
}
