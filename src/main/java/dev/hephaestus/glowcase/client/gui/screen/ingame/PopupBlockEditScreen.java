package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.HexColorEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
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

//TODO: multi-character selection at some point? it may be a bit complex but it'd be nice
public class PopupBlockEditScreen extends BlockEditorScreen<PopupBlockEntity> implements ColorPickerIncludedScreen {
	private TextFieldHelper selectionManager;
	private int currentRow;
	private long ticksSinceOpened = 0;
	private EditBox titleEntryWidget;
	private Button changeAlignment;
	private HexColorEditBox colorEntryWidget;

	private ColorPickerWidget colorPickerWidget;

	public PopupBlockEditScreen(PopupBlockEntity blockEntity) {
		super(blockEntity);
	}

	@Override
	public void init() {
		super.init();

		int innerPadding = width / 100;

		this.selectionManager = new TextFieldHelper(
			() -> this.blockEntity.getRawLine(this.currentRow),
			(string) -> {
				blockEntity.setRawLine(this.currentRow, string);
				this.blockEntity.renderDirty = true;
			},
			TextFieldHelper.createClipboardGetter(this.minecraft),
			TextFieldHelper.createClipboardSetter(this.minecraft),
			(string) -> true);

		this.titleEntryWidget = new EditBox(this.minecraft.font, width / 10, 0, 8 * width / 10, 20, Component.empty());
		this.titleEntryWidget.setMaxLength(HyperlinkBlockEntity.TITLE_MAX_LENGTH);
		this.titleEntryWidget.setValue(this.blockEntity.title);
		this.titleEntryWidget.setHint(TextUtils.placeholder("gui.glowcase.title"));
		this.titleEntryWidget.setResponder(string -> {
			this.blockEntity.title = this.titleEntryWidget.getValue();
			this.blockEntity.renderDirty = true;
		});

		this.changeAlignment = Button.builder(Component.translatableEscape(
			"gui.glowcase.alignment",
			this.blockEntity.textAlignment
		), action -> {
			switch (blockEntity.textAlignment) {
				case LEFT -> blockEntity.textAlignment = TextBlockEntity.TextAlignment.CENTER;
				case CENTER, CENTER_LEFT, CENTER_RIGHT ->
					blockEntity.textAlignment = TextBlockEntity.TextAlignment.RIGHT;
				case RIGHT -> blockEntity.textAlignment = TextBlockEntity.TextAlignment.LEFT;
			}
			this.blockEntity.renderDirty = true;

			this.changeAlignment.setMessage(Component.translatableEscape(
				"gui.glowcase.alignment",
				this.blockEntity.textAlignment
			));
		}).bounds(120 + innerPadding, 20 + innerPadding, 160, 20).build();

		this.colorPickerWidget = createColorPickerWidget();
		this.colorEntryWidget = HexColorEditBox.builder(this.minecraft.font, 280 + innerPadding, 20 + innerPadding,
				() -> this.blockEntity.color, color -> { this.blockEntity.color = color; this.blockEntity.renderDirty = true; }
			)
			.setWidth(50)
			.setColorPickerWidget(this.colorPickerWidget)
			.build();

		this.addRenderableWidget(this.titleEntryWidget);
		this.addRenderableWidget(this.changeAlignment);
		this.addRenderableWidget(this.colorEntryWidget);
	}

	@Override
	public void tick() {
		++this.ticksSinceOpened;
	}

	@Override
	public @Nullable CustomPacketPayload getUpdatePayload() {
		return C2SEditPopupBlock.of(blockEntity);
	}

