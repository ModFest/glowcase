package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ScreenBlockEntity;
import dev.hephaestus.glowcase.client.GlowcaseClient;
//import dev.hephaestus.glowcase.client.GlowcaseRenderLayers;
import dev.hephaestus.glowcase.client.ScreenImageCache;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;

public record ScreenBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ScreenBlockEntity, ScreenBlockEntityRenderer.ScreenRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/screen_block.png");

	public static final int COLOR_SCR_OFF = 0xFF111111;
	public static final int COLOR_SCR_ON = 0xFFFFFFFF;
	public static final int COLOR_SCR_BLUE = 0xFF0000CC;

	public static final int COLOR_TXT_NORMAL = 0xFFAAAAAA;
	public static final int COLOR_TXT_CRASH = 0xFFFFFFFF;

	public static final int SCR_MAX_LINES = 19;

	public static class ScreenRenderState extends BlockEntityRenderState {

	}

	@Override
	public ScreenRenderState createRenderState() {
		return new ScreenRenderState();
	}

	@Override
	public void submit(ScreenRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {

	}
// FIXME 26.1
//	@Override
//	public void render(ScreenBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
//		if (entity.getLevel() == null || entity.getLevel().getBlockState(entity.getBlockPos()).isAir()) return;
//		if (BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getBlockPos()) ||
//			(Minecraft.getInstance().player != null && Minecraft.getInstance().player.getMainHandItem().is(Glowcase.TABLET_ITEM.get())))
//			BlockEntityRenderUtil.renderPlaceholderWithBlockRotation(entity, ITEM_TEXTURE, 1f, matrices, vertexConsumers, -0.1F);
//
//		matrices.pushPose();
//
//		// Positioning
//
//		matrices.translate(.5f, .5f, .5f);
//
//		float rotation = -(entity.getBlockState().getValue(BlockStateProperties.ROTATION_16) * 360) / 16.0F;
//		matrices.mulPose(Axis.YP.rotationDegrees(rotation));
//		matrices.mulPose(Axis.YP.rotationDegrees(180));
//		matrices.translate(entity.getOffset().x(), entity.getOffset().y(), entity.getOffset().z());
//
//		matrices.mulPose(Axis.YP.rotationDegrees(entity.yaw));
//		matrices.mulPose(Axis.XP.rotationDegrees(entity.pitch));
//
//		// Gather needed variables
//
//		Font textRenderer = this.context.getFont();
//
//		int brightness = entity.eink ? light : Lightmap.FULL_BRIGHT;
//
//		String url = entity.url;
//
//		boolean renderBackface = entity.renderBackface;
//
//		// Screen width.height
//		float width = entity.width;
//		float height = entity.height;
//
//		float x1 = -width / 2f;
//		float x2 = width / 2f;
//		float y1 = -height / 2f;
//		float y2 = height / 2f;
//
//		// Start drawing screen
//
//		if (url.isEmpty()) {
//			// Blank screen
//			renderFilledRectangle(COLOR_SCR_OFF, x1, x2, y1, y2, vertexConsumers, matrices, brightness);
//			renderTextCentered(Component.translatableEscape("gui.glowcase.screen.blank"), COLOR_TXT_NORMAL, width, height, matrices, vertexConsumers, textRenderer, brightness);
//		} else {
//			ScreenImageCache screenImageCache = GlowcaseClient.screenImageCache;
//			ScreenImageCache.ScreenTexture image = screenImageCache.getImage(url, entity.getBlockPos());
//			Pair<Integer, Identifier> response = image.getTexture();
//
//			int code = response.getFirst();
//			@Nullable Identifier texture = response.getSecond();
//
//			if (texture != null) {
//				if (!entity.stretch) {
//					Pair<Float, Float> scale = getScale(width, height, image.getWidth(), image.getHeight());
//
//					Float scaled_width = scale.getFirst();
//					Float scaled_height = scale.getSecond();
//
//					x1 = -scaled_width / 2f;
//					x2 = scaled_width / 2f;
//					y1 = -scaled_height / 2f;
//					y2 = scaled_height / 2f;
//				}
//
//				// Actual picture
//				renderPicture(texture, x1, x2, y1, y2, vertexConsumers, matrices, brightness, renderBackface);
//			} else if (code / 100 == 1) {
//				// Loading screen
//				renderFilledRectangle(COLOR_SCR_ON, x1, x2, y1, y2, vertexConsumers, matrices, brightness);
//
//				int frame = (int) (((System.currentTimeMillis() % 4000L) / 250L) % 4);
//				String animation = switch (frame) {
//					case 0 -> "Ooo";
//					case 2 -> "ooO";
//					default -> "oOo";
//				};
//				renderTextCentered(animation, COLOR_TXT_NORMAL, width, height, matrices, vertexConsumers, textRenderer, brightness);
//			} else {
//				// Bluescreen
//				renderFilledRectangle(COLOR_SCR_BLUE, x1, x2, y1, y2, vertexConsumers, matrices, brightness);
//				renderErrCode(code, entity, width, height, vertexConsumers, matrices, textRenderer, brightness);
//			}
//		}
//
//		matrices.popPose();
//	}
//
//	public static void renderPicture(@NotNull Identifier texture, float x1, float x2, float y1, float y2, MultiBufferSource vertexConsumers, PoseStack matrices, int light, boolean renderBackface) {
//		RenderType renderLayer = GlowcaseRenderLayers.getScreen(texture, !renderBackface);
//		VertexConsumer buffer = vertexConsumers.getBuffer(renderLayer);
//
//		PoseStack.Pose matrix = matrices.last();
//		Matrix4f matrix4f = matrix.pose();
//
//		buffer.addVertex(matrix4f, x1, y1, 0f).setColor(0xFFFFFFFF).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, 0f, 0f, 1f);
//		buffer.addVertex(matrix4f, x1, y2, 0f).setColor(0xFFFFFFFF).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, 0f, 0f, 1f);
//		buffer.addVertex(matrix4f, x2, y2, 0f).setColor(0xFFFFFFFF).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, 0f, 0f, 1f);
//		buffer.addVertex(matrix4f, x2, y1, 0f).setColor(0xFFFFFFFF).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, 0f, 0f, 1f);
//	}
//
//	/**
//	 * <p>Responsible for the blue screen of death.
//	 * This screen is always being rendered whenever it could not fetch a given image.</p>
//	 *
//	 * <p>As a replacement for the wanted image, an alternative text description
//	 * as well as the reason why it did not show the text is being shown here.</p>
//	 *
//	 * <p>Note: The code within this method is very messy and someone might want to improve this in the future.</p>
//	 *
//	 * @param code   Error code
//	 * @param width  Width of the screen
//	 * @param height Height of the screen
//	 */
//	public static void renderErrCode(int code, ScreenBlockEntity entity, float width, float height, MultiBufferSource vertexConsumers, PoseStack matrices, Font textRenderer, int light) {
//		// Setup font
//		float lineHeight = height / SCR_MAX_LINES;
//		float font_scale = lineHeight / textRenderer.lineHeight;
//
//		float txt_width = width * 0.95f;
//		float txt_gap = width * 0.05f;
//
//		matrices.translate(txt_width / 2f - (txt_gap / 2f), height / 2f - (lineHeight / 2f), -.1f); // Upper-Left corner
//		matrices.scale(-font_scale, -font_scale, -.1f);
//
//		// Alt-Text
//
//		String alt = Component.translatable("gui.glowcase.screen.alt", entity.alt).getString();
//		ArrayList<String> lines = wrap(alt, font_scale, txt_width, textRenderer);
//
//		matrices.translate(0, textRenderer.lineHeight * ((int) (SCR_MAX_LINES / 2) - 1), 0f);  // Move to second half of screen
//
//		int moved_lines = 0;
//		for (int i = 0; i < lines.size(); i++) {
//			String line = lines.get(i);
//
//			// No overflows here
//			int limit = (SCR_MAX_LINES / 2 - 1);
//			if (i > limit)
//				break;
//			else if (i == limit && i + 1 != lines.size())
//				line = line + "…";
//
//			moved_lines++;
//			matrices.translate(0, textRenderer.lineHeight, 0f); // One line down
//			textRenderer.drawInBatch(line, 0, 0, COLOR_TXT_CRASH, true, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, light);
//		}
//
//		// Error message
//
//		matrices.translate(0, -textRenderer.lineHeight * ((int) (SCR_MAX_LINES / 2) - 1), 0f); // Move cursor back to Upper-Left
//		matrices.translate(0, -textRenderer.lineHeight * moved_lines, 0f);
//
//		matrices.translate(0, textRenderer.lineHeight * 4, 0f);
//
//		MutableComponent hint = Component.translatableWithFallback("gui.glowcase.screen.hint." + code, "");
//		String error_msg = Component.translatable("gui.glowcase.screen.error", code).append(" ").append(hint).getString();
//		lines = wrap(error_msg, font_scale, txt_width, textRenderer);
//
//		moved_lines = 0;
//		for (int i = 0; i < lines.size(); i++) {
//			String line = lines.get(i);
//
//			// No overflows here
//			int limit = (SCR_MAX_LINES / 2) - 7;
//			if (i > limit)
//				break;
//			else if (i == limit && i + 1 != lines.size())
//				line = line + "…";
//
//			moved_lines++;
//			matrices.translate(0, textRenderer.lineHeight, 0f); // One line down
//			textRenderer.drawInBatch(line, 0, 0, COLOR_TXT_CRASH, true, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, light);
//		}
//
//		// Important face
//
//		matrices.translate(0, -textRenderer.lineHeight * (moved_lines + 3), 0f); // Undo cursor positioning
//		matrices.scale(3f, 3f, 1f);
//		textRenderer.drawInBatch(":3", 0, 0, COLOR_TXT_CRASH, true, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, light);
//	}
//
//	@SuppressWarnings("SameParameterValue")
//	public static void renderTextCentered(String text, int color, float scr_width, float scr_height, PoseStack matrices, MultiBufferSource vertexConsumers, Font textRenderer, int light) {
//		renderTextCentered(Component.literal(text), color, scr_width, scr_height, matrices, vertexConsumers, textRenderer, light);
//	}
//
//	public static void renderTextCentered(MutableComponent text, int color, float scr_width, float scr_height, PoseStack matrices, MultiBufferSource vertexConsumers, Font textRenderer, int light) {
//		// Scale font
//		float max_font_width = scr_width / textRenderer.width(text);
//		float max_font_height = scr_height / textRenderer.lineHeight;
//
//		float font_scale_factor = Math.min(max_font_width, max_font_height) * .6f;
//
//		// Apply
//		matrices.scale(-font_scale_factor, -font_scale_factor, -0.5f);
//		matrices.translate(-textRenderer.width(text) / 2f, -textRenderer.lineHeight / 2f, .1f); // Remove offset of string
//
//		textRenderer.drawInBatch(text, 0, 0, color, true, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, light);
//	}
//
//	/**
//	 * Returns the scale factors needed to ensure a picture does not go out of bounds of the given width/height.
//	 */
	public static Pair<Float, Float> getScale(float width, float height, int img_width, int img_height) {
		float width_scale = width / img_width;
		float height_scale = height / img_height;
		float final_scale = Math.min(width_scale, height_scale);

		float scaled_width = (img_width * final_scale);
		float scaled_height = (img_height * final_scale);

		return new Pair<>(scaled_width, scaled_height);
	}
//
//	/**
//	 * Used to trim off the string to fit the given width in a way where words are not broken apart.
//	 * (aka Word wrapping)
//	 *
//	 * @param font_scale Size of the font in relation to the screens sizes.
//	 * @param txt_width  Available width of the screen for the text.
//	 * @return A list of strings where all fit in the expected width.
//	 */
//	private static ArrayList<String> wrap(String text, float font_scale, float txt_width, Font textRenderer) {
//		ArrayList<String> result = new ArrayList<>();
//
//		StringBuilder lineBuilder = new StringBuilder();
//
//		for (String word : text.split(" ")) {
//			if ((textRenderer.width(lineBuilder + word) * font_scale) >= txt_width) {
//				result.add(lineBuilder.toString().trim());
//				lineBuilder = new StringBuilder();
//			}
//			lineBuilder.append(word).append(" ");
//		}
//
//		if (!lineBuilder.isEmpty())
//			result.add(lineBuilder.toString().trim());
//
//		return result;
//	}
//
//	private static void renderFilledRectangle(int color, float x1, float x2, float y1, float y2, MultiBufferSource vertexConsumers, PoseStack matrices, int light) {
//		VertexConsumer buffer = vertexConsumers.getBuffer(RenderTypes.textBackground());
//		Matrix4f matrix4f = matrices.last().pose();
//
//		buffer.addVertex(matrix4f, x1, y1, 0f).setColor(color).setLight(light);
//		buffer.addVertex(matrix4f, x1, y2, 0f).setColor(color).setLight(light);
//		buffer.addVertex(matrix4f, x2, y2, 0f).setColor(color).setLight(light);
//		buffer.addVertex(matrix4f, x2, y1, 0f).setColor(color).setLight(light);
//	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public boolean shouldRender(ScreenBlockEntity blockEntity, Vec3 vec3) {
		return true;
	}
}
