package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Floats;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.ColorPickerWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.IconButtonWidget;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import dev.hephaestus.glowcase.packet.C2SEditTextBlock;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

//TODO: multi-character selection at some point? it may be a bit complex but it'd be nice
public class TextBlockEditScreen extends TextEditorScreen {
	private static final int innerPadding = 4;
	private final TextBlockEntity textBlockEntity;

	private List<EditBox> textWidgets;

	private List<EditBox> colorListeners;

	private TextFieldHelper selectionManager;
	private int currentRow;
	private long ticksSinceOpened = 0;
	private ColorPickerWidget colorPickerWidget;
	//	private Button changeAlignment;
	private EditBox colorEntryWidget;
	private EditBox backgroundColorEntryWidget;
	private Color colorEntryPreColorPicker; //used for color picker cancel button
	private Button zOffsetToggle;
	private Checkbox shadowToggle;

	private EditBox viewDistanceField;
	private Button viewDistanceHelpButton;

	public TextBlockEditScreen(TextBlockEntity textBlockEntity) {
		this.textBlockEntity = textBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		this.selectionManager = new TextFieldHelper(
			() -> this.textBlockEntity.getRawLine(this.currentRow),
			(string) -> {
				textBlockEntity.setRawLine(this.currentRow, string);
				this.textBlockEntity.renderDirty = true;
			},
			TextFieldHelper.createClipboardGetter(this.minecraft),
			TextFieldHelper.createClipboardSetter(this.minecraft),
			(string) -> true);

		int middle = width / 2;

		Button decreaseSize = Button.builder(Component.literal("-"), action -> {
			this.textBlockEntity.scale = Math.max(0, this.textBlockEntity.scale - (minecraft.hasShiftDown() ? 1F : 0.125F));
			this.textBlockEntity.renderDirty = true;
		}).bounds(middle - 130, 0, 20, 20).build();

		Button increaseSize = Button.builder(Component.literal("+"), action -> {
			this.textBlockEntity.scale += minecraft.hasShiftDown() ? 1F : 0.125F;
			this.textBlockEntity.renderDirty = true;
		}).bounds(middle - 110, 0, 20, 20).build();


		Map<TextBlockEntity.TextAlignment, Button> textAlignmentButtons = new HashMap<>();

		Consumer<TextBlockEntity.TextAlignment> setAlignment = (alignment) -> {
			var previous = textBlockEntity.textAlignment;
			var prevButton = textAlignmentButtons.get(previous);
			if (prevButton != null) {
				prevButton.active = true; // there are a few legacy values without a button
			}

			textBlockEntity.textAlignment = alignment;
			textBlockEntity.renderDirty = true;
		};

		var textAlignLeft = IconButtonWidget.builder(Glowcase.id("text_alignment/left"), button -> {
				setAlignment.accept(TextBlockEntity.TextAlignment.LEFT);
				button.active = false;
			})
			.position(middle - 90 + innerPadding, 0)
			.size(20, 20, 16, 16)
			.build();
		var textAlignCenter = IconButtonWidget.builder(Glowcase.id("text_alignment/center"), button -> {
				setAlignment.accept(TextBlockEntity.TextAlignment.CENTER);
				button.active = false;
			})
			.position(middle - 90 + innerPadding + 20, 0)
			.size(20, 20, 16, 16)
			.build();
		var textAlignRight = IconButtonWidget.builder(Glowcase.id("text_alignment/right"), button -> {
				setAlignment.accept(TextBlockEntity.TextAlignment.RIGHT);
				button.active = false;
			})
			.position(middle - 90 + innerPadding + 40, 0)
			.size(20, 20, 16, 16)
			.build();

		textAlignmentButtons.put(TextBlockEntity.TextAlignment.LEFT, textAlignLeft);
		textAlignmentButtons.put(TextBlockEntity.TextAlignment.CENTER, textAlignCenter);
		textAlignmentButtons.put(TextBlockEntity.TextAlignment.RIGHT, textAlignRight);

		var initialTextAlignmentButton = textAlignmentButtons.get(textBlockEntity.textAlignment);
		if (initialTextAlignmentButton != null) { // there are a few legacy values without a button
			initialTextAlignmentButton.active = false;
		}

		this.addRenderableWidget(textAlignCenter);
		this.addRenderableWidget(textAlignLeft);
		this.addRenderableWidget(textAlignRight);

		this.shadowToggle = Checkbox.builder(Component.translatable("gui.glowcase.shadow"), this.font)
			.selected(this.textBlockEntity.shadow)
			.onValueChange((widget, checked) -> {
				this.textBlockEntity.shadow = checked;
				this.textBlockEntity.renderDirty = true;
			})
			.pos(middle - 90 + innerPadding, 20 + innerPadding).build();

		this.colorEntryWidget = new EditBox(this.minecraft.font, middle + 70 + innerPadding * 2, 0, 64, 20, Component.empty());
		this.colorEntryWidget.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.color")));
		this.colorEntryWidget.setValue(ColorUtil.toAlphaHex(this.textBlockEntity.color));
		this.colorEntryWidget.setResponder(string -> {
			ColorUtil.parse(this.colorEntryWidget.getValue(), this.textBlockEntity.color).ifSuccess(newColor -> {
				final int color = (Math.max(newColor >>> 24, 0x1A) << 24) | (newColor & ColorUtil.COLOR_MASK);

				this.textBlockEntity.color = color;
				// make sure it doesn't update from the color picker updating the text
				if (this.colorEntryWidget.isFocused()) {
					this.colorPickerWidget.setColor(new Color(color));
				}
				this.textBlockEntity.renderDirty = true;
			});
		});

		this.backgroundColorEntryWidget = new EditBox(this.minecraft.font, middle + 136 + innerPadding * 2, 0, 64, 20, Component.empty());
		this.backgroundColorEntryWidget.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.background_color")));
		this.backgroundColorEntryWidget.setValue(ColorUtil.toAlphaHex(this.textBlockEntity.backgroundColor));
		this.backgroundColorEntryWidget.setResponder(string -> {
			ColorUtil.parse(string, this.textBlockEntity.backgroundColor).ifSuccess(newColor -> {
				this.textBlockEntity.backgroundColor = newColor;
				if (this.colorEntryWidget.isFocused()) {
					this.colorPickerWidget.setColor(new Color(newColor));
				}
				this.textBlockEntity.renderDirty = true;
			});
		});

		this.zOffsetToggle = Button.builder(Component.literal(this.textBlockEntity.zOffset.name()), action -> {
			switch (textBlockEntity.zOffset) {
				case FRONT -> textBlockEntity.zOffset = TextBlockEntity.ZOffset.CENTER;
				case CENTER -> textBlockEntity.zOffset = TextBlockEntity.ZOffset.BACK;
				case BACK -> textBlockEntity.zOffset = TextBlockEntity.ZOffset.FRONT;
			}
			this.textBlockEntity.renderDirty = true;

			this.zOffsetToggle.setMessage(Component.literal(this.textBlockEntity.zOffset.name()));
		}).bounds(middle + 2, 20 + innerPadding, 72, 20).build();

		this.colorPickerWidget = ColorPickerWidget.builder(this, 216, 10).size(182, 104).build();
		this.colorPickerWidget.toggle(false); //start deactivated

		this.viewDistanceField = new EditBox(this.minecraft.font, middle - 203, 20 + innerPadding, 83 + innerPadding, 20, Component.empty());
		this.viewDistanceField.setValue(String.valueOf(this.textBlockEntity.viewDistance));
		this.viewDistanceField.setResponder(s -> {
			if (Floats.tryParse(s) instanceof Float parsed) {
				this.textBlockEntity.viewDistance = parsed;
			}
		});
		this.viewDistanceField.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.screen.text_edit.view_distance")));
		this.viewDistanceHelpButton = Button.builder(Component.literal("?"), action -> {
			})
			.bounds(middle - 115 + innerPadding + 5, 20 + innerPadding, 20, 20).build();
		this.viewDistanceHelpButton.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.screen.text_edit.view_distance")));

		this.addRenderableWidget(colorPickerWidget);
		this.addRenderableWidget(increaseSize);
		this.addRenderableWidget(decreaseSize);
//		this.addRenderableWidget(this.changeAlignment);
		this.addRenderableWidget(this.shadowToggle);
		this.addRenderableWidget(this.zOffsetToggle);
		this.addRenderableWidget(this.colorEntryWidget);
		this.addRenderableWidget(this.backgroundColorEntryWidget);

		this.addRenderableWidget(this.viewDistanceField);
		this.addRenderableWidget(this.viewDistanceHelpButton);

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
	public void onClose() {
		C2SEditTextBlock.of(textBlockEntity).send();
		super.onClose();
	}

	private boolean isFocusedTextActive() {
		final GuiEventListener focused = this.getFocused();
		if (focused instanceof EditBox text) {
			return text.canConsumeInput();
		}
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		graphics.pose().pushMatrix();
		graphics.pose().translate(0, 40 + 2 * this.width / 100F);
		for (int i = 0; i < this.textBlockEntity.lines.size(); ++i) {
			var text = this.currentRow == i ? Component.literal(this.textBlockEntity.getRawLine(i)) : this.textBlockEntity.lines.get(i);

			int lineWidth = this.font.width(text);
			switch (this.textBlockEntity.textAlignment) {
				case LEFT -> graphics.text(minecraft.font, text, this.width / 10, i * 12, this.textBlockEntity.color);
				case CENTER, CENTER_LEFT, CENTER_RIGHT ->
					graphics.text(minecraft.font, text, this.width / 2 - lineWidth / 2, i * 12, this.textBlockEntity.color);
				case RIGHT ->
					graphics.text(minecraft.font, text, this.width - this.width / 10 - lineWidth, i * 12, this.textBlockEntity.color);
			}
		}

		int caretStart = this.selectionManager.getCursorPos();
		int caretEnd = this.selectionManager.getSelectionPos();

		if (caretStart >= 0) {
			String line = this.textBlockEntity.getRawLine(this.currentRow);
			int selectionStart = Mth.clamp(Math.min(caretStart, caretEnd), 0, line.length());
			int selectionEnd = Mth.clamp(Math.max(caretStart, caretEnd), 0, line.length());

			String preSelection = line.substring(0, Mth.clamp(line.length(), 0, selectionStart));
			int startX = this.minecraft.font.width(preSelection);

			float push = switch (this.textBlockEntity.textAlignment) {
				case LEFT -> this.width / 10F;
				case CENTER, CENTER_LEFT, CENTER_RIGHT -> this.width / 2F - this.font.width(line) / 2F;
				case RIGHT -> this.width - this.width / 10F - this.font.width(line);
			};

			startX += (int) push;


			int caretStartY = this.currentRow * 12;
			if (this.ticksSinceOpened / 6 % 2 == 0 && !this.isFocusedTextActive()) {
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
		graphics.text(minecraft.font, Component.translatable("gui.glowcase.scale_value", this.textBlockEntity.scale), width / 2 - 203, 7, 0xFFFFFFFF);
		colorPickerWidget.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		for (final var element : this.textWidgets) {
			if (element.charTyped(event)) {
				return true;
			}
		}

		return this.selectionManager.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		var keyCode = event.key();
		if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
			for (final var element : this.textWidgets) {
				if (element.isFocused()) {
					return element.keyPressed(event);
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
						Mth.clamp(this.selectionManager.getCursorPos(), 0, this.textBlockEntity.getRawLine(this.currentRow).length())
					));
				this.textBlockEntity.setRawLine(this.currentRow,
					this.textBlockEntity.getRawLine(this.currentRow).substring(0, Mth.clamp(this.selectionManager.getCursorPos(), 0, this.textBlockEntity.getRawLine(this.currentRow).length())
					));
				this.textBlockEntity.renderDirty = true;
				++this.currentRow;
				this.selectionManager.setCursorToStart();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_UP) {
				this.currentRow = Math.max(this.currentRow - 1, 0);
				this.selectionManager.setCursorToEnd();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_DOWN) {
				this.currentRow = Math.min(this.currentRow + 1, (this.textBlockEntity.lines.size() - 1));
				this.selectionManager.setCursorToEnd();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && this.currentRow > 0 && this.textBlockEntity.lines.size() > 1 && this.selectionManager.getCursorPos() == 0 && this.selectionManager.getSelectionPos() == this.selectionManager.getCursorPos()) {
				--this.currentRow;
				this.selectionManager.setCursorToEnd();
				deleteLine();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_DELETE && this.currentRow < this.textBlockEntity.lines.size() - 1 && this.selectionManager.getSelectionPos() == this.textBlockEntity.getRawLine(this.currentRow).length()) {
				deleteLine();
				return true;
			} else {

				//formatting hotkeys
				if (event.hasControlDown()) {
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
					boolean val = this.selectionManager.keyPressed(event) || super.keyPressed(event);
					int selectionOffset = this.textBlockEntity.getRawLine(this.currentRow).length() - this.selectionManager.getCursorPos();

					// Find line feed characters and create proper newlines
					for (int i = 0; i < this.textBlockEntity.lines.size(); ++i) {
						int lineFeedIndex = this.textBlockEntity.getRawLine(i).indexOf("\n");

						if (lineFeedIndex >= 0) {
							this.textBlockEntity.addRawLine(i + 1,
								this.textBlockEntity.getRawLine(i).substring(
									Mth.clamp(lineFeedIndex + 1, 0, this.textBlockEntity.getRawLine(i).length())
								));
							this.textBlockEntity.setRawLine(i,
								this.textBlockEntity.getRawLine(i).substring(0, Mth.clamp(lineFeedIndex, 0, this.textBlockEntity.getRawLine(i).length())
								));
							this.textBlockEntity.renderDirty = true;
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
		this.textBlockEntity.setRawLine(this.currentRow,
			this.textBlockEntity.getRawLine(this.currentRow) + this.textBlockEntity.getRawLine(this.currentRow + 1)
		);

		this.textBlockEntity.lines.remove(this.currentRow + 1);
		this.textBlockEntity.renderDirty = true;
	}

	private void colorListenerClicked(EditBox textWidget) {
		this.colorPickerWidget.setPosition(Math.min(textWidget.getX(), width - colorPickerWidget.getWidth()), textWidget.getY() + textWidget.getHeight());
		this.colorPickerWidget.setTargetElement(textWidget);
		this.colorPickerWidget.setOnAccept(null);
		this.colorPickerWidget.setOnCancel(picker -> {
			picker.setColor(this.colorEntryPreColorPicker);
		});
		this.colorPickerWidget.setChangeListener(color -> {
			final int newColor = ColorUtil.transferAlpha(this.colorEntryPreColorPicker.getRGB(), color.getRGB());
			textWidget.setValue(ColorUtil.toAlphaHex(newColor));
		});
		this.colorPickerWidget.setPresetListener((color, formatting) -> {
			this.colorPickerWidget.setColor(color);
		});
		ColorUtil.parse(textWidget.getValue(), ColorUtil.WHITE).ifSuccess(color -> {
			final Color pickerColor = new Color(color);
			this.colorEntryPreColorPicker = pickerColor;
			this.colorPickerWidget.setColor(pickerColor);
		}).ifError(textColorError -> this.colorEntryPreColorPicker = this.colorPickerWidget.getCurrentColor());
		toggleColorPicker(true);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int topOffset = (int) (40 + 2 * this.width / 100F);

		for (final var text : textWidgets) {
			if (!text.mouseClicked(event, doubleClick)) {
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
				colorPickerWidget.mouseClicked(event, doubleClick);
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
			this.currentRow = Mth.clamp((int) (mouseY - topOffset) / 12, 0, this.textBlockEntity.lines.size() - 1);
			this.setFocused(null);
			String baseContents = this.textBlockEntity.getRawLine(currentRow);
			int baseContentsWidth = this.font.width(baseContents);
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
	public ColorPickerWidget colorPickerWidget() {
		return this.colorPickerWidget;
	}

	@Override
	public void toggleColorPicker(boolean active) {
		this.colorPickerWidget.toggle(active);
	}

	@Override
	TextFieldHelper getSelectionManager() {
		return this.selectionManager;
	}
}