	private void checkRow() {
		final int size = this.blockEntity.lines.size();
		if (this.currentRow >= size) {
			this.currentRow = size - 1;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		graphics.pose().pushMatrix();
		graphics.pose().translate(0, 40 + 2 * this.width / 100F);
		for (int i = 0; i < this.blockEntity.lines.size(); ++i) {
			var text = this.currentRow == i ?
				Component.literal(this.blockEntity.getRawLine(i)) :
				this.blockEntity.lines.get(i);

			int lineWidth = this.font.width(text);
			switch (this.blockEntity.textAlignment) {
				case LEFT -> graphics.text(minecraft.font, text, this.width / 10, i * 12, this.blockEntity.color);
				case CENTER, CENTER_LEFT, CENTER_RIGHT ->
					graphics.text(minecraft.font, text, this.width / 2 - lineWidth / 2, i * 12, this.blockEntity.color);
				case RIGHT -> graphics.text(minecraft.font,
					text,
					this.width - this.width / 10 - lineWidth,
					i * 12,
					this.blockEntity.color
				);
			}
		}

		int caretStart = this.selectionManager.getCursorPos();
		int caretEnd = this.selectionManager.getSelectionPos();

		if (caretStart >= 0) {
			this.checkRow();
			String line = this.blockEntity.getRawLine(this.currentRow);
			int selectionStart = Mth.clamp(Math.min(caretStart, caretEnd), 0, line.length());
			int selectionEnd = Mth.clamp(Math.max(caretStart, caretEnd), 0, line.length());

			String preSelection = line.substring(0, Mth.clamp(line.length(), 0, selectionStart));
			int startX = this.minecraft.font.width(preSelection);

			float push = switch (this.blockEntity.textAlignment) {
				case LEFT -> this.width / 10F;
				case CENTER, CENTER_LEFT, CENTER_RIGHT -> this.width / 2F - this.font.width(line) / 2F;
				case RIGHT -> this.width - this.width / 10F - this.font.width(line);
			};

			startX += (int) push;


			int caretStartY = this.currentRow * 12;
			if (this.ticksSinceOpened / 6 % 2 == 0 && !this.titleEntryWidget.canConsumeInput() && !this.colorEntryWidget.canConsumeInput()) {
				if (selectionStart < line.length()) {
					graphics.fill(startX, caretStartY, startX + 1, caretStartY + 9, 0xCCFFFFFF);
				} else {
					graphics.text(minecraft.font, "_", startX, this.currentRow * 12, 0xFFFFFFFF, false);
				}
			}

			if (caretStart != caretEnd) {
				int endX = startX + this.minecraft.font.width(line.substring(selectionStart, selectionEnd));
				graphics.textHighlight(startX, caretStartY, endX, caretStartY + 9, false);
			}
		}

		graphics.pose().popMatrix();
		this.extractColorPicker(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (this.titleEntryWidget.canConsumeInput()) {
			return this.titleEntryWidget.charTyped(event);
		} else if (this.colorEntryWidget.canConsumeInput()) {
			return this.colorEntryWidget.charTyped(event);
		} else {
			this.selectionManager.charTyped(event);
			return true;
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int keyCode = event.key();
		if (this.keyPressedColorPicker(event)) return true;
		if (this.titleEntryWidget.canConsumeInput()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				this.onClose();
				return true;
			} else {
				return this.titleEntryWidget.keyPressed(event);
			}
		} else if (this.colorEntryWidget.canConsumeInput()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				this.onClose();
				return true;
			} else {
				return this.colorEntryWidget.keyPressed(event);
			}
		} else {
			setFocused(null);
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				this.blockEntity.addRawLine(
					this.currentRow + 1,
					this.blockEntity.getRawLine(this.currentRow).substring(
						Mth.clamp(
							this.selectionManager.getCursorPos(),
							0,
							this.blockEntity.getRawLine(this.currentRow).length()
						)
					));
				this.blockEntity.setRawLine(
					this.currentRow,
					this.blockEntity.getRawLine(this.currentRow)
						.substring(
							0,
							Mth.clamp(
								this.selectionManager.getCursorPos(),
								0,
								this.blockEntity.getRawLine(this.currentRow).length()
							)
					));
				this.blockEntity.renderDirty = true;
				++this.currentRow;
				this.selectionManager.setCursorToStart();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_UP) {
				this.currentRow = Math.max(this.currentRow - 1, 0);
				this.selectionManager.setCursorToEnd();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_DOWN) {
				this.currentRow = Math.min(this.currentRow + 1, (this.blockEntity.lines.size() - 1));
				this.selectionManager.setCursorToEnd();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && this.currentRow > 0 && this.blockEntity.lines.size() > 1 && this.selectionManager.getCursorPos() == 0 && this.selectionManager.getSelectionPos() == this.selectionManager.getCursorPos()) {
				--this.currentRow;
				this.selectionManager.setCursorToEnd();
				deleteLine();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_DELETE && this.currentRow < this.blockEntity.lines.size() - 1 && this.selectionManager.getSelectionPos() == this.blockEntity.getRawLine(
				this.currentRow).length()) {
				deleteLine();
				return true;
			} else {
				try {
					boolean val = this.selectionManager.keyPressed(event) || super.keyPressed(event);
					int selectionOffset = this.blockEntity.getRawLine(this.currentRow)
											  .length() - this.selectionManager.getCursorPos();

					// Find line feed characters and create proper newlines
					for (int i = 0; i < this.blockEntity.lines.size(); ++i) {
						int lineFeedIndex = this.blockEntity.getRawLine(i).indexOf("\n");

						if (lineFeedIndex >= 0) {
							this.blockEntity.addRawLine(
								i + 1,
								this.blockEntity.getRawLine(i).substring(
									Mth.clamp(lineFeedIndex + 1, 0, this.blockEntity.getRawLine(i).length())
								));
							this.blockEntity.setRawLine(
								i,
								this.blockEntity.getRawLine(i)
									.substring(0, Mth.clamp(lineFeedIndex, 0, this.blockEntity.getRawLine(i).length())
								));
							this.blockEntity.renderDirty = true;
							++this.currentRow;
							this.selectionManager.setCursorToEnd();
							this.selectionManager.moveByChars(-selectionOffset);
						}
					}
					return val;
				} catch (StringIndexOutOfBoundsException e) {
					e.printStackTrace();
					Minecraft.getInstance().setScreen(null);
					return false;
				}
			}
		}
	}

