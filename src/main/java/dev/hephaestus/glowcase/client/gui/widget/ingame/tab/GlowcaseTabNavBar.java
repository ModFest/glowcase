package dev.hephaestus.glowcase.client.gui.widget.ingame.tab;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class GlowcaseTabNavBar extends AbstractWidget {
	public final List<GlowcaseTab> tabs = new ArrayList<>();
	public final List<GlowcaseTabButton> tabButtons = new ArrayList<>();
	public final Consumer<Integer> onHeightChange;
	public int tabButtonHeight, widgetAreaPadding;

	public static Builder builder(int width, Consumer<Integer> onHeightChange) {
		return new Builder(width, onHeightChange);
	}

	public GlowcaseTabNavBar(int x, int y, int width, int tabButtonHeight, int widgetAreaPadding, List<GlowcaseTab> tabs, Consumer<Integer> onHeightChange) {
		super(x, y, width, tabButtonHeight, Component.empty());
		this.tabButtonHeight = tabButtonHeight;
		this.widgetAreaPadding = widgetAreaPadding;
		this.onHeightChange = onHeightChange;

		LinearLayout tabsLayout = LinearLayout.horizontal();
		tabsLayout.defaultCellSetting().alignHorizontallyCenter();
		int totalTabsWidth = Math.min(400, this.getWidth()) - 28;
		int tabWidth = Mth.roundToward(totalTabsWidth / tabs.size(), 2);

		for (int i = 0; i < tabs.size(); i++) {
			GlowcaseTab tab = tabs.get(i);
			GlowcaseTabButton button = new GlowcaseTabButton(this, tab, i, 0, 0, tabWidth, tabButtonHeight);
			this.tabs.add(tab);
			this.tabButtons.add(button);
			tabsLayout.addChild(button);
		}

		tabsLayout.arrangeElements();
		tabsLayout.setPosition(Mth.roundToward((this.getWidth() - totalTabsWidth) / 2, 2), this.getY());
		this.selectTab(0);
	}

	public void selectTab(int tabIndex) {
		this.unselectAllTabs(); // Hide all widgets, included current selected

		this.tabButtons.get(tabIndex).select();

		GlowcaseTab tab = this.tabs.get(tabIndex);
		this.setHeight(this.tabButtonHeight + tab.heightForWidgetArea + this.widgetAreaPadding * 2);
	}

	public void unselectAllTabs() {
		for (GlowcaseTabButton tabButton : this.tabButtons) {
			tabButton.unselect();
		}
	}

	public void collapse() {
		this.unselectAllTabs();
		this.setHeight(this.tabButtonHeight);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (GlowcaseTabButton tabButton : tabButtons) {
			boolean canCollapse = tabButton.selected;
			if (tabButton.mouseClicked(event, doubleClick)) {
				if (canCollapse && doubleClick) this.collapse();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		for (GlowcaseTabButton tabButton : tabButtons) {
			if (tabButton.isMouseOver(mouseX, mouseY)) return true;
		}
		return super.isMouseOver(mouseX, mouseY);
	}

	@Override
	public void setHeight(int height) {
		super.setHeight(height);
		this.onHeightChange.accept(height);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.blit(
			RenderPipelines.GUI_TEXTURED, Screen.HEADER_SEPARATOR,
			0, this.tabButtonHeight,
			0, 0,
			this.tabButtons.getFirst().getX(), 2,
			32, 2
		);
		int afterLastTab = this.tabButtons.getLast().getRight();
		graphics.blit(
			RenderPipelines.GUI_TEXTURED, Screen.HEADER_SEPARATOR,
			afterLastTab, this.tabButtonHeight,
			0, 0,
			this.width, 2,
			32, 2
		);

		if (this.height != this.tabButtonHeight) { // Render footer if not collapsed
			graphics.blit(
				RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR,
				0, this.getHeight(),
				0, 0,
				this.width, 2,
				32, 2
			);
		}

		for (GlowcaseTabButton tabButton : this.tabButtons) {
			tabButton.extractRenderState(graphics, mouseX, mouseY, a);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		// NO-OP
	}

	public static class Builder {
		private int x, y;
		private int width;
		private int tabButtonHeight = 20;
		private int widgetAreaPadding = 1;
		private final Consumer<Integer> onHeightChange;
		private List<GlowcaseTab> tabs = new ArrayList<>();

		public Builder(int width, Consumer<Integer> onHeightChange) {
			this.width = width;
			this.onHeightChange = onHeightChange;
		}

		public Builder addTabs(GlowcaseTab... tabs) {
			Collections.addAll(this.tabs, tabs);
			return this;
		}

		public Builder setX(int x) {
			this.x = x;
			return this;
		}

		public Builder setY(int y) {
			this.y = y;
			return this;
		}

		public Builder setTabButtonHeight(int tabButtonHeight) {
			this.tabButtonHeight = tabButtonHeight;
			return this;
		}

		public Builder setWidgetAreaPadding(int widgetAreaPadding) {
			this.widgetAreaPadding = widgetAreaPadding;
			return this;
		}

		public GlowcaseTabNavBar build() {
			return new GlowcaseTabNavBar(
				this.x, this.y, this.width,
				this.tabButtonHeight, this.widgetAreaPadding,
				this.tabs, this.onHeightChange
			);
		}
	}

}
