package dev.hephaestus.glowcase.client.gui.widget.ingame.tab;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.List;

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
