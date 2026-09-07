package dev.hephaestus.glowcase.client.gui.widget.ingame.tab;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A container of information for a tab in a {@link GlowcaseTabNavBar}. All widgets included will be shown when this tab is selected, and hidden when unselected.
 * @apiNote Widget area heights are normally 20 pixels per row, with 2 pixel padding between rows (e.g. 1 row = 20px, 2 rows = 42px, 3 rows = 64px)
 * @see GlowcaseTabNavBar
 * @author Superkat32
 */
public class GlowcaseTab {
	public final Component title;
	public final List<AbstractWidget> widgets;
	public int heightForWidgetArea;

	public GlowcaseTab(Component title, int heightForWidgetArea, List<AbstractWidget> widgets) {
		this.title = title;
		this.widgets = widgets;
		this.heightForWidgetArea = heightForWidgetArea;
	}

	public void toggleWidgets(boolean visible) {
		this.widgets.forEach(widget -> {
			widget.visible = visible;
		});
	}
}
