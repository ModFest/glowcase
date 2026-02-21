package dev.hephaestus.glowcase.client.gui.widget.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class IconButtonWidget extends Button {
	public ResourceLocation icon;
	@Nullable
	public ResourceLocation hoverIcon;
	public int iconWidth;
	public int iconHeight;
	public int z;

	public static IconButtonWidget.Builder builder(ResourceLocation icon, Button.OnPress onPress) {
		return new IconButtonWidget.Builder(icon, onPress);
	}

	public IconButtonWidget(int x, int y, int width, int height, int iconWidth, int iconHeight, ResourceLocation icon, @Nullable ResourceLocation hoverIcon, OnPress onPress) {
		super(x, y, width, height, Component.nullToEmpty(""), onPress, Button.DEFAULT_NARRATION);
		this.icon = icon;
		this.hoverIcon = hoverIcon;
		this.iconWidth = iconWidth;
		this.iconHeight = iconHeight;
	}

	@Override
	protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
		ResourceLocation drawnIcon = this.icon;
		if(this.hoverIcon != null && this.isMouseOver(mouseX, mouseY)) {
			drawnIcon = this.hoverIcon;
		}
		context.blitSprite(RenderPipelines.GUI_TEXTURED, drawnIcon, this.getX(), this.getY(), this.iconWidth, this.iconHeight);
	}

	public void setPosition(int x, int y, int z, int size, int iconSize) {
		this.setX(x);
		this.setY(y);
		this.setSize(size, size);
		this.iconWidth = iconSize;
		this.iconHeight = iconSize;
		this.z = z;
	}

	@Environment(EnvType.CLIENT)
	public static class Builder {
		private final ResourceLocation icon;
		@Nullable
		private ResourceLocation hoverIcon = null;
		private final Button.OnPress onPress;
		private int x;
		private int y;
		private int iconWidth = 16;
		private int iconHeight = 16;
		private int width = 150;
		private int height = 150;

		public Builder(ResourceLocation icon, Button.OnPress onPress) {
			this.icon = icon;
			this.onPress = onPress;
		}

		public IconButtonWidget.Builder position(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}

		public IconButtonWidget.Builder size(int width, int height, int iconWidth, int iconHeight) {
			this.width = width;
			this.height = height;
			this.iconWidth = iconWidth;
			this.iconHeight = iconHeight;
			return this;
		}

		public IconButtonWidget.Builder dimensions(int x, int y, int width, int height, int iconWidth, int iconHeight) {
			return this.position(x, y).size(width, height, iconWidth, iconHeight);
		}

		public IconButtonWidget.Builder hoverIcon(ResourceLocation hoverIcon) {
			this.hoverIcon = hoverIcon;
			return this;
		}

		public IconButtonWidget build() {
			return new IconButtonWidget(this.x, this.y, this.width, this.height, this.iconWidth, this.iconHeight, this.icon, this.hoverIcon, this.onPress);
		}

	}


}
