package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Ints;
import dev.hephaestus.glowcase.block.entity.OutlineBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.HexColorEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.packet.C2SEditOutlineBlock;
import dev.hephaestus.glowcase.util.InputFilters;
import dev.hephaestus.glowcase.util.TextUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public class OutlineBlockEditScreen extends BlockEditorScreen<OutlineBlockEntity> implements ColorPickerIncludedScreen {
	private StringWidget offsetTextWidget;
	private StringWidget scaleTextWidget;
	private StringWidget colorTextWidget;
	private StringWidget widthTextWidget;

	private GlowcaseEditBox xOffsetWidget;
	private GlowcaseEditBox yOffsetWidget;
	private GlowcaseEditBox zOffsetWidget;
	private GlowcaseEditBox xScaleWidget;
	private GlowcaseEditBox yScaleWidget;
	private GlowcaseEditBox zScaleWidget;
	private HexColorEditBox colorWidget;
	private GlowcaseEditBox widthWidget;

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

		AtomicInteger widgetY = new AtomicInteger(initialY);

		this.offsetTextWidget = new StringWidget(width / 2 - 110, widgetY.getAndAdd(lineOffset), 40, 20, Component.translatable("gui.glowcase.offset"), this.font);
		this.scaleTextWidget = new StringWidget(width / 2 - 110, widgetY.getAndAdd(lineOffset), 40, 20, Component.translatable("gui.glowcase.scale"), this.font);
		this.colorTextWidget = new StringWidget(width / 2 - 110, widgetY.getAndAdd(lineOffset), 40, 20, Component.translatable("gui.glowcase.color"), this.font);
		this.widthTextWidget = new StringWidget(width / 2 - 110, widgetY.getAndAdd(lineOffset), 40, 20, Component.translatable("gui.glowcase.width"), this.font);

		// Reposition
		widgetY.set(initialY);

		this.xOffsetWidget = new GlowcaseEditBox(this.font, width / 2 - 65, widgetY.get(), 40, 20, Component.empty());
		this.yOffsetWidget = new GlowcaseEditBox(this.font, width / 2 - 20, widgetY.get(), 40, 20, Component.empty());
		this.zOffsetWidget = new GlowcaseEditBox(this.font, width / 2 + 25, widgetY.get(), 40, 20, Component.empty());
		widgetY.getAndAdd(lineOffset);

		this.xScaleWidget = new GlowcaseEditBox(this.font, width / 2 - 65, widgetY.get(), 40, 20, Component.empty());
		this.yScaleWidget = new GlowcaseEditBox(this.font, width / 2 - 20, widgetY.get(), 40, 20, Component.empty());
		this.zScaleWidget = new GlowcaseEditBox(this.font, width / 2 + 25, widgetY.get(), 40, 20, Component.empty());
		widgetY.getAndAdd(lineOffset);

		this.xOffsetWidget.setValue(String.valueOf(this.blockEntity.offset.getX()));
		this.yOffsetWidget.setValue(String.valueOf(this.blockEntity.offset.getY()));
		this.zOffsetWidget.setValue(String.valueOf(this.blockEntity.offset.getZ()));
		this.xScaleWidget.setValue(String.valueOf(this.blockEntity.scale.getX()));
		this.yScaleWidget.setValue(String.valueOf(this.blockEntity.scale.getY()));
		this.zScaleWidget.setValue(String.valueOf(this.blockEntity.scale.getZ()));

		this.xOffsetWidget.setFilter(InputFilters::integerNumber);
		this.yOffsetWidget.setFilter(InputFilters::integerNumber);
		this.zOffsetWidget.setFilter(InputFilters::integerNumber);
		this.xScaleWidget.setFilter(InputFilters::integerNumber);
		this.yScaleWidget.setFilter(InputFilters::integerNumber);
		this.zScaleWidget.setFilter(InputFilters::integerNumber);

		this.xOffsetWidget.setResponder(string -> {
			if (Ints.tryParse(string) instanceof Integer x) {
				Vec3i offset = this.blockEntity.offset;
				this.blockEntity.offset = new Vec3i(x, offset.getY(), offset.getZ());
			}
		});
		this.yOffsetWidget.setResponder(string -> {
			if (Ints.tryParse(string) instanceof Integer y) {
				Vec3i offset = this.blockEntity.offset;
				this.blockEntity.offset = new Vec3i(offset.getX(), y, offset.getZ());
			}
		});
		this.zOffsetWidget.setResponder(string -> {
			if (Ints.tryParse(string) instanceof Integer z) {
				Vec3i offset = this.blockEntity.offset;
				this.blockEntity.offset = new Vec3i(offset.getX(), offset.getY(), z);
			}
		});

		this.xScaleWidget.setResponder(string -> {
			if (Ints.tryParse(string) instanceof Integer x) {
				Vec3i scale = this.blockEntity.scale;
				this.blockEntity.scale = new Vec3i(x, scale.getY(), scale.getZ());
			}
		});
		this.yScaleWidget.setResponder(string -> {
			if (Ints.tryParse(string) instanceof Integer y) {
				Vec3i scale = this.blockEntity.scale;
				this.blockEntity.scale = new Vec3i(scale.getX(), y, scale.getZ());
			}
		});
		this.zScaleWidget.setResponder(string -> {
			if (Ints.tryParse(string) instanceof Integer z) {
				Vec3i scale = this.blockEntity.scale;
				this.blockEntity.scale = new Vec3i(scale.getX(), scale.getY(), z);
			}
		});

		this.xOffsetWidget.setHint(TextUtils.placeholder("gui.glowcase.x"));
		this.yOffsetWidget.setHint(TextUtils.placeholder("gui.glowcase.y"));
		this.zOffsetWidget.setHint(TextUtils.placeholder("gui.glowcase.z"));
		this.xScaleWidget.setHint(TextUtils.placeholder("gui.glowcase.x"));
		this.yScaleWidget.setHint(TextUtils.placeholder("gui.glowcase.y"));
		this.zScaleWidget.setHint(TextUtils.placeholder("gui.glowcase.z"));

		this.colorPickerWidget = this.createColorPickerWidget();

		this.colorWidget = HexColorEditBox.builder(this.minecraft.font, width / 2 - 65, widgetY.getAndAdd(lineOffset),
				() -> this.blockEntity.color, color -> this.blockEntity.color = color
			)
			.setWidth(50)
			.setColorPickerWidget(this.colorPickerWidget)
			.build();

		this.widthWidget = new GlowcaseEditBox(this.minecraft.font, width / 2 - 65, widgetY.getAndAdd(lineOffset), 50, 20, Component.empty());
		this.widthWidget.setValue(String.valueOf(this.blockEntity.width));
		this.widthWidget.setFilter(InputFilters::naturalNumber);
		this.widthWidget.setResponder(string -> this.blockEntity.width = Math.clamp(Integer.parseInt(string), 1, 5));

		this.addRenderableWidget(this.offsetTextWidget);
		this.addRenderableWidget(this.scaleTextWidget);
		this.addRenderableWidget(this.colorTextWidget);
		this.addRenderableWidget(this.widthTextWidget);
		this.addRenderableWidget(this.xOffsetWidget);
		this.addRenderableWidget(this.yOffsetWidget);
		this.addRenderableWidget(this.zOffsetWidget);
		this.addRenderableWidget(this.xScaleWidget);
		this.addRenderableWidget(this.yScaleWidget);
		this.addRenderableWidget(this.zScaleWidget);
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
