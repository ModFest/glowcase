package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ScreenBlockEntity;
import dev.hephaestus.glowcase.client.GlowcaseClient;
import dev.hephaestus.glowcase.client.GlowcaseRenderTypes;
import dev.hephaestus.glowcase.client.ScreenImageCache;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import dev.hephaestus.glowcase.client.util.Quaternionsf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public record ScreenBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ScreenBlockEntity, ScreenBlockEntityRenderer.ScreenRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/screen_block.png");

	private static final Component TEXT_BLANK = Component.translatableEscape("gui.glowcase.screen.blank");
	// Hardcoding these 4 are *fine* as it's not actually text.
	private static final FormattedCharSequence FACE = FormattedCharSequence.forward(":3", Style.EMPTY);
	private static final FormattedCharSequence ANI_1 = FormattedCharSequence.forward("Ooo", Style.EMPTY);
	private static final FormattedCharSequence ANI_2 = FormattedCharSequence.forward("oOo", Style.EMPTY);
	private static final FormattedCharSequence ANI_3 = FormattedCharSequence.forward("ooO", Style.EMPTY);

	public static final int COLOR_SCR_OFF = 0xFF111111;
	public static final int COLOR_SCR_ON = 0xFFFFFFFF;
	public static final int COLOR_SCR_BLUE = 0xFF0000CC;

	public static final int COLOR_TXT_NORMAL = 0xFFAAAAAA;
	public static final int COLOR_TXT_CRASH = 0xFFFFFFFF;

	public static final int SCR_MAX_LINES = 19;

	public static class ScreenRenderState extends BlockEntityRenderState {
		public boolean renderBackface;
		public boolean stretch;
		public @Nullable ScreenImageCache.ScreenTexture image;
		public String alt;

		public Vector3f offset;

		public float width;
		public float height;

		public int rotation;
		public float yaw;
		public float pitch;

		public float rotationInDegrees() {
			return -(this.rotation * 360.0F) / 16.0F;
		}
	}

	@Override
	public ScreenRenderState createRenderState() {
		return new ScreenRenderState();
	}

	@Override
	public void extractRenderState(final ScreenBlockEntity blockEntity, final ScreenRenderState state, final float partialTicks, final Vec3 cameraPosition, final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

		if (!blockEntity.eink) {
			state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
		}

		state.renderBackface = blockEntity.renderBackface;
		state.stretch = blockEntity.stretch;

		if (!blockEntity.url.isEmpty()) {
			ScreenImageCache screenImageCache = GlowcaseClient.screenImageCache;
			state.image = screenImageCache.getImage(blockEntity.url, blockEntity.getBlockPos());
		} else {
			state.image = null;
		}

		state.alt = blockEntity.alt;

		state.offset = blockEntity.getOffset();

		state.width = blockEntity.width;
		state.height = blockEntity.height;

		state.rotation = blockEntity.getBlockState().getValue(BlockStateProperties.ROTATION_16);
		state.yaw = blockEntity.yaw;
		state.pitch = blockEntity.pitch;
	}

	@Override
	public void submit(ScreenRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (BlockEntityRenderUtil.shouldRenderPlaceholder(state.blockPos) ||
			(Minecraft.getInstance().player != null && Minecraft.getInstance().player.getMainHandItem().is(Glowcase.TABLET_ITEM.get()))) {
			BlockEntityRenderUtil.renderPlaceholderWithBlockRotation(state, state.rotation, ITEM_TEXTURE, 1.0F, matrices, submitNodeCollector);
		}

		matrices.pushPose();

		// Positioning

		matrices.translate(.5f, .5f, .5f);

		float rotation = state.rotationInDegrees();
		matrices.mulPose(Axis.YP.rotationDegrees(rotation + 180));
		matrices.translate(state.offset.x(), state.offset.y(), state.offset.z());

		matrices.mulPose(Quaternionsf.rotateDegreesYXZ(state.yaw, state.pitch, 0));

		renderScreen(state, matrices, submitNodeCollector);

		matrices.popPose();
	}

	private void renderScreen(ScreenRenderState state, PoseStack poses, SubmitNodeCollector collector) {
		// Gather needed variables

		final Plane plane = new Plane(state.width, state.height);

		// Start drawing screen

		if (state.image == null) {
			// Blank screen
			submitFilledRectangle(COLOR_SCR_OFF, plane, collector, poses, state.lightCoords);

			submitTextCentered(TEXT_BLANK, COLOR_TXT_NORMAL, state.width, state.height, poses, collector, context.font(), state.lightCoords);
			return;
		}

		ScreenImageCache.ScreenTexture image = state.image;
		Pair<Integer, Identifier> response = image.getTexture();

		int code = response.getFirst();
		@Nullable Identifier texture = response.getSecond();

		if (texture != null) {
			final Plane rescaledPlane;
			if (!state.stretch) {
				rescaledPlane = getScale(state.width, state.height, image.getWidth(), image.getHeight());
			} else {
				rescaledPlane = plane;
			}

			submitPicture(poses, collector, texture, state.renderBackface, rescaledPlane, state.lightCoords);

			return;
		}

		if (code / 100 == 1) {
			// Loading screen
			submitFilledRectangle(COLOR_SCR_ON, plane, collector, poses, state.lightCoords);

			int frame = (int) (((System.currentTimeMillis() % 4000L) / 250L) % 4);
			FormattedCharSequence animation = switch (frame) {
				case 0 -> ANI_1;
				case 2 -> ANI_3;
				default -> ANI_2;
			};

			submitTextCentered(animation, COLOR_TXT_NORMAL, state.width, state.height, poses, collector, context.font(), state.lightCoords);
		} else {
			// Bluescreen
			submitFilledRectangle(COLOR_SCR_BLUE, plane, collector, poses, state.lightCoords);
			renderErrCode(code, state, state.width, state.height, collector, poses, context.font(), state.lightCoords);
		}
	}

	public static void submitPicture(PoseStack poses, SubmitNodeCollector collector, Identifier texture, boolean backface, Plane plane, int light) {
		collector.submitCustomGeometry(
			poses,
			GlowcaseRenderTypes.getScreen(texture, !backface),
			(pose, buffer) -> {
				buffer.addVertex(pose, plane.x1, plane.y1, 0f)
					.setColor(0xFFFFFFFF)
					.setUv(1f, 1f)
					.setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(light)
					.setNormal(pose, 0f, 0f, 1f);
				buffer.addVertex(pose, plane.x1, plane.y2, 0f)
					.setColor(0xFFFFFFFF)
					.setUv(1f, 0f)
					.setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(light)
					.setNormal(pose, 0f, 0f, 1f);
				buffer.addVertex(pose, plane.x2, plane.y2, 0f)
					.setColor(0xFFFFFFFF)
					.setUv(0f, 0f)
					.setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(light)
					.setNormal(pose, 0f, 0f, 1f);
				buffer.addVertex(pose, plane.x2, plane.y1, 0f)
					.setColor(0xFFFFFFFF)
					.setUv(0f, 1f)
					.setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(light)
					.setNormal(pose, 0f, 0f, 1f);
			}
		);
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
	public static void renderErrCode(int code, ScreenRenderState entity, float width, float height, SubmitNodeCollector collector, PoseStack poses, Font font, int light) {
		// Setup font
		float lineHeight = height / SCR_MAX_LINES;
		float font_scale = lineHeight / font.lineHeight;

		float txt_width = width * 0.95f;
		float txt_gap = width * 0.05f;

		poses.translate(txt_width / 2f - (txt_gap / 2f), height / 2f - (lineHeight / 2f), -.01f); // Upper-Left corner
		poses.scale(-font_scale, -font_scale, -.1f);

		// Alt-Text

		String alt = Component.translatable("gui.glowcase.screen.alt", entity.alt).getString();
		List<Component> lines = wordWrap(alt, font_scale, txt_width, font);

		poses.translate(0, font.lineHeight * ((SCR_MAX_LINES / 2) - 1), 0f);  // Move to second half of screen

		int moved_lines = 0;
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);

			// No overflows here
			int limit = (SCR_MAX_LINES / 2 - 1);
			if (i > limit)
				break;
			else if (i == limit && i + 1 != lines.size())
				line = Component.empty().append(line).append("…");

			moved_lines++;
			poses.translate(0, font.lineHeight, 0f); // One line down
			collector.submitText(poses, 0, 0, line.getVisualOrderText(), true, Font.DisplayMode.NORMAL, light, COLOR_TXT_CRASH, 0, 0);
		}

		// Error message

		poses.translate(0, -font.lineHeight * ((SCR_MAX_LINES / 2) - 1), 0f); // Move cursor back to Upper-Left
		poses.translate(0, -font.lineHeight * moved_lines, 0f);

		poses.translate(0, font.lineHeight * 4, 0f);

		MutableComponent hint = Component.translatableWithFallback("gui.glowcase.screen.hint." + code, "");
		String error_msg = Component.translatable("gui.glowcase.screen.error", code).append(" ").append(hint).getString();
		lines = wordWrap(error_msg, font_scale, txt_width, font);

		moved_lines = 0;
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);

			// No overflows here
			int limit = (SCR_MAX_LINES / 2) - 7;
			if (i > limit)
				break;
			else if (i == limit && i + 1 != lines.size())
				line = Component.empty().append(line).append("…");

			moved_lines++;
			poses.translate(0, font.lineHeight, 0f); // One line down
			collector.submitText(poses, 0, 0, line.getVisualOrderText(), true, Font.DisplayMode.NORMAL, light, COLOR_TXT_CRASH, 0, 0);
		}

		// Important face

		poses.translate(0, -font.lineHeight * (moved_lines + 3), 0f); // Undo cursor positioning
		poses.scale(3f, 3f, 1f);
		collector.submitText(poses, 0, 0, FACE, true, Font.DisplayMode.NORMAL, light, COLOR_TXT_CRASH, 0, 0);
	}

	public static void submitTextCentered(Component text, int color, float scr_width, float scr_height, PoseStack matrices, SubmitNodeCollector collector, Font font, int light) {
		submitTextCentered(text.getVisualOrderText(), color, scr_width, scr_height, matrices, collector, font, light);
	}

	public static void submitTextCentered(FormattedCharSequence text, int color, float scr_width, float scr_height, PoseStack matrices, SubmitNodeCollector collector, Font font, int light) {
		// Scale font
		float max_font_width = scr_width / font.width(text);
		float max_font_height = scr_height / font.lineHeight;

		float font_scale_factor = Math.min(max_font_width, max_font_height) * .6f;

		// Apply
		matrices.scale(-font_scale_factor, -font_scale_factor, -0.5f);
		matrices.translate(-font.width(text) / 2f, -font.lineHeight / 2f, .1f); // Remove offset of string

		collector.submitText(matrices, 0, 0, text, true, Font.DisplayMode.NORMAL, light, color, 0, 0);
	}

	/**
	 * Returns the scale factors needed to ensure a picture does not go out of bounds of the given width/height.
	 */
	public static Plane getScale(float width, float height, int img_width, int img_height) {
		float width_scale = width / img_width;
		float height_scale = height / img_height;
		float final_scale = Math.min(width_scale, height_scale);

		float scaled_width = (img_width * final_scale);
		float scaled_height = (img_height * final_scale);

		return new Plane(scaled_width, scaled_height);
	}

	/**
	 * Used to trim off the string to fit the given width in a way where words are not broken apart.
	 * (aka Word wrapping)
	 *
	 * @param font_scale Size of the font in relation to the screens sizes.
	 * @param txt_width  Available width of the screen for the text.
	 * @return A list of strings where all fit in the expected width.
	 */
	private static List<Component> wordWrap(String text, float font_scale, float txt_width, Font font) {
		ArrayList<Component> result = new ArrayList<>();

		StringBuilder lineBuilder = new StringBuilder();

		for (String word : text.split(" ")) {
			if ((font.width(lineBuilder + word) * font_scale) >= txt_width) {
				result.add(Component.literal(lineBuilder.toString().trim()));
				lineBuilder = new StringBuilder();
			}
			lineBuilder.append(word).append(" ");
		}

		if (!lineBuilder.isEmpty())
			result.add(Component.literal(lineBuilder.toString().trim()));

		return result;
	}

	private static void submitFilledRectangle(int color, Plane plane, SubmitNodeCollector collector, PoseStack poseStack, int light) {
		collector.submitCustomGeometry(
			poseStack,
			RenderTypes.textBackground(),
			(pose, buffer) -> {
				buffer.addVertex(pose, plane.x1, plane.y1, 0f).setColor(color).setLight(light);
				buffer.addVertex(pose, plane.x1, plane.y2, 0f).setColor(color).setLight(light);
				buffer.addVertex(pose, plane.x2, plane.y2, 0f).setColor(color).setLight(light);
				buffer.addVertex(pose, plane.x2, plane.y1, 0f).setColor(color).setLight(light);
			}
		);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public boolean shouldRender(ScreenBlockEntity blockEntity, Vec3 vec3) {
		return true;
	}

	public record Plane(
		float x1,
		float x2,
		float y1,
		float y2
	) {
		private Plane(float width, float height) {
			this(
				-width / 2,
				width / 2,
				-height / 2,
				height / 2
			);
		}

		public float width() {
			return x2 - x1;
		}

		public float height() {
			return y2 - y1;
		}
	}
}
