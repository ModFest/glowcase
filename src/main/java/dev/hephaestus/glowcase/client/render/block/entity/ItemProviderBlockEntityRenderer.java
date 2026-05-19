package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.ItemProviderBlock;
import dev.hephaestus.glowcase.block.entity.ItemProviderBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import dev.hephaestus.glowcase.client.util.Quaternionsf;
import dev.hephaestus.glowcase.client.util.RenderText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public record ItemProviderBlockEntityRenderer(
	BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ItemProviderBlockEntity, ItemProviderBlockEntityRenderer.ItemProviderRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/item_provider_block.png");

	private static final Map<Direction, Transformation> transformCache = Util.makeEnumMap(
		Direction.class,
		ItemProviderBlockEntityRenderer::build
	);
	private static final float textScale = 0.025F;
	private static final Matrix4f textTransformCache = new Matrix4f()
		.rotateZ(Mth.PI)
		.translate(0, -.6f, -.3f)
		.scale(textScale, textScale, -textScale); // note: -z is shadow fix

	private static Transformation build(Direction direction) {
		if (direction == Direction.UP || direction == Direction.DOWN) {
			return null;
		}

		final Matrix4f matrix = new Matrix4f();
		matrix.translate(0.5F, 0.5F, 0.5F);
		matrix.rotate(direction.getOpposite().getRotation().rotateX(-Mth.HALF_PI));
		matrix.translate(0.F, 0.F, 0.375F);
		matrix.scale(0.5F);

		return new Transformation(matrix);
	}

	public static class ItemProviderRenderState extends BlockEntityRenderState {
		public ItemStackRenderState itemRenderState = new ItemStackRenderState();
		public Direction facing = Direction.UP;
		public boolean shouldRenderPlaceholder;
		public boolean isBlockItem;
		public boolean isInvisible;
		public Component name = Component.empty();
		public int textColor;
		public boolean canGive;
		public Component countText;
	}

	@Override
	public ItemProviderRenderState createRenderState() {
		return new ItemProviderRenderState();
	}

	@Override
	public void extractRenderState(ItemProviderBlockEntity blockEntity, ItemProviderRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

		var stack = blockEntity.getStack();
		this.context.itemModelResolver().updateForTopItem(state.itemRenderState, stack, ItemDisplayContext.FIXED, blockEntity.getLevel(), null, (int) state.blockPos.asLong());
		state.facing = blockEntity.getBlockState().getValue(ItemProviderBlock.FACING);
		state.isBlockItem = stack.getItem() instanceof BlockItem;
		state.shouldRenderPlaceholder = !blockEntity.hasItem() || BlockEntityRenderUtil.shouldRenderPlaceholder(blockEntity.getBlockPos());
		state.isInvisible = blockEntity.isInvisible();
		state.name = stack.isEmpty() ? Component.translatable("gui.glowcase.none") : (Component.literal("")).append(stack.getHoverName()).withStyle(stack.getRarity().color());
		state.textColor = ARGB.opaque(state.name.getStyle().getColor() == null ? 0xFFFFFF : state.name.getStyle().getColor().getValue());
		state.canGive = blockEntity.canGiveTo(Minecraft.getInstance().player);
		if (state.canGive) {
			state.countText = Component.literal("%dx".formatted(blockEntity.getStack().getCount()));
		} else {
			state.countText = switch (blockEntity.getGivesItem()) {
				case ONE -> Component.literal("[MAX]").withStyle(ChatFormatting.YELLOW);
				case TIMED -> {
					final long cooldownMS = blockEntity.getCooldownTicks(Minecraft.getInstance().player) * 50;
					if (cooldownMS <= 0) {
						yield Component.literal("[??:??]").withStyle(ChatFormatting.RED);
					}
					yield Component.literal("[%s]".formatted(DurationFormatUtils.formatDuration(cooldownMS, cooldownMS > 3600000 ? "HH:mm:ss" : "mm:ss"))).withStyle(ChatFormatting.YELLOW);
				}
				case ALWAYS ->
					Component.literal("[I'm sorry, I'm actually the item unprovider.]").withStyle(ChatFormatting.RED);
			};
		}
	}

	@Override
	public void submit(ItemProviderRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (state.shouldRenderPlaceholder) {
			if (state.facing.getAxis() != Direction.Axis.Y) {
				BlockEntityRenderUtil.renderFacingPlaceholder(
					state,
					state.facing,
					ITEM_TEXTURE,
					1.0F,
					poseStack,
					submitNodeCollector
				);
			} else if (state.isBlockItem) {
				BlockEntityRenderUtil.renderTrackingPlaceholder(
					state,
					ITEM_TEXTURE,
					1.0F,
					poseStack,
					submitNodeCollector,
					camera.pos
				);
			} else {
				BlockEntityRenderUtil.renderBillboardPlaceholder(
					state,
					ITEM_TEXTURE,
					1.0F,
					poseStack,
					submitNodeCollector,
					camera
				);
			}
		}

		final boolean displayText = BlockEntityRenderUtil.isBeingLookedAt(state.blockPos);

		if (state.isInvisible && !displayText) {
			return;
		}

		poseStack.pushPose();

		final Transformation transform = transformCache.get(state.facing);
		if (transform == null) {
			poseStack.translate(0.5D, 0.5D, 0.5D);
			poseStack.scale(0.5F, 0.5F, 0.5F);
			if (state.isBlockItem) {
				poseStack.mulPose(Quaternionsf.rotateYX(BlockEntityRenderUtil.getTracking(camera.pos, state.blockPos)));
			} else {
				poseStack.mulPose(Quaternionsf.rotateDegreesYXZ(-camera.yRot, camera.xRot, 0));
			}
		} else {
			poseStack.mulPose(transform);
		}

		if (!state.isInvisible) {
			state.itemRenderState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		}

		if (displayText) {
			// Freely just clobber the matrix, we're not reusing it.
			poseStack.mulPose(textTransformCache);

			Component name = state.name;
			int color = ARGB.opaque(name.getStyle().getColor() == null ? 0xFFFFFF : name.getStyle().getColor().getValue());

			RenderText.brightCenterText(submitNodeCollector, poseStack, context.font(), 0, -4, name, color);

			if (!state.itemRenderState.isEmpty()) {
				RenderText.brightText(
					submitNodeCollector,
					poseStack,
					-context.font().width(state.countText) + 16,
					state.canGive ? 32 : 24,
					state.countText,
					color
				);
			}
		}

		poseStack.popPose();
	}
}
