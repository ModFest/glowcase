package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import dev.hephaestus.glowcase.client.util.Quaternionsf;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
public class TextBlockEntityRenderer implements BakedBlockEntityRenderer<TextBlockEntity, TextBlockEntityRenderer.TextRenderState, TextBlockEntityRenderer.TextRenderState> {
	public static final Identifier ITEM_TEXTURE = Glowcase.id("textures/item/text_block.png");
	private final Font font;

	public TextBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
		this.font = ctx.font();
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	public static class TextRenderState extends BlockEntityRenderState {
		public int rotation16;
		public List<FormattedCharSequence> lines = List.of();
		public TextBlockEntity.TextAlignment textAlignment;
		public TextBlockEntity.HorizontalAlignment horizontalAlignment;
		public TextBlockEntity.ZOffset zOffset;
		public boolean shadow;
		public float scale = 1;
		public int color;
		public int backgroundColor;
		public Vec3 offset;
		public Vec3 rotation; // Yaw, pitch, roll
	}

	// Unbaked rendering

	@Override
	public TextRenderState createRenderState() {
		return new TextRenderState();
	}

	@Override
	public boolean shouldRender(TextBlockEntity blockEntity, Vec3 cameraPosition) {
		return BakedBlockEntityRenderer.super.shouldRender(blockEntity, cameraPosition) && shouldRenderPlaceholder(blockEntity);
	}

	private boolean isEmpty(TextBlockEntity blockEntity) {
		return !blockEntity.lines.isEmpty() && blockEntity.lines.stream().allMatch(t -> t.getString().isBlank());
	}

	private boolean shouldRenderPlaceholder(TextBlockEntity blockEntity) {
		return blockEntity.lines.stream().allMatch(t -> t.getString().isBlank()) || BlockEntityRenderUtil.shouldRenderPlaceholder(blockEntity.getBlockPos());
	}

	@Override
	public void extractRenderState(TextBlockEntity blockEntity, TextRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BakedBlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.rotation16 = blockEntity.getBlockState().getValue(BlockStateProperties.ROTATION_16);
		state.zOffset = blockEntity.zOffset;
	}

	@Override
	public void submitForRendering(TextRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		BlockEntityRenderUtil.renderPlaceholderWithBlockRotation(state, state.rotation16, ITEM_TEXTURE, 1.0F, poseStack, submitNodeCollector, state.zOffset == TextBlockEntity.ZOffset.CENTER ? 0.01F : state.zOffset == TextBlockEntity.ZOffset.FRONT ? 0.4F : -0.4F);
	}

	// Baked rendering

	@Override
	public void extractBakingRenderState(TextBlockEntity blockEntity, TextRenderState state, int light) {
		BakedBlockEntityRenderer.super.extractBakingRenderState(blockEntity, state, light);
		state.zOffset = blockEntity.zOffset;
		state.rotation16 = blockEntity.getBlockState().getValue(BlockStateProperties.ROTATION_16);

		state.lines = blockEntity.lines.stream().map(Component::getVisualOrderText).toList();
		state.textAlignment = blockEntity.textAlignment;
		state.horizontalAlignment = blockEntity.horizontalAlignment;
		state.zOffset = blockEntity.zOffset;
		state.shadow = blockEntity.shadow;
		state.scale = blockEntity.scale;
		state.color = blockEntity.color;
		state.backgroundColor = blockEntity.backgroundColor;
		state.offset = blockEntity.offset;
		state.rotation = blockEntity.rotation;
	}

	@Override
	public TextRenderState createBakedRenderState() {
		return new TextRenderState();
	}

	@Override
	public boolean shouldBake(TextBlockEntity entity) {
		return !isEmpty(entity);
	}

