package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import dev.hephaestus.glowcase.client.util.Quaternionsf;
import dev.hephaestus.glowcase.util.Anchor;
import dev.hephaestus.glowcase.util.TextJustify;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public class TextBlockEntityRenderer implements BakedBlockEntityRenderer<TextBlockEntity, TextBlockEntityRenderer.TextRenderState> {
	public static final Identifier ITEM_TEXTURE = Glowcase.id("textures/item/text_block.png");
	private final Font font;

	public TextBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
		this.font = ctx.font();
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	public static class TextRenderState extends BlockEntityRenderState {
		public boolean shouldBake;
		public boolean renderPlaceholder;
		public int rotation16;
		public int linesHash;
		public List<FormattedCharSequence> lines;
		public TextJustify textAlignment;
		public Anchor anchor;
		public boolean shadow;
		public float scale = 1;
		public int color;
		public int backgroundColor;
		public Vec3 offset;
		public Vec3 rotation; // Yaw, pitch, roll

		public float zOffset() {
			if (offset.z() - (offset.z() % 0.01D) == 0.D) {
				return 0.01F;
			}
			return Mth.clamp((float) offset.z(), -0.4F, 0.4F);
		}
	}

	// Unbaked rendering

	@Override
	public TextRenderState createRenderState() {
		return new TextRenderState();
	}

	@Override
	public boolean shouldRender(TextBlockEntity blockEntity, Vec3 cameraPosition) {
		return BakedBlockEntityRenderer.super.shouldRender(blockEntity, cameraPosition);
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

		state.shouldBake = !isEmpty(blockEntity);
		state.renderPlaceholder = shouldRenderPlaceholder(blockEntity);
		state.linesHash = blockEntity.lines.hashCode();
		state.lines = blockEntity.lines.stream().map(Component::getVisualOrderText).toList();
		state.textAlignment = blockEntity.textAlignment;
		state.anchor = blockEntity.anchor;
		state.shadow = blockEntity.shadow;
		state.scale = blockEntity.scale;
		state.color = blockEntity.color;
		state.backgroundColor = blockEntity.backgroundColor;
		state.offset = blockEntity.offset;
		state.rotation = blockEntity.rotation;
	}

	@Override
	public void submit(TextRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (!state.renderPlaceholder) return;
		BlockEntityRenderUtil.renderPlaceholderWithBlockRotation(
			state,
			state.rotation16,
			ITEM_TEXTURE,
			1.0F,
			poseStack,
			submitNodeCollector,
			state.zOffset()
		);
	}

	// Baked rendering

	@Override
	public Object renderStateIdentity(TextRenderState state) {
		var identity = new ArrayList<>();
		identity.add(state.rotation16);
		identity.add(state.linesHash);
		identity.add(state.textAlignment);
		identity.add(state.anchor);
		identity.add(state.shadow);
		identity.add(state.scale);
		identity.add(state.color);
		identity.add(state.backgroundColor);
		identity.add(state.offset);
		identity.add(state.rotation);
		return identity;
	}

	@Override
	public boolean shouldBake(TextRenderState state) {
		return state.shouldBake;
	}

	@Override
	public int getViewDistance() {
		return Integer.MAX_VALUE;
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
		poseStack.rotate(Axis.YP.rotationDegrees(rotation));

		// Translate extra offsets (negative y to match Minecraft's coordinates)
		poseStack.translate(state.offset.x(), -state.offset.y(), state.offset.z());

		// Apply extra rotations
		poseStack.rotate(Quaternionsf.rotateDegreesXYZ(
			(float) state.rotation.x(),
			(float) state.rotation.y(),
			(float) state.rotation.z()
		));

		// Scale for parity with older versions of Glowcase.
		// Unless Mojang ever changes the rendering scale, this shall remain.
		final float scale = 0.010416667F * state.scale;
		poseStack.scale(scale, scale, scale);

		poseStack.translate(
			state.anchor.getX() * maxWidth / -2F,
			// 2.d required for alignment against the block border.
			state.anchor.getY() == 1 ? 2.d :
				state.anchor.getY() == -1 ?
					Math.fma(state.lines.size(), -height, 2.d) :
					// Adjusts the text positioning to parity. For some reason, the text moved up;
					// 24/26.d is roughly the required movement down for 100% parity.
					Math.fma(state.lines.size(), height / -2.d, 24.d / 16.d),
			0D
		);

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

 			// I've replaced it with submitTextBackground instead :3 - Luna
			float bgX = x - 2;
			float bgY = y - 2;
			submitNodeCollector.submitTextBackground(
				poseStack,
				bgX,
				bgY,
				bgX + width + 4,
				bgY + height,
				state.backgroundColor,
				Font.DisplayMode.NORMAL,
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
}
