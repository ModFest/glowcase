package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
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
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/text_block.png");
	private boolean wasOutOfRange = false;

	private final Font font;

	public TextBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
		this.font = ctx.font();
	}

	public static class TextRenderState extends BlockEntityRenderState {
		public boolean shouldRenderPlaceholder;
		public int rotation16;
		public List<FormattedCharSequence> lines = List.of();
		public TextBlockEntity.TextAlignment textAlignment;
		public TextBlockEntity.HorizontalAlignment horizontalAlignment;
		public TextBlockEntity.ZOffset zOffset;
		public boolean shadow;
		public float scale = 1;
		public int color;
		public int backgroundColor;
	}

	@Override
	public TextRenderState createRenderState() {
		return new TextRenderState();
	}

	@Override
	public TextRenderState createBakedRenderState() {
		return new TextRenderState();
	}

	@Override
	public void extractRenderState(TextBlockEntity blockEntity, TextRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BakedBlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.shouldRenderPlaceholder = blockEntity.lines.stream().allMatch(t -> t.getString().isBlank()) || BlockEntityRenderUtil.shouldRenderPlaceholder(blockEntity.getBlockPos());

		state.rotation16 = blockEntity.getBlockState().getValue(BlockStateProperties.ROTATION_16);

		state.lines = blockEntity.lines.stream().map(Component::getVisualOrderText).toList();
		state.textAlignment = blockEntity.textAlignment;
		state.horizontalAlignment = blockEntity.horizontalAlignment;
		state.zOffset = blockEntity.zOffset;
		state.shadow = blockEntity.shadow;
		state.scale = blockEntity.scale;
		state.color = blockEntity.color;
		state.backgroundColor = blockEntity.backgroundColor;
	}

	@Override
	public void extractBakingRenderState(TextBlockEntity blockEntity, TextRenderState state) {
		BakedBlockEntityRenderer.super.extractBakingRenderState(blockEntity, state);
		// TODO: Change for the port to 26.1, if needed
		state.shouldRenderPlaceholder = blockEntity.lines.stream().allMatch(t -> t.getString().isBlank()) || BlockEntityRenderUtil.shouldRenderPlaceholder(blockEntity.getBlockPos());
		state.zOffset = blockEntity.zOffset;
		state.rotation16 = blockEntity.getBlockState().getValue(BlockStateProperties.ROTATION_16);

		state.lines = blockEntity.lines.stream().map(Component::getVisualOrderText).toList();
		state.scale = blockEntity.scale;
	}

	@Override
	public boolean shouldBake(TextBlockEntity entity) {
//		return !entity.lines.isEmpty();
		return false;
	}

	@Override
	public void submitForRendering(TextRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (state.shouldRenderPlaceholder) {
			BlockEntityRenderUtil.renderPlaceholderWithBlockRotation(state, state.rotation16, ITEM_TEXTURE, 1.0F, poseStack, submitNodeCollector, state.zOffset == TextBlockEntity.ZOffset.CENTER ? 0.01F : state.zOffset == TextBlockEntity.ZOffset.FRONT ? 0.4F : -0.4F);
		}

		// TODO: move this to a baked rendering method
		//   currently it errors "Rendersystem called from wrong thread" on `this.font.width(text)`
		int maxWidth = 0;
		for (var line : state.lines) {
			maxWidth = Math.max(maxWidth, this.font.width(line));
		}

		poseStack.pushPose();
		poseStack.translate(0.5D, 0.5D, 0.5D);
		// 2D rendering of the font has Y axis going down, not up
		poseStack.scale(1, -1, 1);

		switch (state.zOffset) {
			case FRONT -> poseStack.translate(0D, 0D, 0.4D);
			case BACK -> poseStack.translate(0D, 0D, -0.4D);
		}

		float rotation = -(state.rotation16 * 360) / 16.0F;
		poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

		poseStack.scale(0.1F * state.scale, 0.1F * state.scale, 0.1F * state.scale);
		poseStack.translate(0, -(state.lines.size() * this.font.lineHeight) / 2D, 0D);

		switch (state.horizontalAlignment) {
			case LEFT -> poseStack.translate(-maxWidth / 2F, 0, 0);
			case RIGHT -> poseStack.translate(maxWidth / 2F, 0, 0);
		}

		for (int i = 0; i < state.lines.size(); ++i) {
			var line = state.lines.get(i);

			int width = this.font.width(line);
			if (width == 0) {
				continue;
			}

			float x = switch (state.textAlignment) {
				case LEFT -> -maxWidth / 2F;
				case CENTER -> (maxWidth - width) / 2F - maxWidth / 2F;
				case CENTER_LEFT -> -(50F / state.scale) - (width / 2F);
				case CENTER_RIGHT -> (50F / state.scale) - (width / 2F);
				case RIGHT -> maxWidth - width - maxWidth / 2F;
			};

			submitNodeCollector.submitText(poseStack,
				x,
				i * this.font.lineHeight,
				line, state.shadow,
				Font.DisplayMode.NORMAL,
				LightCoordsUtil.FULL_BRIGHT,
				state.color,
				state.backgroundColor,
				0);
		}

		poseStack.popPose();
	}

	@Override
	public void submitForBaking(TextRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
		// TODO: Port to 26.1
//		poseStack.pushPose();
//		poseStack.mulPose(Axis.YP.rotationDegrees(-(state.rotation16 * 360) / 16.0F));
//		float a = 1 / 9f;
//		poseStack.translate(-.5, 1.5, -.5);
//		poseStack.scale(a, -a, a);
//		submitNodeCollector.submitText(poseStack, 0, 0, Component.literal("waff :3").getVisualOrderText(), true, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, 0xFFFFFFFF, 0, 0);
//		poseStack.popPose();
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
