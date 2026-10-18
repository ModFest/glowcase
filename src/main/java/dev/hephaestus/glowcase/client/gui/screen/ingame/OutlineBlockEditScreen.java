package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.OutlineBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.HexColorEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.number.NumberEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.number.Vec3iFieldsWidget;
import dev.hephaestus.glowcase.client.util.Tooltips;
import dev.hephaestus.glowcase.packet.C2SEditOutlineBlock;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public class OutlineBlockEditScreen extends BlockEditorScreen<OutlineBlockEntity> implements ColorPickerIncludedScreen {
	private StringWidget offsetTextWidget;
	private StringWidget scaleTextWidget;
	private StringWidget colorTextWidget;
	private StringWidget widthTextWidget;

	private Vec3iFieldsWidget offsetWidget;
	private Vec3iFieldsWidget scaleWidget;
	private HexColorEditBox colorWidget;
	private NumberEditBox widthWidget;

	private ColorPickerWidget colorPickerWidget;

	public OutlineBlockEditScreen(OutlineBlockEntity blockEntity) {
		super(blockEntity);
	}

	@Override
	public void init() {
		super.init();

		final int lineOffset = 30;
		final int lines = 4;
		final int initialY = height / 2 - (lineOffset * lines) / 2;

		// Atomic is *way* over the top for this.
		AtomicInteger widgetY = new AtomicInteger(initialY);

		// TODO: reformat this to not be lang-specific
		//  The text should go above the given field, not beside it.
		this.offsetTextWidget = new StringWidget(width / 2 - 110, widgetY.getAndAdd(lineOffset), 40, 20, Component.translatable("gui.glowcase.offset"), this.font);
		this.scaleTextWidget = new StringWidget(width / 2 - 110, widgetY.getAndAdd(lineOffset), 40, 20, Component.translatable("gui.glowcase.scale"), this.font);
		this.colorTextWidget = new StringWidget(width / 2 - 110, widgetY.getAndAdd(lineOffset), 40, 20, Component.translatable("gui.glowcase.color"), this.font);
		this.widthTextWidget = new StringWidget(width / 2 - 110, widgetY.getAndAdd(lineOffset), 40, 20, Component.translatable("gui.glowcase.width"), this.font);

		// Reposition
		widgetY.set(initialY);

		this.offsetWidget = Vec3iFieldsWidget.builder(this.font, this.blockEntity.offset)
			.setOnValueChange(value -> this.blockEntity.offset = value)
			.setPos(width / 2 - 65, widgetY.get())
			.setSize(150, 20)
			.setColored(true)
			.setTooltips(Tooltips.OFFSET_AXES)
			.build();
		widgetY.getAndAdd(lineOffset);

		this.scaleWidget = Vec3iFieldsWidget.builder(this.font, this.blockEntity.scale)
			.setOnValueChange(value -> this.blockEntity.scale = value)
			.setPos(width / 2 - 65, widgetY.get())
			.setSize(150, 20)
			.setColored(true)
			.setTooltips(Tooltips.SCALE_AXES)
			.build();
		widgetY.getAndAdd(lineOffset);

		this.colorPickerWidget = this.createColorPickerWidget();

		this.colorWidget = HexColorEditBox.builder(this.minecraft.font, width / 2 - 65, widgetY.getAndAdd(lineOffset),
				() -> this.blockEntity.color, color -> this.blockEntity.color = color
			)
			.setWidth(50)
			.setColorPickerWidget(this.colorPickerWidget)
			.build();

		this.widthWidget = NumberEditBox.integerBuilder(
				this.minecraft.font,
				this.blockEntity.width,
				value -> this.blockEntity.width = value
			)
			.setMinMaxValues(1, 5)
			.setBounds(width / 2 - 65, widgetY.getAndAdd(lineOffset), 50, 20)
			.build();

		this.addRenderableWidget(this.offsetTextWidget);
		this.addRenderableWidget(this.scaleTextWidget);
		this.addRenderableWidget(this.colorTextWidget);
		this.addRenderableWidget(this.widthTextWidget);
		this.addRenderableWidget(this.offsetWidget);
		this.addRenderableWidget(this.scaleWidget);
		this.addRenderableWidget(this.colorWidget);
		this.addRenderableWidget(this.widthWidget);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		this.extractColorPicker(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.mouseClickedColorPicker(event, doubleClick)) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.keyPressedColorPicker(event)) return true;
		return super.keyPressed(event);
	}

	@Override
	public @Nullable CustomPacketPayload getUpdatePayload() {
		return C2SEditOutlineBlock.of(blockEntity);
	}

	@Override
	public ColorPickerWidget getColorPickerWidget() {
		return this.colorPickerWidget;
	}
}
