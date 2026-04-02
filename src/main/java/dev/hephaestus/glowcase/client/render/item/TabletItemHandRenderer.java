package dev.hephaestus.glowcase.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.GlowcaseClient;
import dev.hephaestus.glowcase.client.ScreenImageCache;
import dev.hephaestus.glowcase.client.render.block.entity.ScreenBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class TabletItemHandRenderer extends ItemHandRenderer {
	private static final Identifier TABLET_TEXTURE = Glowcase.id("textures/gui/tablet_hand.png");

	@Override
	public void render(PoseStack matrices, SubmitNodeCollector collector, int light, ItemStack stack) {
		matrices.pushPose();
		//RenderSystem.enableBlend();

		// Render background

		matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
		matrices.mulPose(Axis.ZP.rotationDegrees(180.0F));
		matrices.scale(0.38F, 0.38F, 0.38F);
		matrices.translate(-0.5F, -0.5F, 0.0F);
		matrices.scale(0.0078125F, 0.0078125F, 0.0078125F);

		collector.submitCustomGeometry(matrices, RenderTypes.text(TABLET_TEXTURE), (matrix4f, vertexConsumer) -> {
			vertexConsumer.addVertex(matrix4f, -7.0F, 135.0F, 0.0F).setColor(CommonColors.WHITE).setUv(0.0F, 1.0F).setLight(light);
			vertexConsumer.addVertex(matrix4f, 135.0F, 135.0F, 0.0F).setColor(CommonColors.WHITE).setUv(1.0F, 1.0F).setLight(light);
			vertexConsumer.addVertex(matrix4f, 135.0F, -7.0F, 0.0F).setColor(CommonColors.WHITE).setUv(1.0F, 0.0F).setLight(light);
			vertexConsumer.addVertex(matrix4f, -7.0F, -7.0F, 0.0F).setColor(CommonColors.WHITE).setUv(0.0F, 0.0F).setLight(light);
		});

		if (!stack.has(Glowcase.SLIDESHOW_COMPONENT.get()) || !stack.has(Glowcase.CURRENT_SLIDE_COMPONENT.get())) {
			matrices.popPose();
			return;
		}

		List<Pair<String, String>> slideshow = stack.get(Glowcase.SLIDESHOW_COMPONENT.get());
		Integer index = stack.getOrDefault(Glowcase.CURRENT_SLIDE_COMPONENT.get(), 0);

		if (slideshow == null || index >= slideshow.size()) {
			matrices.popPose();
			return;
		}

		// Render current slide text

		{
			Font textRenderer = Minecraft.getInstance().font;
			MutableComponent literal = Component.translatable("gui.glowcase.progress", index + 1, slideshow.size());

			float font_scale = 1f;
			float font_width = textRenderer.width(literal);
			float font_max_width = 142f/64f*14f;
			if (font_width >= font_max_width) {
				font_scale = font_max_width / font_width;
			}

			float off_x = 142f/64f*29.2f - (font_width * font_scale)/2f;
			float off_y = 142f/64f*8.2f;

			matrices.translate(off_x, off_y, -.01f);
			matrices.scale(font_scale, font_scale, 1f);
			collector.submitText(matrices, 0, 0, Language.getInstance().getVisualOrder(literal), false, Font.DisplayMode.NORMAL, light, 0xFFFFFFFF, 0, 0);
			matrices.translate(-off_x, -off_y, .01f);
			matrices.scale(1f/font_scale, 1f/font_scale, 1f);
		}

		// Render current picture

		String url = slideshow.get(index).getFirst();

		ScreenImageCache.ScreenTexture image = GlowcaseClient.screenImageCache.getImage(url, null);
		Identifier texture = image.getTexture().getSecond();
		if (texture == null) {
			matrices.popPose();
			return;
		}

		collector.submitCustomGeometry(matrices, RenderTypes.text(texture), (matrix4f, vertexConsumer) -> {
			float pixel = 142f / 64f;

			float x1 = pixel * -21;
			float y1 = pixel * -15;
			float x2 = pixel * 21;
			float y2 = pixel * 13;

			ScreenBlockEntityRenderer.Plane scale = ScreenBlockEntityRenderer.getScale(x2 - x1, y2 - y1, image.getWidth(), image.getHeight());

			x1 = scale.x1();
			x2 = scale.x2();
			y1 = scale.y1();
			y2 = scale.y2();

			vertexConsumer.addVertex(matrix4f, 64 + x1, 64 + y1, -0.01F).setColor(CommonColors.WHITE).setUv(0f, 0f).setLight(light);
			vertexConsumer.addVertex(matrix4f, 64 + x1, 64 + y2, -0.01F).setColor(CommonColors.WHITE).setUv(0f, 1f).setLight(light);
			vertexConsumer.addVertex(matrix4f, 64 + x2, 64 + y2, -0.01F).setColor(CommonColors.WHITE).setUv(1f, 1f).setLight(light);
			vertexConsumer.addVertex(matrix4f, 64 + x2, 64 + y1, -0.01F).setColor(CommonColors.WHITE).setUv(1f, 0f).setLight(light);
		});

		matrices.popPose();
	}
}
