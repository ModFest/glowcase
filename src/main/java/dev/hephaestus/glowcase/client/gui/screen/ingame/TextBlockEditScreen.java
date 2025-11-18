package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Floats;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.ColorPickerWidget;
import dev.hephaestus.glowcase.util.ColorUtil;
import dev.hephaestus.glowcase.packet.C2SEditTextBlock;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.List;

//TODO: multi-character selection at some point? it may be a bit complex but it'd be nice
public class TextBlockEditScreen extends TextEditorScreen {
	private static final int innerPadding = 4;
	private final TextBlockEntity textBlockEntity;

	private List<TextFieldWidget> textWidgets;

	private List<TextFieldWidget> colorListeners;

	private SelectionManager selectionManager;
	private int currentRow;
	private long ticksSinceOpened = 0;
	private ColorPickerWidget colorPickerWidget;
	private ButtonWidget changeAlignment;
	private TextFieldWidget colorEntryWidget;
	private TextFieldWidget backgroundColorEntryWidget;
	private Color colorEntryPreColorPicker; //used for color picker cancel button
	private ButtonWidget zOffsetToggle;
	private CheckboxWidget shadowToggle;

	private TextFieldWidget viewDistanceField;
	private ButtonWidget viewDistanceHelpButton;

	public TextBlockEditScreen(TextBlockEntity textBlockEntity) {
		this.textBlockEntity = textBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		this.selectionManager = new SelectionManager(
			() -> this.textBlockEntity.getRawLine(this.currentRow),
			(string) -> {
				textBlockEntity.setRawLine(this.currentRow, string);
				this.textBlockEntity.renderDirty = true;
			},
			SelectionManager.makeClipboardGetter(this.client),
			SelectionManager.makeClipboardSetter(this.client),
			(string) -> true);

		int middle = width / 2;

		ButtonWidget decreaseSize = ButtonWidget.builder(Text.literal("-"), action -> {
			this.textBlockEntity.scale = Math.max(0, this.textBlockEntity.scale - (Screen.hasShiftDown() ? 1F : 0.125F));
			this.textBlockEntity.renderDirty = true;
		}).dimensions(middle - 130, 0, 20, 20).build();

		ButtonWidget increaseSize = ButtonWidget.builder(Text.literal("+"), action -> {
			this.textBlockEntity.scale += Screen.hasShiftDown() ? 1F : 0.125F;
			this.textBlockEntity.renderDirty = true;
		}).dimensions(middle - 110, 0, 20, 20).build();

		this.changeAlignment = ButtonWidget.builder(Text.stringifiedTranslatable("gui.glowcase.alignment", this.textBlockEntity.textAlignment), action -> {
			switch (textBlockEntity.textAlignment) {
				case LEFT -> textBlockEntity.textAlignment = TextBlockEntity.TextAlignment.CENTER;
				case CENTER -> textBlockEntity.textAlignment = TextBlockEntity.TextAlignment.CENTER_LEFT;
				case CENTER_LEFT -> textBlockEntity.textAlignment = TextBlockEntity.TextAlignment.CENTER_RIGHT;
				case CENTER_RIGHT -> textBlockEntity.textAlignment = TextBlockEntity.TextAlignment.RIGHT;
				case RIGHT -> textBlockEntity.textAlignment = TextBlockEntity.TextAlignment.LEFT;
			}
			this.textBlockEntity.renderDirty = true;

			this.changeAlignment.setMessage(Text.stringifiedTranslatable("gui.glowcase.alignment", this.textBlockEntity.textAlignment));
		}).dimensions(middle - 90 + innerPadding, 0, 160, 20).build();

		this.shadowToggle = CheckboxWidget.builder(Text.translatable("gui.glowcase.shadow"), this.textRenderer)
			.checked(this.textBlockEntity.shadow)
			.callback((widget, checked) -> {
				this.textBlockEntity.shadow = checked;
				this.textBlockEntity.renderDirty = true;
			})
			.pos(middle - 90 + innerPadding, 20 + innerPadding).build();

		this.colorEntryWidget = new TextFieldWidget(this.client.textRenderer, middle + 70 + innerPadding * 2, 0, 64, 20, Text.empty());
		this.colorEntryWidget.setTooltip(Tooltip.of(Text.translatable("gui.glowcase.color")));
		this.colorEntryWidget.setText(ColorUtil.toAlphaHex(this.textBlockEntity.color));
		this.colorEntryWidget.setChangedListener(string -> {
			ColorUtil.parse(this.colorEntryWidget.getText(), this.textBlockEntity.color).ifSuccess(newColor -> {
				final int color = (Math.max(newColor >>> 24, 0x1A) << 24) | (newColor & ColorUtil.COLOR_MASK);

				this.textBlockEntity.color = color;
				//make sure it doesn't update from the color picker updating the text
				if (this.colorEntryWidget.isFocused()) {
					this.colorPickerWidget.setColor(new Color(color));
				}
				this.textBlockEntity.renderDirty = true;
			});
		});

		this.backgroundColorEntryWidget = new TextFieldWidget(this.client.textRenderer, middle + 136 + innerPadding * 2, 0, 64, 20, Text.empty());
		this.backgroundColorEntryWidget.setTooltip(Tooltip.of(Text.translatable("gui.glowcase.background_color")));
		this.backgroundColorEntryWidget.setText(ColorUtil.toAlphaHex(this.textBlockEntity.backgroundColor));
		this.backgroundColorEntryWidget.setChangedListener(string -> {
			ColorUtil.parse(string, this.textBlockEntity.backgroundColor).ifSuccess(newColor -> {
				this.textBlockEntity.backgroundColor = newColor;
				if (this.colorEntryWidget.isFocused()) {
					this.colorPickerWidget.setColor(new Color(newColor));
				}
				this.textBlockEntity.renderDirty = true;
			});
		});

		this.zOffsetToggle = ButtonWidget.builder(Text.literal(this.textBlockEntity.zOffset.name()), action -> {
			switch (textBlockEntity.zOffset) {
				case FRONT -> textBlockEntity.zOffset = TextBlockEntity.ZOffset.CENTER;
				case CENTER -> textBlockEntity.zOffset = TextBlockEntity.ZOffset.BACK;
				case BACK -> textBlockEntity.zOffset = TextBlockEntity.ZOffset.FRONT;
			}
			this.textBlockEntity.renderDirty = true;

			this.zOffsetToggle.setMessage(Text.literal(this.textBlockEntity.zOffset.name()));
		}).dimensions(middle + 2, 20 + innerPadding, 72, 20).build();

		this.colorPickerWidget = ColorPickerWidget.builder(this, 216, 10).size(182, 104).build();
		this.colorPickerWidget.toggle(false); //start deactivated

		this.viewDistanceField = new TextFieldWidget(this.client.textRenderer, middle - 203, 20 + innerPadding, 83 + innerPadding, 20, Text.empty());
		this.viewDistanceField.setText(String.valueOf(this.textBlockEntity.viewDistance));
		this.viewDistanceField.setChangedListener(s -> {
			if (Floats.tryParse(s) instanceof Float parsed) {
				this.textBlockEntity.viewDistance = parsed;
			}
		});
		this.viewDistanceField.setTooltip(Tooltip.of(Text.translatable("gui.glowcase.screen.text_edit.view_distance")));
		this.viewDistanceHelpButton = ButtonWidget.builder(Text.literal("?"), action -> {
			})
			.dimensions(middle - 115 + innerPadding + 5, 20 + innerPadding, 20, 20).build();
		this.viewDistanceHelpButton.setTooltip(Tooltip.of(Text.translatable("gui.glowcase.screen.text_edit.view_distance")));

		this.addDrawableChild(colorPickerWidget);
		this.addDrawableChild(increaseSize);
		this.addDrawableChild(decreaseSize);
		this.addDrawableChild(this.changeAlignment);
		this.addDrawableChild(this.shadowToggle);
		this.addDrawableChild(this.zOffsetToggle);
		this.addDrawableChild(this.colorEntryWidget);
		this.addDrawableChild(this.backgroundColorEntryWidget);

		this.addDrawableChild(this.viewDistanceField);
		this.addDrawableChild(this.viewDistanceHelpButton);

		this.textWidgets = List.of(
			this.colorEntryWidget,
			this.backgroundColorEntryWidget,
			this.viewDistanceField
		);

		this.colorListeners = List.of(
			this.colorEntryWidget,
			this.backgroundColorEntryWidget
		);

		addFormattingButtons(middle + 70, 20, innerPadding, 20, 2);
	}

