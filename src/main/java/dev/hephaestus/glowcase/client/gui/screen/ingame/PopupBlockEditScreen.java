package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
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
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

//TODO: multi-character selection at some point? it may be a bit complex but it'd be nice
public class PopupBlockEditScreen extends GlowcaseScreen {
	private final PopupBlockEntity popupBlockEntity;

	private TextFieldHelper selectionManager;
	private int currentRow;
	private long ticksSinceOpened = 0;
	private EditBox titleEntryWidget;
	private Button changeAlignment;
	private EditBox colorEntryWidget;

	public PopupBlockEditScreen(PopupBlockEntity popupBlockEntity) {
		this.popupBlockEntity = popupBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		int innerPadding = width / 100;

		this.selectionManager = new TextFieldHelper(
			() -> this.popupBlockEntity.getRawLine(this.currentRow),
			(string) -> {
				popupBlockEntity.setRawLine(this.currentRow, string);
				this.popupBlockEntity.renderDirty = true;
			},
			TextFieldHelper.createClipboardGetter(this.minecraft),
			TextFieldHelper.createClipboardSetter(this.minecraft),
			(string) -> true);

		this.titleEntryWidget = new EditBox(this.minecraft.font, width / 10, 0, 8 * width / 10, 20, Component.empty());
		this.titleEntryWidget.setMaxLength(HyperlinkBlockEntity.TITLE_MAX_LENGTH);
		this.titleEntryWidget.setValue(this.popupBlockEntity.title);
		this.titleEntryWidget.setHint(TextUtils.placeholder("gui.glowcase.title"));
		this.titleEntryWidget.setResponder(string -> {
			this.popupBlockEntity.title = this.titleEntryWidget.getValue();
			this.popupBlockEntity.renderDirty = true;
		});

		this.changeAlignment = Button.builder(Component.translatableEscape("gui.glowcase.alignment", this.popupBlockEntity.textAlignment), action -> {
			switch (popupBlockEntity.textAlignment) {
				case LEFT -> popupBlockEntity.textAlignment = TextBlockEntity.TextAlignment.CENTER;
				case CENTER, CENTER_LEFT, CENTER_RIGHT -> popupBlockEntity.textAlignment = TextBlockEntity.TextAlignment.RIGHT;
				case RIGHT -> popupBlockEntity.textAlignment = TextBlockEntity.TextAlignment.LEFT;
			}
			this.popupBlockEntity.renderDirty = true;

			this.changeAlignment.setMessage(Component.translatableEscape("gui.glowcase.alignment", this.popupBlockEntity.textAlignment));
		}).bounds(120 + innerPadding, 20 + innerPadding, 160, 20).build();

		this.colorEntryWidget = new EditBox(this.minecraft.font, 280 + innerPadding * 2, 20 + innerPadding, 50, 20, Component.empty());
		this.colorEntryWidget.setValue("#" + Integer.toHexString(this.popupBlockEntity.color & 0x00FFFFFF));
		this.colorEntryWidget.setResponder(string -> {
			TextColor.parseColor(this.colorEntryWidget.getValue()).ifSuccess(color -> {
				this.popupBlockEntity.color = color == null ? 0xFFFFFFFF : color.getValue() | 0xFF000000;
				this.popupBlockEntity.renderDirty = true;
			});
		});

		this.addRenderableWidget(this.titleEntryWidget);
		this.addRenderableWidget(this.changeAlignment);
		this.addRenderableWidget(this.colorEntryWidget);
	}

	@Override
	public void tick() {
		++this.ticksSinceOpened;
	}

