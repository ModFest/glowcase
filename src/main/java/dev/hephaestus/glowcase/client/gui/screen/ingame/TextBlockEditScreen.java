package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.ColorPickerWidget;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import dev.hephaestus.glowcase.packet.C2SEditTextBlock;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
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
import java.util.List;

//TODO: multi-character selection at some point? it may be a bit complex but it'd be nice
public class TextBlockEditScreen extends TextEditorScreen {
	private static final int innerPadding = 4;
	private final TextBlockEntity textBlockEntity;

	private List<EditBox> textWidgets;
	private List<EditBox> colorListeners;

	private TextFieldHelper selectionManager;
	private EditBox colorEntryWidget;
	private int currentRow;
	private long ticksSinceOpened = 0;
	private ColorPickerWidget colorPickerWidget;
	private Color colorEntryPreColorPicker; //used for color picker cancel button

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

		var scaleSlider = new TextScaleSliderWidget(textBlockEntity, middle - 203, 0, 113, 20);
		this.addRenderableWidget(scaleSlider);

		var moreOptionsButton = Button.builder(
				Component.translatable("gui.glowcase.more"),
				button -> {
					var optionsScreen = new TextBlockOptionsScreen(this, textBlockEntity);
					Minecraft.getInstance().setScreen(optionsScreen);
				})
			.bounds(middle + 124, 0, 80, 20)
			.build();
		this.addRenderableWidget(moreOptionsButton);

		this.colorEntryWidget = new EditBox(this.minecraft.font, middle + 54, 0, 64, 20, Component.empty());
		this.colorEntryWidget.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.color")));
		this.colorEntryWidget.setValue(ColorUtil.toAlphaHex(this.textBlockEntity.color));
		this.colorEntryWidget.setResponder(string -> {
			ColorUtil.parse(string, this.textBlockEntity.color).ifSuccess(newColor -> {
				final int color = (Math.max(newColor >>> 24, 0x1A) << 24) | (newColor & ColorUtil.COLOR_MASK);

				this.textBlockEntity.color = color;
				// make sure it doesn't update from the color picker updating the text
				if (this.colorEntryWidget.isFocused()) {
					this.colorPickerWidget.setColor(new Color(color));
				}
				this.textBlockEntity.renderDirty = true;
			});
		});

		this.colorPickerWidget = ColorPickerWidget.builder(this, 216, 10).size(182, 104).build();
		this.colorPickerWidget.toggle(false); //start deactivated

		this.addRenderableWidget(colorPickerWidget);
		this.addRenderableWidget(this.colorEntryWidget);

		this.textWidgets = List.of(
			this.colorEntryWidget
		);

		this.colorListeners = List.of(
			this.colorEntryWidget
		);

		addFormattingButtons(middle - 90 + 6, 0, 0, 20, 2);
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
		graphics.pose().translate(0, 20 + 2 * this.width / 100F);
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

	public static class TextScaleSliderWidget extends AbstractSliderButton {
		private static final float MIN_SCALE = 0.125F;
		private static final float MAX_SCALE = 16;

		private final TextBlockEntity entity;

		public TextScaleSliderWidget(TextBlockEntity entity, int x, int y, int width, int height) {
			var initialValue = (entity.scale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE);
			super(x, y, width, height, Component.translatable("gui.glowcase.scale_value", entity.scale), initialValue);
			this.entity = entity;
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.translatable("gui.glowcase.scale_value", entity.scale));
		}

		@Override
		protected void applyValue() {
			entity.scale = (float) Math.round(Mth.lerp(this.value, MIN_SCALE, MAX_SCALE) * 8F) / 8F;
			entity.renderDirty = true;
		}
	}
}