	@Override
	public void tick() {
		++this.ticksSinceOpened;
	}

	@Override
	public void close() {
		C2SEditTextBlock.of(textBlockEntity).send();
		super.close();
	}

	private boolean isFocusedTextActive() {
		final Element focused = this.getFocused();
		if (focused instanceof TextFieldWidget text) {
			return text.isActive();
		}
		return false;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (this.client != null) {
			super.render(context, mouseX, mouseY, delta);

			context.getMatrices().pushMatrix();
			context.getMatrices().translate(0, 40 + 2 * this.width / 100F);
			for (int i = 0; i < this.textBlockEntity.lines.size(); ++i) {
				var text = this.currentRow == i ? Text.literal(this.textBlockEntity.getRawLine(i)) : this.textBlockEntity.lines.get(i);

				int lineWidth = this.textRenderer.getWidth(text);
				switch (this.textBlockEntity.textAlignment) {
					case LEFT -> context.drawTextWithShadow(client.textRenderer, text, this.width / 10, i * 12, this.textBlockEntity.color);
					case CENTER, CENTER_LEFT, CENTER_RIGHT -> context.drawTextWithShadow(client.textRenderer, text, this.width / 2 - lineWidth / 2, i * 12, this.textBlockEntity.color);
					case RIGHT -> context.drawTextWithShadow(client.textRenderer, text, this.width - this.width / 10 - lineWidth, i * 12, this.textBlockEntity.color);
				}
			}

			int caretStart = this.selectionManager.getSelectionStart();
			int caretEnd = this.selectionManager.getSelectionEnd();

			if (caretStart >= 0) {
				String line = this.textBlockEntity.getRawLine(this.currentRow);
				int selectionStart = MathHelper.clamp(Math.min(caretStart, caretEnd), 0, line.length());
				int selectionEnd = MathHelper.clamp(Math.max(caretStart, caretEnd), 0, line.length());

				String preSelection = line.substring(0, MathHelper.clamp(line.length(), 0, selectionStart));
				int startX = this.client.textRenderer.getWidth(preSelection);

				float push = switch (this.textBlockEntity.textAlignment) {
					case LEFT -> this.width / 10F;
					case CENTER, CENTER_LEFT, CENTER_RIGHT -> this.width / 2F - this.textRenderer.getWidth(line) / 2F;
					case RIGHT -> this.width - this.width / 10F - this.textRenderer.getWidth(line);
				};

				startX += (int) push;


				int caretStartY = this.currentRow * 12;
				if (this.ticksSinceOpened / 6 % 2 == 0 && !this.isFocusedTextActive()) {
					if (selectionStart < line.length()) {
						context.fill(startX, caretStartY, startX + 1, caretStartY + 9, 0xCCFFFFFF);
					} else {
						context.drawText(client.textRenderer, "_", startX, this.currentRow * 12, 0xFFFFFFFF, false);
					}
				}

				if (caretStart != caretEnd) {
					int endX = startX + this.client.textRenderer.getWidth(line.substring(selectionStart, selectionEnd));
					context.drawSelection(startX, caretStartY, endX, caretStartY + 9);
				}
			}

			context.getMatrices().popMatrix();
			context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.glowcase.scale_value", this.textBlockEntity.scale), width / 2 - 203, 7, 0xFFFFFFFF);
			colorPickerWidget.render(context, mouseX, mouseY, delta);
		}
	}