	@Override
	public void submitForBaking(TextRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
		int maxWidth = 0;
		for (var line : state.lines) {
			maxWidth = Math.max(maxWidth, this.font.width(line));
		}
		// + 3 required for both padding between lines and internal padding with underline.
		// <u>Changing</u> it is <b>futile</b>.
		final int height = this.font.lineHeight + 3;

		poseStack.pushPose();
		poseStack.translate(0.5D, 0.5D, 0.5D);
		// 2D rendering of the font has Y axis going down, not up
		poseStack.scale(1, -1, 1);

		float rotation = -(state.rotation16 * 360) / 16.0F;
		poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

		// Must be done after rotation.
		// Else it's always along global Z-axis as unintended.
		switch (state.zOffset) {
			case FRONT -> poseStack.translate(0D, 0D, 0.4D);
			case BACK -> poseStack.translate(0D, 0D, -0.4D);
		}

		// Translate extra offsets (negative y to match Minecraft's coordinates)
		poseStack.translate(state.offset.x(), -state.offset.y(), state.offset.z());

		// Apply extra rotations
		poseStack.mulPose(Quaternionsf.rotateDegreesYXZ((float) state.rotation.x(), (float) state.rotation.y(), (float) state.rotation.z()));

		// Scale for parity with older versions of Glowcase.
		// Unless Mojang ever changes the rendering scale, this shall remain.
		final float scale = 0.010416667F * state.scale;
		poseStack.scale(scale, scale, scale);

		// Adjusts the text positioning to parity. For some reason, the text moved up;
		// 24/26.d is roughly the required movement down for 100% parity.
		poseStack.translate(0, Math.fma(state.lines.size(), height / -2.d, 24.d / 16.d), 0D);

		switch (state.horizontalAlignment) {
			case LEFT -> poseStack.translate(-maxWidth / 2F, 0, 0);
			case RIGHT -> poseStack.translate(maxWidth / 2F, 0, 0);
		}

		for (int i = 0; i < state.lines.size(); ++i) {
			var line = state.lines.get(i);

			final int width = this.font.width(line);
			if (width == 0) {
				continue;
			}

			final float x = switch (state.textAlignment) {
				case LEFT -> -maxWidth / 2F;
				case CENTER -> (maxWidth - width) / 2F - maxWidth / 2F;
				case CENTER_LEFT -> -(50F / state.scale) - (width / 2F);
				case CENTER_RIGHT -> (50F / state.scale) - (width / 2F);
				case RIGHT -> maxWidth - width - maxWidth / 2F;
			};
			final float y = i * height;

			// Hey.
			//
			// What if I said: We need the hack again
			// :333

			// padding: 1pt 2pt
			// No, you cannot replace this with submitText or its future descendants.
			// It has been tried 3 times now. It genuinely looks worse,
			// and is inaccessible with bold and underline.
			submitFilledRectangle(
				submitNodeCollector,
				poseStack,
				RenderTypes.textBackground(),
				x - 2,
				y - 2,
				width + 4,
				height,
				-0.004F,
				state.backgroundColor,
				LightCoordsUtil.FULL_BRIGHT
			);

			submitNodeCollector.submitText(poseStack,
				x,
				y,
				line,
				state.shadow,
				Font.DisplayMode.NORMAL,
				LightCoordsUtil.FULL_BRIGHT,
				state.color,
				0,
				0
			);
		}

		poseStack.popPose();
	}

	// TODO: make this a common render utility
	//  It's been repeated 6 different times now I think we can make this a common.
	private static void submitFilledRectangle(
		final SubmitNodeCollector collector,
		final PoseStack poseStack,
		final RenderType renderType,
		final float x1,
		final float y1,
		final float width,
		final float height,
		final float zIndex,
		final int color,
		final int light
	) {
		final float x2 = x1 + width;
		final float y2 = y1 + height;
		collector.submitCustomGeometry(
			poseStack,
			renderType,
			(pose, buffer) -> {
				buffer.addVertex(pose, x1, y1, zIndex).setColor(color).setLight(light);
				buffer.addVertex(pose, x1, y2, zIndex).setColor(color).setLight(light);
				buffer.addVertex(pose, x2, y2, zIndex).setColor(color).setLight(light);
				buffer.addVertex(pose, x2, y1, zIndex).setColor(color).setLight(light);
			}
		);
	}

