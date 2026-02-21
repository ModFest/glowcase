package dev.hephaestus.glowcase.client.gui.widget.ingame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hephaestus.glowcase.util.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

public class SuggestionListWidget<T> extends AbstractWidget {
	public static final Identifier BLUR_ID = Identifier.withDefaultNamespace("blur");
	public static final RenderTarget FRAMEBUFFER = new TextureTarget("Glowcase Suggestions", 1, 1, false);
	public static final CrossFrameResourcePool POOL = new CrossFrameResourcePool(3);
	private final Font textRenderer;
	private final Minecraft client;

	private final List<T> suggestions = new ArrayList<>();
	private int selectedItem = -1;
	private final @Nullable EditBox textFieldWidget;
	private @NotNull String filter = "";
	private int scrollOffset = 0;

	private final int baseLineHeight;
	private final int padding;
	private final int maxRows;
	/**
	 * The approximate maximum number of characters that'll fit inside the width of this widget
	 */
	private int characterWidth;

	private final Consumer<T> onSelect;
	private final Function<T, String> toStringFunction;

	public boolean draggingScrollbar = false;
	private int scrollbarDragStartY = 0;
	private int initialScrollOffset = 0;

	public SuggestionListWidget(@Nullable EditBox widget, Font textRenderer, int x, int y, int width, int height, int baseLineHeight, int padding, int maxRows, Consumer<T> onSelect, Function<T, String> toStringFunction) {
		super(x, y, width, height, Component.empty());

		this.textFieldWidget = widget;

		this.client = Minecraft.getInstance();

		this.baseLineHeight = baseLineHeight;
		this.padding = padding;
		this.maxRows = maxRows;
		this.onSelect = onSelect;
		this.toStringFunction = toStringFunction;
		this.textRenderer = textRenderer;
		this.characterWidth = 1;
		setWidth(width);
	}

	public static <T> SuggestionListWidget<T> forTextField(EditBox textField, Font textRenderer, Function<T, String> toStringFunction) {
		return new SuggestionListWidget<>(
			textField, textRenderer,
			textField.getX(),
			textField.getY() + textField.getHeight() + 5, textField.getWidth(),
			100, 10, 4, 5,
			a -> textField.setValue(toStringFunction.apply(a)),
			toStringFunction
		);
	}

	public static <T> SuggestionListWidget<T> forTextFieldWithStaticSuggestions(EditBox textField, Font textRenderer, List<T> suggestions, Function<T, String> toStringFunction, @Nullable ContainerEventHandler parent) {
		var suggestionWidget = forTextField(textField, textRenderer, toStringFunction);
		textField.setResponder((text) -> suggestionWidget.updateSuggestions(suggestions, text, parent));
		return suggestionWidget;
	}

	@Override
	public void setWidth(int width) {
		super.setWidth(width);
		while (textRenderer.width("m".repeat(characterWidth)) < this.width) {
			characterWidth++;
		}
	}

	public void updateSuggestions(List<T> newSuggestions, String filter, @Nullable ContainerEventHandler parent) {
		updateSuggestions(newSuggestions, filter, true, parent);
	}

	// update the suggestion list based on filter
	public void updateSuggestions(List<T> newSuggestions, String filter, boolean strict, @Nullable ContainerEventHandler parent) {
		suggestions.clear();
		this.filter = filter;

		for (T suggestion : newSuggestions) {
			if (suggestion == null) {
				throw new NullPointerException("A suggestion can not be null!");
			}
			String text = toStringFunction.apply(suggestion);

			if (strict ? text.startsWith(filter) : text.contains(filter)) {
				suggestions.add(suggestion);
			}
		}

		// if there is only 1 suggestion & it's equal to input, hide the list
		if (suggestions.size() == 1 && toStringFunction.apply(suggestions.getFirst()).equals(filter)) {
			suggestions.clear();
		}

		scrollOffset = 0;

		if (suggestions.isEmpty()) {
			FRAMEBUFFER.resize(1, 1);
		}

		this.setFocused(!suggestions.isEmpty());
	}

	@Override
	public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
		if (suggestions.isEmpty()) return;

		if (textFieldWidget != null && !textFieldWidget.isFocused()) {
			this.suggestions.clear();
			this.setFocused(false);
		}

