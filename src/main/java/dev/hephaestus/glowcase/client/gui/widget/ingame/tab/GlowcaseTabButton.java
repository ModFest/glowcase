package dev.hephaestus.glowcase.client.gui.widget.ingame.tab;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * A clickable button which, when clicked, handles the actions for switching tabs in a {@link GlowcaseTabNavBar}.
 * @see GlowcaseTab
 * @see GlowcaseTabNavBar
 * @author Superkat32
 */
public class GlowcaseTabButton extends AbstractButton {
	public static final WidgetSprites SPRITES = new WidgetSprites(
		Glowcase.id("tab/tab_selected"),
		Glowcase.id("tab/tab"),
		Glowcase.id("tab/tab_selected_highlighted"),
		Glowcase.id("tab/tab_highlighted")
	);

	public final GlowcaseTabNavBar navBar;
	public final GlowcaseTab tab;
	public final int tabIndex;
	public final Component inactiveTitle;
	public boolean selected = false;

	public GlowcaseTabButton(GlowcaseTabNavBar navBar, GlowcaseTab tab, int tabIndex, int x, int y, int width, int height) {
		super(x, y, width, height, tab.title);
		this.navBar = navBar;
		this.tab = tab;
		this.tabIndex = tabIndex;
		this.inactiveTitle = AbstractWidget.WithInactiveMessage.defaultInactiveMessage(tab.title);
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.navBar.selectTab(this.tabIndex);
	}

	public void select() {
		this.selected = true;
		this.tab.toggleWidgets(true);
	}

	public void unselect() {
		this.selected = false;
		this.tab.toggleWidgets(false);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.blitSprite(
			RenderPipelines.GUI_TEXTURED, SPRITES.get(this.selected, this.isHoveredOrFocused()), this.getX(), this.getY(), this.width, this.height
		);

		Font font = Minecraft.getInstance().font;
		Component text = this.selected ? this.getMessage().copy().withStyle(ChatFormatting.UNDERLINE)
			: this.active ? this.getMessage() : this.inactiveTitle;
		graphics.centeredText(font, text, this.getX() + this.getWidth() / 2, this.getY() + this.getHeight() / 4 + 2, ColorUtil.WHITE);

		if (this.isHovered()) graphics.requestCursor(CursorTypes.POINTING_HAND);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
