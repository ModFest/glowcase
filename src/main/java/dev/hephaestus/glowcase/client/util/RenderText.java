package dev.hephaestus.glowcase.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;

/**
 * @author Ampflower
 */
public final class RenderText {
	public static void billboardCenteredText(
		final SubmitNodeCollector collector,
		final PoseStack poseStack,
		final CameraRenderState camera,
		final Font font,
		final float x,
		final float y,
		final float z,
		final Component text,
		final int color
	) {
		billboardCenteredText(collector, poseStack, camera, font, x, y, z, text.getVisualOrderText(), color);
	}

	public static void billboardCenteredText(
		final SubmitNodeCollector collector,
		final PoseStack poseStack,
		final CameraRenderState camera,
		final Font font,
		final float x,
		final float y,
		final float z,
		final FormattedCharSequence text,
		final int color
	) {
		poseStack.pushPose();
		poseStack.translate(0.5D, 0.5D, 0.5D);
		poseStack.mulPose(Quaternionsf.rotateDegreesYXZ(-camera.yRot, camera.xRot, 180));
		poseStack.translate(0, 0, -z);
		final float scale = 0.5F * 0.025F;
		// -Z fixes shadow being rendered in front of actual text
		poseStack.scale(scale, scale, -scale);

		RenderText.brightCenterText(collector, poseStack, font, x, y, text, color);

		poseStack.popPose();
	}

	public static void brightCenterText(
		final SubmitNodeCollector collector,
		final PoseStack poseStack,
		final Font font,
		final float x,
		final float y,
		final Component component,
		final int color
	) {
		brightCenterText(collector, poseStack, font, x, y, component.getVisualOrderText(), color);
	}

	public static void brightCenterText(
		final SubmitNodeCollector collector,
		final PoseStack poseStack,
		final Font font,
		final float x,
		final float y,
		final FormattedCharSequence component,
		final int color
	) {
		brightText(collector, poseStack, Math.fma(font.width(component), -.5F, x), y, component, color);
	}

	public static void brightText(
		final SubmitNodeCollector collector,
		final PoseStack poseStack,
		final float x,
		final float y,
		final Component component,
		final int color
	) {
		brightText(collector, poseStack, x, y, component.getVisualOrderText(), color);
	}

	public static void brightText(
		final SubmitNodeCollector collector,
		final PoseStack poseStack,
		final float x,
		final float y,
		final FormattedCharSequence component,
		final int color
	) {
		collector.submitText(
			poseStack,
			x,
			y,
			component,
			true,
			Font.DisplayMode.NORMAL,
			LightCoordsUtil.FULL_BRIGHT,
			color,
			0,
			0
		);
	}
}
