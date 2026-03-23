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

public class BlockEntityRenderUtil {
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
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutCull(texture), new SubmitNodeCollector.CustomGeometryRenderer() {
			@Override
			public void render(PoseStack.Pose pose, VertexConsumer buffer) {
				renderPlaceholderBackFace(pose, buffer, state.blockPos);
				renderPlaceholderFace(pose, buffer, state.blockPos);
			}
		});
		poseStack.popPose();
	}

	public static void renderBillboardPlaceholder(BlockEntityRenderState entity, Identifier texture, float scale, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - camera.yRot));
		poseStack.mulPose(Axis.XP.rotationDegrees(-camera.xRot));
		poseStack.scale(scale, scale, scale);
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture), new SubmitNodeCollector.CustomGeometryRenderer() {
			@Override
			public void render(PoseStack.Pose pose, VertexConsumer buffer) {
				renderPlaceholderFace(pose, buffer, entity.blockPos);
			}
		});
		poseStack.popPose();
	}

	public static void renderTrackingPlaceholder(BlockEntityRenderState state, Identifier texture, float scale, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Vec3 camera) {
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		Vec2 tracking = getTracking(camera, state.blockPos);
		float pitch = tracking.x;
		float yaw = tracking.y;
		poseStack.mulPose(Axis.YP.rotation((float) (Math.PI + yaw)));
		poseStack.mulPose(Axis.XP.rotation(-pitch));
		poseStack.scale(scale, scale, scale);
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture), new SubmitNodeCollector.CustomGeometryRenderer() {
			@Override
			public void render(PoseStack.Pose pose, VertexConsumer buffer) {
				renderPlaceholderFace(pose, buffer, state.blockPos);
			}
		});
		poseStack.popPose();
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
		renderPlaceholder(entity, texture, scale, facing.getRotation().mul(Axis.XP.rotationDegrees(-90.0F)), matrices, submitNodeCollector, -0.4F);
	}

	public static boolean shouldRenderPlaceholder(BlockPos pos) {
		return shouldRenderPlaceholder(pos, true);
	}

	public static boolean shouldRenderPlaceholder(BlockPos pos, boolean disappearWhenFaced) {
		return Minecraft.getInstance().player != null && Minecraft.getInstance().player.isHolding(stack -> stack.is(Glowcase.ITEM_TAG)) && (!disappearWhenFaced || !(Minecraft.getInstance().hitResult instanceof BlockHitResult bhr && bhr.getBlockPos().equals(pos)));
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
