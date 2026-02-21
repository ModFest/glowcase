package dev.hephaestus.glowcase.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.gui.screen.ingame.NoteEditScreen;
import dev.hephaestus.glowcase.client.util.NoteTextColorResource;
import dev.hephaestus.glowcase.item.component.NoteComponent;
import org.joml.Matrix4f;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.ItemStack;

public class NoteItemHandRenderer extends ItemHandRenderer {
	private static final ResourceLocation NOTE_TEXTURE = Glowcase.id("textures/gui/note.png");

	private static final int BG_SIZE = 256;

	private static final int BG_WIDTH = 244 ;
	private static final int BG_HEIGHT = 117;

	private static final int TXT_X_PADDING = 15 * 2;

	@Override
	public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, ItemStack stack) {
		matrices.pushPose();
		matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
		matrices.mulPose(Axis.ZP.rotationDegrees(180.0F));
		matrices.scale(0.38F, 0.38F, 0.38F);
		matrices.translate(-0.5F, -0.5F, 0.0F);
		matrices.scale(0.0078125F, 0.0078125F, 0.0078125F);

		// Render background

		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderType.text(NOTE_TEXTURE));
		Matrix4f matrix4f = matrices.last().pose();

		{
			float max_x = 1.0F / BG_SIZE * BG_WIDTH;
			float max_y = 1.0F / BG_SIZE * BG_HEIGHT;

			float scaler = 20F;
			float begin = -7F - scaler;
			float end = 135F + scaler;

			float x_off = 10F;

			float height = (Math.abs(begin) + end) / BG_SIZE * BG_HEIGHT;
			float y_off = -20F;
			float y1 = begin + ((Math.abs(begin) + end) - height) + y_off;
			end += y_off;

			vertexConsumer.addVertex(matrix4f, x_off + begin, end, 0.0F).setColor(CommonColors.WHITE).setUv(0.0F, max_y).setLight(light);
			vertexConsumer.addVertex(matrix4f, x_off + end, end, 0.0F).setColor(CommonColors.WHITE).setUv(max_x, max_y).setLight(light);
			vertexConsumer.addVertex(matrix4f, x_off + end, y1, 0.0F).setColor(CommonColors.WHITE).setUv(max_x, 0.0F).setLight(light);
			vertexConsumer.addVertex(matrix4f, x_off + begin, y1, 0.0F).setColor(CommonColors.WHITE).setUv(0.0F, 0.0F).setLight(light);
		}

		// Render Text

		NoteComponent noteComponent = stack.get(Glowcase.NOTE_COMPONENT.get());
		if (noteComponent == null) {
			matrices.popPose();
			return;
		}

		float off_x = -7F;
		float off_y = 63F;

		Font textRenderer = Minecraft.getInstance().font;
		matrices.translate(off_x, off_y, -.01f);
		matrices.scale(0.67f, 0.67f, 1f);

		float width = BG_WIDTH - TXT_X_PADDING;

		List<Component> lines = noteComponent.lines();
		for (int i=0; i<lines.size(); i++) {
			FormattedText text = lines.get(i);
			if (NoteEditScreen.outOfBounds(textRenderer, text))
				text = NoteEditScreen.ensureBounds(textRenderer, text);

			float x = switch (noteComponent.alignment()) {
				case LEFT -> 0;
				case CENTER -> width/2f - textRenderer.width(text)/2f - 1; // We don't ask why the -1 is there
				case RIGHT ->  BG_WIDTH - TXT_X_PADDING - textRenderer.width(text);
			};

			textRenderer.drawInBatch(Language.getInstance().getVisualOrder(text), x, textRenderer.lineHeight * i, NoteTextColorResource.TXT_COLOR, false, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, light);
		}

		matrices.popPose();
	}

	@Override
	public boolean visible(ItemStack stack) {
		return (stack.has(Glowcase.NOTE_COMPONENT.get()));
	}
}