	@Override
	public void onClose() {
		C2SEditPopupBlock.of(popupBlockEntity).send();
		super.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		graphics.pose().pushMatrix();
		graphics.pose().translate(0, 40 + 2 * this.width / 100F);
		for (int i = 0; i < this.popupBlockEntity.lines.size(); ++i) {
			var text = this.currentRow == i ? Component.literal(this.popupBlockEntity.getRawLine(i)) : this.popupBlockEntity.lines.get(i);

			int lineWidth = this.font.width(text);
			switch (this.popupBlockEntity.textAlignment) {
				case LEFT -> graphics.text(minecraft.font, text, this.width / 10, i * 12, this.popupBlockEntity.color);
				case CENTER, CENTER_LEFT, CENTER_RIGHT -> graphics.text(minecraft.font, text, this.width / 2 - lineWidth / 2, i * 12, this.popupBlockEntity.color);
				case RIGHT -> graphics.text(minecraft.font, text, this.width - this.width / 10 - lineWidth, i * 12, this.popupBlockEntity.color);
			}
		}

		int caretStart = this.selectionManager.getCursorPos();
		int caretEnd = this.selectionManager.getSelectionPos();

		if (caretStart >= 0) {
			String line = this.popupBlockEntity.getRawLine(this.currentRow);
			int selectionStart = Mth.clamp(Math.min(caretStart, caretEnd), 0, line.length());
			int selectionEnd = Mth.clamp(Math.max(caretStart, caretEnd), 0, line.length());

			String preSelection = line.substring(0, Mth.clamp(line.length(), 0, selectionStart));
			int startX = this.minecraft.font.width(preSelection);

			float push = switch (this.popupBlockEntity.textAlignment) {
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
				this.popupBlockEntity.addRawLine(this.currentRow + 1,
					this.popupBlockEntity.getRawLine(this.currentRow).substring(
						Mth.clamp(this.selectionManager.getCursorPos(), 0, this.popupBlockEntity.getRawLine(this.currentRow).length())
					));
				this.popupBlockEntity.setRawLine(this.currentRow,
					this.popupBlockEntity.getRawLine(this.currentRow).substring(0, Mth.clamp(this.selectionManager.getCursorPos(), 0, this.popupBlockEntity.getRawLine(this.currentRow).length())
					));
				this.popupBlockEntity.renderDirty = true;
				++this.currentRow;
				this.selectionManager.setCursorToStart();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_UP) {
				this.currentRow = Math.max(this.currentRow - 1, 0);
				this.selectionManager.setCursorToEnd();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_DOWN) {
				this.currentRow = Math.min(this.currentRow + 1, (this.popupBlockEntity.lines.size() - 1));
				this.selectionManager.setCursorToEnd();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && this.currentRow > 0 && this.popupBlockEntity.lines.size() > 1 && this.selectionManager.getCursorPos() == 0 && this.selectionManager.getSelectionPos() == this.selectionManager.getCursorPos()) {
				--this.currentRow;
				this.selectionManager.setCursorToEnd();
				deleteLine();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_DELETE && this.currentRow < this.popupBlockEntity.lines.size() - 1 && this.selectionManager.getSelectionPos() == this.popupBlockEntity.getRawLine(this.currentRow).length()) {
				deleteLine();
				return true;
			} else {
				try {
					boolean val = this.selectionManager.keyPressed(event) || super.keyPressed(event);
					int selectionOffset = this.popupBlockEntity.getRawLine(this.currentRow).length() - this.selectionManager.getCursorPos();

					// Find line feed characters and create proper newlines
					for (int i = 0; i < this.popupBlockEntity.lines.size(); ++i) {
						int lineFeedIndex = this.popupBlockEntity.getRawLine(i).indexOf("\n");

						if (lineFeedIndex >= 0) {
							this.popupBlockEntity.addRawLine(i + 1,
								this.popupBlockEntity.getRawLine(i).substring(
									Mth.clamp(lineFeedIndex + 1, 0, this.popupBlockEntity.getRawLine(i).length())
								));
							this.popupBlockEntity.setRawLine(i,
								this.popupBlockEntity.getRawLine(i).substring(0, Mth.clamp(lineFeedIndex, 0, this.popupBlockEntity.getRawLine(i).length())
								));
							this.popupBlockEntity.renderDirty = true;
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
		this.popupBlockEntity.setRawLine(this.currentRow,
			this.popupBlockEntity.getRawLine(this.currentRow) + this.popupBlockEntity.getRawLine(this.currentRow + 1)
		);

		this.popupBlockEntity.lines.remove(this.currentRow + 1);
		this.popupBlockEntity.renderDirty = true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int topOffset = (int) (40 + 2 * this.width / 100F);
		if (!this.titleEntryWidget.mouseClicked(event, doubleClick)) {
			this.titleEntryWidget.setFocused(false);
		}
		if (!this.colorEntryWidget.mouseClicked(event, doubleClick)) {
			this.colorEntryWidget.setFocused(false);
		}
		if (mouseY > topOffset) {
			this.currentRow = Mth.clamp((int) (mouseY - topOffset) / 12, 0, this.popupBlockEntity.lines.size() - 1);
			this.setFocused(null);
			String baseContents = this.popupBlockEntity.getRawLine(currentRow);
			int baseContentsWidth = this.font.width(baseContents);
			int contentsStart;
			int contentsEnd;
			switch (this.popupBlockEntity.textAlignment) {
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
}
