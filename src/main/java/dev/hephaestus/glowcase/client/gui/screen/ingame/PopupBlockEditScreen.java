package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.HexColorEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.text.GlowcaseMultilineEditBox;
import dev.hephaestus.glowcase.packet.C2SEditPopupBlock;
import dev.hephaestus.glowcase.util.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class PopupBlockEditScreen extends TextEditorScreen implements BlockEditor<PopupBlockEntity> {
	private final PopupBlockEntity popupBlockEntity;

	private GlowcaseMultilineEditBox glowcaseEditBox;
	private GlowcaseEditBox titleEntryWidget;
	private Button changeAlignment;
	private HexColorEditBox colorEntryWidget;

	private ColorPickerWidget colorPickerWidget;

	public PopupBlockEditScreen(PopupBlockEntity blockEntity) {
		this.popupBlockEntity = blockEntity;
	}

	@Override
	public void init() {
		super.init();

		int innerPadding = this.width / 100;
		int middle = this.width / 2;

		this.glowcaseEditBox = GlowcaseMultilineEditBox.builder(
			this.font, this.popupBlockEntity.lines,
			2, 40 + innerPadding, this.width - 4, this.height - 40 - innerPadding,
			parsedLines -> {
				this.popupBlockEntity.lines = parsedLines;
				this.popupBlockEntity.renderDirty = true;
			}
		).build();
		this.glowcaseEditBox.updateSettings(this.popupBlockEntity.color, true, this.popupBlockEntity.textAlignment);

		this.titleEntryWidget = new GlowcaseEditBox(this.font, this.width / 10, 2, 8 * this.width / 10, 20, Component.empty());
		this.titleEntryWidget.setMaxLength(HyperlinkBlockEntity.TITLE_MAX_LENGTH);
		this.titleEntryWidget.setValue(this.popupBlockEntity.title);
		this.titleEntryWidget.setHint(Component.translatable("gui.glowcase.title"));
		this.titleEntryWidget.setResponder(value -> {
			this.popupBlockEntity.title = value;
			this.popupBlockEntity.renderDirty = true;
		});

		// startX = middle - (160 + 2 + 50 + 4 + (20 + 2) * 6) / 2 = middle - 174;
		this.changeAlignment = Button.builder(Component.translatableEscape(
			"gui.glowcase.alignment",
			this.popupBlockEntity.textAlignment
		), action -> {
			switch (popupBlockEntity.textAlignment) {
				case LEFT -> popupBlockEntity.textAlignment = TextBlockEntity.TextAlignment.CENTER;
				case CENTER, CENTER_LEFT, CENTER_RIGHT ->
					popupBlockEntity.textAlignment = TextBlockEntity.TextAlignment.RIGHT;
				case RIGHT -> popupBlockEntity.textAlignment = TextBlockEntity.TextAlignment.LEFT;
			}
			this.popupBlockEntity.renderDirty = true;

			this.changeAlignment.setMessage(Component.translatableEscape(
				"gui.glowcase.alignment",
				this.popupBlockEntity.textAlignment
			));

			this.glowcaseEditBox.setTextAlignment(this.popupBlockEntity.textAlignment);
		}).bounds(middle - 174, 20 + innerPadding, 160, 20).build();

		this.colorPickerWidget = createColorPickerWidget();
		this.colorEntryWidget = HexColorEditBox.builder(this.minecraft.font, middle - 174 + 162, 20 + innerPadding,
			() -> this.popupBlockEntity.color, color -> {
				this.popupBlockEntity.color = color;
				this.popupBlockEntity.renderDirty = true;
				this.glowcaseEditBox.setTextColor(this.popupBlockEntity.color);
			})
			.setWidth(50)
			.setColorPickerWidget(this.colorPickerWidget)
			.build();

		this.initFormattingButtons(middle - 174 + 162 + 54, 20 + innerPadding, 0);

		this.addRenderableWidget(this.glowcaseEditBox);
		this.addRenderableWidget(this.titleEntryWidget);
		this.addRenderableWidget(this.changeAlignment);
		this.addRenderableWidget(this.colorEntryWidget);
	}

	@Override
	public @Nullable CustomPacketPayload getUpdatePayload() {
		return C2SEditPopupBlock.of(popupBlockEntity);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		this.extractColorPicker(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.keyPressedColorPicker(event)) return true;
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.mouseClickedColorPicker(event, doubleClick)) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public PopupBlockEntity getBlockEntity() {
		return this.popupBlockEntity;
	}

	@Override
	public ColorPickerWidget getColorPickerWidget() {
		return this.colorPickerWidget;
	}

	@Override
	GlowcaseMultilineEditBox getGlowcaseMultilineEditBox() {
		return this.glowcaseEditBox;
	}
}