		context.nextStratum();
		context.guiRenderState.submitGuiElement(new GuiElementRenderState() {
			@Override
			public void buildVertices(VertexConsumer vertices) {
				Matrix3x2fStack matrix = context.pose();
				vertices.addVertexWith2DPose(matrix, 0, 0).setUv(0, 0).setColor(0xFFFFFFFF);
				vertices.addVertexWith2DPose(matrix, 0, 1).setUv(0, 1).setColor(0xFFFFFFFF);
				vertices.addVertexWith2DPose(matrix, 1, 1).setUv(1, 1).setColor(0xFFFFFFFF);
				vertices.addVertexWith2DPose(matrix, 1, 0).setUv(1, 0).setColor(0xFFFFFFFF);
			}

			@Override
			public RenderPipeline pipeline() {
				return RenderPipelines.MOJANG_LOGO;
			}

			@Override
			public TextureSetup textureSetup() {
				return TextureSetup.singleTexture(FRAMEBUFFER.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			}

			@Override
			public @Nullable ScreenRectangle scissorArea() {
				return null;
			}

			@Override
			public @Nullable ScreenRectangle bounds() {
				return ScreenRectangle.empty();
			}
		});

		context.nextStratum();
		context.pose().pushMatrix();

		int bgColor = 0x90000000;
		int adjustedLineHeight = baseLineHeight + padding * 2;
		int rows = Math.min(suggestions.size(), maxRows);
		int dynamicHeight = rows * adjustedLineHeight;

		boolean scrollable = suggestions.size() > maxRows;
		int totalLines = suggestions.size();

		int listWidth = scrollable ? this.getWidth() - 5 - 10 : this.getWidth();

		int x = SuggestionListWidget.this.getX();
		int y = SuggestionListWidget.this.getY();
		context.enableScissor(x, y, x + listWidth, y + dynamicHeight);

		context.submitBlit(RenderPipelines.GUI_TEXTURED, FRAMEBUFFER.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST),0, 0, FRAMEBUFFER.width / this.client.getWindow().getGuiScale(), FRAMEBUFFER.height / this.client.getWindow().getGuiScale(), 0, 1, 0, 1, -1);

		context.fill(x, y, x + listWidth, y + dynamicHeight, bgColor);

		drawOutline(context, x, y, listWidth, dynamicHeight, 0xFFFFFFFF);

		if (scrollOffset > totalLines - rows) {
			scrollOffset = Math.max(0, totalLines - rows);
		}

		// render each suggestion
		for (int i = 0; i < rows; i++) {
			int suggestionIndex = i + scrollOffset;
			if (suggestionIndex >= totalLines) break;

			T suggestion = suggestions.get(suggestionIndex);
			String suggestionText = toStringFunction.apply(suggestion);
			int suggestionY = y + i * adjustedLineHeight;

			// highlight hovered suggestion
			boolean hover = mouseX >= x && mouseX <= x + listWidth && mouseY >= suggestionY && mouseY < suggestionY + adjustedLineHeight;
			if (hover || suggestionIndex == selectedItem) {
				context.fill(x, suggestionY, x + listWidth, suggestionY + adjustedLineHeight, 0xFF217C08);
				drawOutline(context, x, suggestionY, listWidth, adjustedLineHeight, 0xFFFFFFFF);
			}

			// detect if the text is too long AND if the item is hovered, then scroll, otherwise don't
			if (textRenderer.width(suggestionText) > (this.getWidth() - padding - 20)) {
				drawOverflowText(context, textRenderer, Component.literal(suggestionText), x + padding, suggestionY + padding - 2, x + listWidth - padding, suggestionY + adjustedLineHeight, 0xFFFFFFFF, hover);
			} else {
				context.drawString(textRenderer, Component.literal(suggestionText), x + padding, suggestionY + padding + 1, 0xFFFFFFFF);
			}
		}

		context.disableScissor();

		// scrollbar thingy
		if (scrollable) {
			int scrollbarWidth = 10;

			int sbX = x + listWidth + 5;

			context.enableScissor(sbX, y, sbX + scrollbarWidth, y + dynamicHeight);

			context.submitBlit(RenderPipelines.GUI_TEXTURED, FRAMEBUFFER.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST), 0, 0, FRAMEBUFFER.width / this.client.getWindow().getGuiScale(), FRAMEBUFFER.height / this.client.getWindow().getGuiScale(), 0, 1, 0, 1, -1);

			context.fill(sbX, y, sbX + scrollbarWidth, y + dynamicHeight, bgColor);
			context.disableScissor();

			drawOutline(context, sbX, y, scrollbarWidth, dynamicHeight, 0xFFFFFFFF);

			float visibleRatio = (float) rows / totalLines;
			int handleHeight = Math.max((int) (visibleRatio * (dynamicHeight - 2 * 2)), 4);

			int availableScroll = totalLines - rows;
			int handleYOffset = availableScroll > 0 ? (int) (((float) scrollOffset / availableScroll) * ((dynamicHeight - 2 * 2) - handleHeight)) : 0;
			int handleX = sbX + 2;
			int handleY = y + 2 + handleYOffset;
			int handleWidth = scrollbarWidth - 2 * 2;

