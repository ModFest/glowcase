package dev.hephaestus.glowcase.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BlockEntityRenderUtil {
	private static final Vector3f[] placeholderVertices = new Vector3f[]{
		new Vector3f(-0.5F, -0.5F, 0.0F),
		new Vector3f(0.5F, -0.5F, 0.0F),
		new Vector3f(0.5F, 0.5F, 0.0F),
		new Vector3f(-0.5F, 0.5F, 0.0F)
	};

	public static void renderPlaceholder(BlockEntityRenderState state, Identifier texture, float scale, Quaternionf rotation, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, float zOffset) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(rotation);
		poseStack.translate(0, 0, zOffset);
		poseStack.scale(scale, scale, scale);
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutCull(texture), (pose, buffer) -> {
			renderPlaceholderBackFace(pose, buffer, state.blockPos);
			renderPlaceholderFace(pose, buffer, state.blockPos);
		});
		poseStack.popPose();
	}

	public static void renderBillboardPlaceholder(BlockEntityRenderState entity, Identifier texture, float scale, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		renderPlaceholder(entity, texture, scale, camera.orientation, poseStack, submitNodeCollector, 0);
	}

	public static void renderTrackingPlaceholder(BlockEntityRenderState state, Identifier texture, float scale, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Vec3 camera) {
		renderPlaceholder(
			state,
			texture,
			scale,
			getTrackingQuaternion(camera, state.blockPos),
			poseStack,
			submitNodeCollector,
			0
		);
	}

	public static void renderPlaceholderWithBlockRotation(BlockEntityRenderState entity, int rotation16, Identifier texture,
														  float scale, PoseStack matrices, SubmitNodeCollector vertexConsumers, float zOffset) {
		renderPlaceholder(entity, texture, scale,
			Axis.YP.rotationDegrees(-(rotation16 * 360) / 16.0F),
			matrices, vertexConsumers, zOffset);
	}

	public static void renderPlaceholderWithBlockRotation(BlockEntityRenderState entity, int rotation16, Identifier texture,
														  float scale, PoseStack matrices, SubmitNodeCollector vertexConsumers) {
		renderPlaceholderWithBlockRotation(entity, rotation16, texture, scale, matrices, vertexConsumers, 0F);
	}

	public static void renderCenteredPlaceholder(BlockEntityRenderState entity, Identifier texture,
												 float scale, Quaternionf rotation, PoseStack matrices, SubmitNodeCollector submitNodeCollector) {
		renderPlaceholder(entity, texture, scale, rotation, matrices, submitNodeCollector, 0F);
	}

	public static void renderFacingPlaceholder(BlockEntityRenderState entity, Direction facing, Identifier texture,
											   float scale, PoseStack matrices, SubmitNodeCollector submitNodeCollector) {
		renderPlaceholder(
			entity,
			texture,
			scale,
			facing.getRotation().rotateX(-Mth.HALF_PI),
			matrices,
			submitNodeCollector,
			-0.4F
		);
	}

	public static boolean isBeingLookedAt(final BlockPos pos) {
		return Minecraft.getInstance().hitResult instanceof BlockHitResult result && result.getBlockPos().equals(pos);
	}

	public static boolean shouldRenderPlaceholder(BlockPos pos) {
		return shouldRenderPlaceholder(pos, true);
	}

	public static boolean shouldRenderPlaceholder(BlockPos pos, boolean disappearWhenFaced) {
		return Minecraft.getInstance().player != null && Minecraft.getInstance().player.isHolding(stack -> stack.is(
			Glowcase.ITEM_TAG)) && (!disappearWhenFaced || !isBeingLookedAt(pos));
	}

	public static Vec2 getTracking(Vec3 camera, BlockPos pos) {
		double d = pos.getX() - camera.x + 0.5;
		double e = pos.getY() - camera.y + 0.5;
		double f = pos.getZ() - camera.z + 0.5;
		double g = Mth.sqrt((float) (d * d + f * f));

		float pitch = (float) ((-Mth.atan2(e, g)));
		float yaw = (float) (-Mth.atan2(f, d) + Math.PI / 2);

		return new Vec2(pitch, yaw);
	}

	public static Quaternionf getTrackingQuaternion(Vec3 camera, BlockPos pos) {
		final Vec2 vec = getTracking(camera, pos);

		return Quaternionsf.rotateYXZ(Mth.PI + vec.y, -vec.x, 0.f);
	}

	private static void renderPlaceholderFace(PoseStack.Pose entry, VertexConsumer vertexConsumer, BlockPos pos) {
		int color = Minecraft.getInstance().hitResult instanceof BlockHitResult bhr && bhr.getBlockPos().equals(pos) ? 0x808080 : 0xFFFFFF;
		placeholderVertex(entry, vertexConsumer, placeholderVertices[0], 0, 1, color);
		placeholderVertex(entry, vertexConsumer, placeholderVertices[1], 1, 1, color);
		placeholderVertex(entry, vertexConsumer, placeholderVertices[2], 1, 0, color);
		placeholderVertex(entry, vertexConsumer, placeholderVertices[3], 0, 0, color);
	}

	private static void renderPlaceholderBackFace(PoseStack.Pose entry, VertexConsumer vertexConsumer, BlockPos pos) {
		int color = Minecraft.getInstance().hitResult instanceof BlockHitResult bhr && bhr.getBlockPos().equals(pos) ? 0x404040 : 0x808080;
		placeholderVertex(entry, vertexConsumer, placeholderVertices[3], 0, 0, color);
		placeholderVertex(entry, vertexConsumer, placeholderVertices[2], 1, 0, color);
		placeholderVertex(entry, vertexConsumer, placeholderVertices[1], 1, 1, color);
		placeholderVertex(entry, vertexConsumer, placeholderVertices[0], 0, 1, color);
	}

	private static void placeholderVertex(
		PoseStack.Pose matrix, VertexConsumer vertexConsumer, Vector3f vertex, float u, float v, int color) {
		vertexConsumer.addVertex(matrix, vertex.x(), vertex.y(), vertex.z())
			.setColor(color)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(0xFF)
			.setNormal(0, 1, 0);
	}
}