	//	FIXME 26.1
//	@Override
//	public void renderUnbaked(TextBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
//		Entity camera = Minecraft.getInstance().getCameraEntity();
//		if (camera != null && entity.viewDistance >= 0) {
//			double dx = camera.getX() - (entity.getBlockPos().getX() + 0.5);
//			double dy = camera.getY() - (entity.getBlockPos().getY() + 0.5);
//			double dz = camera.getZ() - (entity.getBlockPos().getZ() + 0.5);
//
//			if ((dx * dx + dy * dy + dz * dz) > (entity.viewDistance * entity.viewDistance)) {
//				if (!wasOutOfRange) {
//                    entity.renderDirty = true;
//                    wasOutOfRange = true;
//                }
//			} else {
//				if (wasOutOfRange) {
//					entity.renderDirty = true;
//				}
//
//				wasOutOfRange = false;
//			}
//		}
//
//		if (entity.renderDirty) {
//			entity.renderDirty = false;
//			BakedBlockEntityRenderer.Manager.markForRebuild(entity.getBlockPos());
//		}
//
//		if (entity.getLevel() == null || entity.getLevel().getBlockState(entity.getBlockPos()).isAir()) return;
//	}
//
//	@Override
//	public void renderBaked(TextBlockEntity entity, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
//		Entity camera = Minecraft.getInstance().getCameraEntity();
//		if (camera != null && entity.viewDistance >= 0) {
//			double dx = camera.getX() - (entity.getBlockPos().getX() + 0.5);
//			double dy = camera.getY() - (entity.getBlockPos().getY() + 0.5);
//			double dz = camera.getZ() - (entity.getBlockPos().getZ() + 0.5);
//
//			if ((dx * dx + dy * dy + dz * dz) > (entity.viewDistance * entity.viewDistance)) {
//				if (!wasOutOfRange) {
//                    entity.renderDirty = true;
//                    wasOutOfRange = true;
//                }
//
//				return;
//			} else {
//                if (wasOutOfRange) {
//					entity.renderDirty = true;
//				}
//
//				wasOutOfRange = false;
//            }
//		}
//
//		matrices.pushPose();
//		matrices.translate(0.5D, 0.5D, 0.5D);
//
//		float rotation = -(entity.getBlockState().getValue(BlockStateProperties.ROTATION_16) * 360) / 16.0F;
//		matrices.mulPose(Axis.YP.rotationDegrees(rotation));
//
//		switch (entity.zOffset) {
//			case FRONT -> matrices.translate(0D, 0D, 0.4D);
//			case BACK -> matrices.translate(0D, 0D, -0.4D);
//		}
//
//		float scale = 0.010416667F * entity.scale;
//		matrices.scale(scale, -scale, scale);
//		Font textRenderer = this.context.getFont();
//
//		double maxLength = 0;
//		double minLength = Double.MAX_VALUE;
//		for (int i = 0; i < entity.lines.size(); ++i) {
//			maxLength = Math.max(maxLength, textRenderer.width(entity.lines.get(i)));
//			minLength = Math.min(minLength, textRenderer.width(entity.lines.get(i)));
//		}
//
//		matrices.translate(0, -((entity.lines.size() - 0.25) * 12) / 2D, 0D);
//		for (int i = 0; i < entity.lines.size(); ++i) {
//			Component line = entity.lines.get(i);
//			double width = textRenderer.width(line);
//			if (width == 0) continue;
//
//			double dX = switch (entity.textAlignment) {
//				case LEFT -> -maxLength / 2D;
//				case CENTER -> (maxLength - width) / 2D - maxLength / 2D;
//				case CENTER_LEFT -> -(50D / entity.scale) - (width / 2D);
//				case CENTER_RIGHT -> (50D / entity.scale) - (width / 2D);
//				case RIGHT -> maxLength - width - maxLength / 2D;
//			};
//
//			matrices.pushPose();
//			matrices.translate(dX, 0, 0);
//
//			Font.PreparedTextBuilder drawer = (Font.PreparedTextBuilder) textRenderer.prepareText(line.getVisualOrderText(), 0, i * 12, entity.color, entity.shadow, 0);
//
//			Font.GlyphVisitor glyphDrawer = Font.GlyphVisitor.forMultiBufferSource(
//				vertexConsumers,
//				matrices.last().pose(),
//				DisplayMode.NORMAL,
//				// TODO: use the light param and add a toggle to make it glow (use LightmapTextureManager.MAX_LIGHT_COORDINATE)
//				Lightmap.FULL_BRIGHT
//			);
//
//			// Yep, we're back to that hack again.
//			if (entity.backgroundColor != 0) {
//				BakedGlyph rectangleBakedGlyph = ((FontAccessor) textRenderer)
//					.invokeGetFontStorage(Style.DEFAULT_FONT)
//					.whiteGlyph();
//
//				final BakedGlyph.Effect rect = new BakedGlyph.Effect(
//					-4, i * 12 - 2f,
//					(float) width + 4, (i + 1) * 12 - 2f,
//					-0.01F, entity.backgroundColor);
//
//				glyphDrawer.acceptEffect(rectangleBakedGlyph, rect);
//			}
//
//			drawer.visit(glyphDrawer);
//
//			matrices.popPose();
//		}
//
//		matrices.popPose();
//	}

}