			context.fill(handleX, handleY, handleX + handleWidth, handleY + handleHeight, 0xFFFFFFFF);
		}

		context.pose().popMatrix();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		var mouseX = event.x();
		var mouseY = event.y();

		boolean scrollable = suggestions.size() > maxRows;

		int adjustedLineHeight = baseLineHeight + padding * 2;
		int listWidth = scrollable ? this.getWidth() - 5 - 10 : this.getWidth();

		int relativeY = (int) mouseY - this.getY();
		int clickedIndex = relativeY / adjustedLineHeight + scrollOffset;

		if (scrollable) {
			int sbX = getX() + listWidth + 5;
			int sbY = getY();
			int scrollbarHeight = Math.min(suggestions.size(), maxRows) * adjustedLineHeight;

			if (mouseX >= sbX && mouseX <= sbX + 10 && mouseY >= sbY && mouseY <= sbY + scrollbarHeight) {
				draggingScrollbar = true;

				scrollbarDragStartY = (int) mouseY;
				initialScrollOffset = scrollOffset;

				return true;
			}
		}

		if (mouseX >= this.getX() && mouseX <= this.getX() + listWidth) {
			if (clickedIndex >= 0 && clickedIndex < suggestions.size()) {
				onSelect.accept(suggestions.get(clickedIndex));
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		var mouseX = event.x();
		var mouseY = event.y();

		if (draggingScrollbar) {
			int adjustedLineHeight = baseLineHeight + padding * 2;
			int rows = Math.min(suggestions.size(), maxRows);

			int dynamicHeight = rows * adjustedLineHeight;
			int totalLines = suggestions.size();
			int availableScroll = totalLines - maxRows;

			float visibleRatio = (float) maxRows / totalLines;
			int handleHeight = Math.max((int) (visibleRatio * (dynamicHeight - 2 * 2)), 4);

			int dragDelta = (int) (mouseY - scrollbarDragStartY);

			if ((dynamicHeight - 2 * 2) - handleHeight > 0) {
				int newOffset = initialScrollOffset + (int) ((float) dragDelta / ((dynamicHeight - 2 * 2) - handleHeight) * availableScroll);
				scrollOffset = Math.max(0, Math.min(newOffset, availableScroll));
			}

			return true;
		}

		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		draggingScrollbar = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int rows = Math.min(suggestions.size(), maxRows);

		int totalLines = suggestions.size();
		int maxLines = rows;
		scrollOffset -= (int) verticalAmount;

		if (scrollOffset < 0) scrollOffset = 0;
		if (scrollOffset > totalLines - maxLines) scrollOffset = Math.max(0, totalLines - maxLines);

		return true;
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		int adjustedLineHeight = baseLineHeight + padding * 2;
		int rows = Math.min(suggestions.size(), maxRows);
		int dynamicHeight = rows * adjustedLineHeight;

		boolean scrollable = suggestions.size() > maxRows;
		int listWidth = scrollable ? this.getWidth() - 5 - 10 : this.getWidth();

		boolean overList = (mouseX >= this.getX() && mouseX <= this.getX() + listWidth && mouseY >= this.getY() && mouseY < this.getY() + dynamicHeight);
		boolean overScrollbar = false;

		if (scrollable) {
			int sbX = this.getX() + listWidth + 5;
			int sbY = this.getY();

			overScrollbar = (mouseX >= sbX && mouseX <= sbX + 10 && mouseY >= sbY && mouseY <= sbY + dynamicHeight);
		}

		return overList || overScrollbar;
	}

	private void drawOutline(GuiGraphics context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width - 1, y, x + width, y + height, color);
	}

	// similar to drawScrollableText but not centered
	private void drawOverflowText(GuiGraphics context, Font textRenderer, Component text, int startX, int startY, int endX, int endY, int color, boolean hovered) {
		int textRendererWidth = textRenderer.width(text);
		int availableWidth = endX - startX;
		int y = startY + ((endY - startY) - 9) / 2;

		// if hovered, we scroll
		if (hovered) {
			int extra = textRendererWidth - availableWidth;
			double time = Util.getMillis() / 1000.0;
			double period = Math.max(extra / 8.0, 2.0);
			double scroll = 0.5 - 0.5 * Math.cos(2 * Math.PI * time / period);
			int offset = (int) (scroll * extra);

			context.enableScissor(startX, startY, endX, endY);
			context.drawString(textRenderer, text, startX - offset, y, color);
			context.disableScissor();
		} else {
			// otherwise try to shorten the text as much as possible
			String rawText = text.getString();

			String collapsedText;
			int colonIndex = rawText.indexOf(':');

			// if no colon, prob nothing to collapse
			if (colonIndex == -1) {
				collapsedText = rawText;
			} else {
				int lastSlashIndex = rawText.lastIndexOf('/');

				// if no slash after colon, leave as-is
				if (lastSlashIndex == -1 || lastSlashIndex < colonIndex) {
					collapsedText = rawText;
				} else {
					String namespace = rawText.substring(0, colonIndex + 1);
					String lastPart = rawText.substring(lastSlashIndex + 1);

					collapsedText = namespace + ".../" + lastPart;
				}
			}

			String finalText;
			if (filter.trim().isEmpty()) {
				finalText = collapsedText;
			} else if (filter.length() >= (rawText.indexOf(':') + 1)) {
				if (rawText.lastIndexOf('/') == -1) {
					//... and no slash, put ... before
					finalText = "..." + collapsedText.substring(rawText.indexOf(':') + 1);
				} else {
					// no namespace
					finalText = collapsedText.substring(rawText.indexOf(':') + 1);
				}
			} else if (filter.length() > 1) {
				int removeCount = Math.min(filter.length(), collapsedText.length());
				finalText = "..." + collapsedText.substring(removeCount);
			} else {
				finalText = collapsedText;
			}

			if (filter.trim().isEmpty()) {
				int slashIndex = collapsedText.lastIndexOf("/");

				if (slashIndex != -1) {
					String prefix = collapsedText.substring(0, slashIndex + 1);
					String lastPart = collapsedText.substring(slashIndex + 1);

					if (textRenderer.width(collapsedText) > availableWidth) {
						int prefixWidth = textRenderer.width(prefix);
						int allowedForLast = availableWidth - prefixWidth;

						if (allowedForLast < 0) {
							finalText = trimToWidth(collapsedText, availableWidth, textRenderer);
						} else {
							if (textRenderer.width(lastPart) > allowedForLast) {
								lastPart = trimToWidth(lastPart, allowedForLast, textRenderer);
							}

							finalText = prefix + lastPart;
						}
					}
				} else {
					finalText = trimToWidth(collapsedText, availableWidth, textRenderer);
				}
			} else {
				if (textRenderer.width(finalText) > availableWidth) {
					finalText = trimToWidth(finalText, availableWidth, textRenderer);
				}
			}

			context.enableScissor(startX, startY, endX, endY);
			context.drawString(textRenderer, Component.literal(finalText), startX, y, color);
			context.disableScissor();
		}
	}

	private String trimToWidth(String rawText, int availableWidth, Font textRenderer) {
		if (textRenderer.width(rawText) <= availableWidth) {
			return rawText;
		}

		int maxWidth = availableWidth - textRenderer.width("...");
		int trimIndex = rawText.length();

		while (trimIndex > 0 && textRenderer.width(rawText.substring(0, trimIndex)) > maxWidth) {
			trimIndex--;
		}

		return rawText.substring(0, trimIndex) + "...";
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (suggestions.isEmpty()) {
			return false;
		}

		int rows = Math.min(suggestions.size(), maxRows);

		var keyCode = event.key();
		boolean affected = switch (keyCode) {
			case GLFW.GLFW_KEY_UP -> {
				if (this.selectedItem == -1) this.selectedItem = 0;
				this.selectedItem--;
				yield true;
			}
			case GLFW.GLFW_KEY_DOWN -> {
				this.selectedItem++;
				yield true;
			}
			case GLFW.GLFW_KEY_PAGE_UP -> {
				if (this.selectedItem == -1) this.selectedItem = 0;
				this.selectedItem = Math.max(this.selectedItem - rows, 0);
				yield true;
			}
			case GLFW.GLFW_KEY_PAGE_DOWN -> {
				if (this.selectedItem == -1) this.selectedItem = 0;
				this.selectedItem = Math.min(this.selectedItem + rows, suggestions.size() - 1);
				yield true;
			}
			case GLFW.GLFW_KEY_ENTER -> {
				if (this.selectedItem == -1) yield false;
				onSelect.accept(suggestions.get(selectedItem));
				yield true;
			}
			default -> false;
		};

		if (affected) {
			if (!this.suggestions.isEmpty()) {
				this.selectedItem = MathUtils.clampWrap(this.selectedItem, 0, this.suggestions.size() - 1);
				this.scrollOffset = Math.max(this.selectedItem - rows + 1, 0);
			}
			return true;
		}

		if (textFieldWidget != null) {
			return textFieldWidget.keyPressed(event);
		}

		return super.keyPressed(event);
	}

	@Override
	public void setFocused(boolean focused) {
		if (!focused) {
			this.selectedItem = -1;
		}

		super.setFocused(focused);
	}
}