	@Override
	public boolean charTyped(char chr, int keyCode) {
		for (final var element : this.textWidgets) {
			if (element.charTyped(chr, keyCode)) {
				return true;
			}
		}

		return this.selectionManager.insert(chr);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
			for (final var element : this.textWidgets) {
				if (element.isFocused()) {
					return element.keyPressed(keyCode, scanCode, modifiers);
				}
			}
		}

		if (this.colorPickerWidget.active && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE)) {
			if (keyCode == GLFW.GLFW_KEY_ENTER) {
				this.colorPickerWidget.confirmColor();
			} else {
				this.colorPickerWidget.cancel();
			}

			this.toggleColorPicker(false);
			this.setFocused(null);

			return true;
		} else {
			setFocused(null);
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				this.textBlockEntity.addRawLine(this.currentRow + 1,
					this.textBlockEntity.getRawLine(this.currentRow).substring(
						MathHelper.clamp(this.selectionManager.getSelectionStart(), 0, this.textBlockEntity.getRawLine(this.currentRow).length())
					));
				this.textBlockEntity.setRawLine(this.currentRow,
					this.textBlockEntity.getRawLine(this.currentRow).substring(0, MathHelper.clamp(this.selectionManager.getSelectionStart(), 0, this.textBlockEntity.getRawLine(this.currentRow).length())
					));
				this.textBlockEntity.renderDirty = true;
				++this.currentRow;
				this.selectionManager.moveCursorToStart();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_UP) {
				this.currentRow = Math.max(this.currentRow - 1, 0);
				this.selectionManager.putCursorAtEnd();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_DOWN) {
				this.currentRow = Math.min(this.currentRow + 1, (this.textBlockEntity.lines.size() - 1));
				this.selectionManager.putCursorAtEnd();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && this.currentRow > 0 && this.textBlockEntity.lines.size() > 1 && this.selectionManager.getSelectionStart() == 0 && this.selectionManager.getSelectionEnd() == this.selectionManager.getSelectionStart()) {
				--this.currentRow;
				this.selectionManager.putCursorAtEnd();
				deleteLine();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_DELETE && this.currentRow < this.textBlockEntity.lines.size() - 1 && this.selectionManager.getSelectionEnd() == this.textBlockEntity.getRawLine(this.currentRow).length()) {
				deleteLine();
				return true;
			} else {

				//formatting hotkeys
				if (Screen.hasControlDown()) {
					if (keyCode == GLFW.GLFW_KEY_B) {
						insertTag(TagRegistry.SAFE.getTag("bold"), true);
						return true;
					} else if (keyCode == GLFW.GLFW_KEY_I) {
						insertTag(TagRegistry.SAFE.getTag("italic"), true);
						return true;
					} else if (keyCode == GLFW.GLFW_KEY_U) {
						insertTag(TagRegistry.SAFE.getTag("underline"), true);
						return true;
					} else if (keyCode == GLFW.GLFW_KEY_5 || keyCode == GLFW.GLFW_KEY_S) {
						//There isn't a commonly agreed upon hotkey for strikethrough unlike the rest above
						//apparently 5 is commonly used for strikethrough ¯\_(ツ)_/¯
						//Google Docs and Microsoft Word have 5 in their hotkeys, while Discord has S in its hotkey
						insertTag(TagRegistry.SAFE.getTag("strikethrough"), true);
						return true;
					} else if (keyCode == GLFW.GLFW_KEY_O) {
						insertTag(TagRegistry.SAFE.getTag("obfuscated"), true);
						return true;
					}
				}

				try {
					boolean val = this.selectionManager.handleSpecialKey(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
					int selectionOffset = this.textBlockEntity.getRawLine(this.currentRow).length() - this.selectionManager.getSelectionStart();

					// Find line feed characters and create proper newlines
					for (int i = 0; i < this.textBlockEntity.lines.size(); ++i) {
						int lineFeedIndex = this.textBlockEntity.getRawLine(i).indexOf("\n");

						if (lineFeedIndex >= 0) {
							this.textBlockEntity.addRawLine(i + 1,
								this.textBlockEntity.getRawLine(i).substring(
									MathHelper.clamp(lineFeedIndex + 1, 0, this.textBlockEntity.getRawLine(i).length())
								));
							this.textBlockEntity.setRawLine(i,
								this.textBlockEntity.getRawLine(i).substring(0, MathHelper.clamp(lineFeedIndex, 0, this.textBlockEntity.getRawLine(i).length())
								));
							this.textBlockEntity.renderDirty = true;
							++this.currentRow;
							this.selectionManager.putCursorAtEnd();
							this.selectionManager.moveCursor(-selectionOffset);
						}
					}
					return val;
				} catch (StringIndexOutOfBoundsException e) {
					e.printStackTrace();
					MinecraftClient.getInstance().setScreen(null);
					return false;
				}
			}
		}
	}

	private void deleteLine() {
		this.textBlockEntity.setRawLine(this.currentRow,
			this.textBlockEntity.getRawLine(this.currentRow) + this.textBlockEntity.getRawLine(this.currentRow + 1)
		);

		this.textBlockEntity.lines.remove(this.currentRow + 1);
		this.textBlockEntity.renderDirty = true;
	}

	private void colorListenerClicked(TextFieldWidget textWidget) {
		this.colorPickerWidget.setPosition(Math.min(textWidget.getX(), width - colorPickerWidget.getWidth()), textWidget.getY() + textWidget.getHeight());
		this.colorPickerWidget.setTargetElement(textWidget);
		this.colorPickerWidget.setOnAccept(null);
		this.colorPickerWidget.setOnCancel(picker -> {
			picker.setColor(this.colorEntryPreColorPicker);
		});
		this.colorPickerWidget.setChangeListener(color -> {
			final int newColor = ColorUtil.transferAlpha(this.colorEntryPreColorPicker.getRGB(), color.getRGB());
			textWidget.setText(ColorUtil.toAlphaHex(newColor));
		});
		this.colorPickerWidget.setPresetListener((color, formatting) -> {
			this.colorPickerWidget.setColor(color);
		});
		ColorUtil.parse(textWidget.getText(), ColorUtil.WHITE).ifSuccess(color -> {
			final Color pickerColor = new Color(color);
			this.colorEntryPreColorPicker = pickerColor;
			this.colorPickerWidget.setColor(pickerColor);
		}).ifError(textColorError -> this.colorEntryPreColorPicker = this.colorPickerWidget.getCurrentColor());
		toggleColorPicker(true);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int topOffset = (int) (40 + 2 * this.width / 100F);

		for (final var text : textWidgets) {
			if (!text.mouseClicked(mouseX, mouseY, button)) {
				continue;
			}
			this.setFocused(text);
			if (this.colorListeners.contains(text)) {
				this.colorListenerClicked(text);
			}
			if (this.colorPickerWidget.targetElement != text || !this.colorPickerWidget.isMouseOver(mouseX, mouseY)) {
				text.setFocused(false);
			}
			break;
		}

		if (colorPickerWidget.active && colorPickerWidget.visible) {
			if (colorPickerWidget.isMouseOver(mouseX, mouseY)) {
				colorPickerWidget.mouseClicked(mouseX, mouseY, button);
				this.setFocused(colorPickerWidget);
				this.setDragging(true);
				return true;
			} else {
				if (!this.colorPickerWidget.targetElement.isMouseOver(mouseX, mouseY)) {
					toggleColorPicker(false);
				}
			}
		}
		if (mouseY > topOffset) {
			this.currentRow = MathHelper.clamp((int) (mouseY - topOffset) / 12, 0, this.textBlockEntity.lines.size() - 1);
			this.setFocused(null);
			String baseContents = this.textBlockEntity.getRawLine(currentRow);
			int baseContentsWidth = this.textRenderer.getWidth(baseContents);
			int contentsStart;
			int contentsEnd;
			switch (this.textBlockEntity.textAlignment) {
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
				this.selectionManager.moveCursorToStart();
			} else if (mouseX >= contentsEnd) {
				this.selectionManager.putCursorAtEnd();
			} else {
				int lastWidth = 0;
				for (int i = 1; i < baseContents.length(); i++) {
					String testContents = baseContents.substring(0, i);
					int width = this.textRenderer.getWidth(testContents);
					int midpointWidth = (width + lastWidth) / 2;
					if (mouseX < contentsStart + midpointWidth) {
						this.selectionManager.moveCursorTo(i - 1, false);
						break;
					} else if (mouseX <= contentsStart + width) {
						this.selectionManager.moveCursorTo(i, false);
						break;
					}
					lastWidth = width;
				}
			}
			return true;
		} else {
			return super.mouseClicked(mouseX, mouseY, button);
		}
	}

	@Override
	public ColorPickerWidget colorPickerWidget() {
		return this.colorPickerWidget;
	}

	@Override
	public void toggleColorPicker(boolean active) {
		this.colorPickerWidget.toggle(active);
	}

	@Override
	SelectionManager getSelectionManager() {
		return this.selectionManager;
	}
}
