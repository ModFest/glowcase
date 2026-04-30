package dev.hephaestus.glowcase.client.render.bakedbe;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.client.render.bakedbe.section.RenderSectionPos;
import dev.hephaestus.glowcase.client.render.block.entity.BakedBlockEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class BakedRendererUtil {
	private BakedRendererUtil() {}

	public static <S extends BlockEntityRenderState> void submitForBaking(BakedBlockEntityRenderer<?, ?, S> renderer, BlockPos pos, S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
		poseStack.pushPose();
		poseStack.translate(Vec3.atLowerCornerOf(RenderSectionPos.maskToSection(pos)));
		renderer.submitForBaking(state, poseStack, submitNodeCollector);
		poseStack.popPose();

		checkPoseStack(poseStack);
	}

	public static void checkPoseStack(final PoseStack poseStack) {
		if (!poseStack.isEmpty()) {
			throw new IllegalStateException("Pose stack not empty");
		}
	}
}
