package dev.hephaestus.glowcase.client.gui.widget.ingame;

import dev.hephaestus.glowcase.util.InputFilters;
import dev.hephaestus.glowcase.util.ParseUtil;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class Vec3FieldsWidget extends AbstractContainerWidget {
	private final GlowcaseEditBox x;
	private final GlowcaseEditBox y;
	private final GlowcaseEditBox z;

	private Vec3 value;

	public Vec3FieldsWidget(int x, int y, int width, int height, Minecraft client, Vec3 defaultValue) {
		super(x, y, width, height, Component.empty(),  AbstractScrollArea.defaultSettings(10));
		this.x = new GlowcaseEditBox(
			client.font,
			x, y,
			width / 3, height,
			Component.empty()
		);

		this.y = new GlowcaseEditBox(
			client.font,
			x + width / 3, y,
			width / 3, height,
			Component.empty()
		);

		this.z = new GlowcaseEditBox(
			client.font,
			x + (width / 3 * 2), y,
			width / 3, height,
			Component.empty()
		);

		this.value = defaultValue;

		this.x.setValue(String.valueOf(defaultValue.x));
		this.y.setValue(String.valueOf(defaultValue.y));
		this.z.setValue(String.valueOf(defaultValue.z));

		this.x.setFilter(InputFilters::realNumber);
		this.y.setFilter(InputFilters::realNumber);
		this.z.setFilter(InputFilters::realNumber);

		this.x.setResponder(s -> value = new Vec3(ParseUtil.parseOrDefault(s, value.x), value.y , value.z));
		this.y.setResponder(s -> value = new Vec3(value.x, ParseUtil.parseOrDefault(s, value.y), value.z));
		this.z.setResponder(s -> value = new Vec3(value.x, value.y, ParseUtil.parseOrDefault(s, value.z)));
	}

	public void setVec(Vec3 newVec) {
		this.x.setValue(String.valueOf(newVec.x));
		this.y.setValue(String.valueOf(newVec.y));
		this.z.setValue(String.valueOf(newVec.z));
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return List.of(x, y, z);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		x.extractWidgetRenderState(context, mouseX, mouseY, delta);
		y.extractWidgetRenderState(context, mouseX, mouseY, delta);
		z.extractWidgetRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput builder) {
		x.updateWidgetNarration(builder);
		y.updateWidgetNarration(builder);
		z.updateWidgetNarration(builder);
	}

	public Vec3 value() {
		return value;
	}

	@Override
	protected int contentHeight() {
		return 9 + 4; //FIXME: get this right
	}

	@Override
	protected double scrollRate() {
		return 9.0 / 2.0;
	}
}