	private void deleteLine() {
		this.blockEntity.setRawLine(
			this.currentRow,
			this.blockEntity.getRawLine(this.currentRow) + this.blockEntity.getRawLine(this.currentRow + 1)
		);

		this.blockEntity.lines.remove(this.currentRow + 1);
		this.blockEntity.renderDirty = true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int topOffset = (int) (40 + 2 * this.width / 100F);
		if (this.mouseClickedColorPicker(event, doubleClick)) return true;
		if (!this.titleEntryWidget.mouseClicked(event, doubleClick)) {
			this.titleEntryWidget.setFocused(false);
		}
		if (!this.colorEntryWidget.mouseClicked(event, doubleClick)) {
			this.colorEntryWidget.setFocused(false);
		}
		if (mouseY > topOffset) {
			this.currentRow = Mth.clamp((int) (mouseY - topOffset) / 12, 0, this.blockEntity.lines.size() - 1);
			this.setFocused(null);
			String baseContents = this.blockEntity.getRawLine(currentRow);
			int baseContentsWidth = this.font.width(baseContents);
			int contentsStart;
			int contentsEnd;
			switch (this.blockEntity.textAlignment) {
				case LEFT -> {
					contentsStart = this.width / 10;
					contentsEnd = contentsStart + baseContentsWidth;
				}
				case CENTER, CENTER_LEFT, CENTER_RIGHT -> {
					int midpoint = this.width / 2;
					int textMidpoint = baseContentsWidth / 2;
					contentsStart = midpoint - textMidpoint;
					contentsEnd = midpoint + textMidpoint;
				}
				case RIGHT -> {
					contentsEnd = this.width - this.width / 10;
					contentsStart = contentsEnd - baseContentsWidth;
				}
				//even though this is exhaustive, javac won't treat contentsStart and contentsEnd as initialized
				//why? who knows! just throw bc this should be impossible
				default -> throw new IllegalStateException(":HOW:");
			}

			if (mouseX <= contentsStart) {
				this.selectionManager.setCursorToStart();
			} else if (mouseX >= contentsEnd) {
				this.selectionManager.setCursorToEnd();
			} else {
				int lastWidth = 0;
				for (int i = 1; i < baseContents.length(); i++) {
					String testContents = baseContents.substring(0, i);
					int width = this.font.width(testContents);
					int midpointWidth = (width + lastWidth) / 2;
					if (mouseX < contentsStart + midpointWidth) {
						this.selectionManager.setCursorPos(i - 1, false);
						break;
					} else if (mouseX <= contentsStart + width) {
						this.selectionManager.setCursorPos(i, false);
						break;
					}
					lastWidth = width;
				}
			}
			return true;
		} else {
			return super.mouseClicked(event, doubleClick);
		}
	}

	@Override
	public ColorPickerWidget getColorPickerWidget() {
		return this.colorPickerWidget;
	}
}
