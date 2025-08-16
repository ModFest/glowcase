package dev.hephaestus.glowcase.client.gui.widget.ingame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.hephaestus.glowcase.util.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.Pool;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

public class SuggestionListWidget<T> extends ClickableWidget {
	public static final Identifier BLUR_ID = Identifier.ofVanilla("blur");
	public static final Framebuffer FRAMEBUFFER = new SimpleFramebuffer("Glowcase Suggestions", 1, 1, false);
	public static final Pool POOL = new Pool(3);
	private final TextRenderer textRenderer;
	private final MinecraftClient client;

	private final List<T> suggestions = new ArrayList<>();
	private int selectedItem = -1;
	private final @Nullable TextFieldWidget textFieldWidget;
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

    public SuggestionListWidget(@Nullable TextFieldWidget widget, TextRenderer textRenderer, int x, int y, int width, int height, int baseLineHeight, int padding, int maxRows, Consumer<T> onSelect, Function<T, String> toStringFunction) {
        super(x, y, width, height, Text.empty());

		this.textFieldWidget = widget;

		this.client = MinecraftClient.getInstance();

        this.baseLineHeight = baseLineHeight;
        this.padding = padding;
        this.maxRows = maxRows;
        this.onSelect = onSelect;
        this.toStringFunction = toStringFunction;
        this.textRenderer = textRenderer;
		this.characterWidth = 1;
		setWidth(width);
    }

	public static <T> SuggestionListWidget<T> forTextField(TextFieldWidget textField, TextRenderer textRenderer, Function<T, String> toStringFunction) {
		return new SuggestionListWidget<>(
			textField, textRenderer,
			textField.getX(),
			textField.getY() + textField.getHeight() + 5, textField.getWidth(),
			100, 10, 4, 5,
			a -> textField.setText(toStringFunction.apply(a)),
			toStringFunction
		);
	}

	public static <T> SuggestionListWidget<T> forTextFieldWithStaticSuggestions(TextFieldWidget textField, TextRenderer textRenderer, List<T> suggestions, Function<T, String> toStringFunction, @Nullable ParentElement parent) {
		var suggestionWidget = forTextField(textField, textRenderer, toStringFunction);
		textField.setChangedListener((text) -> suggestionWidget.updateSuggestions(suggestions, text, parent));
		return suggestionWidget;
	}

	@Override
	public void setWidth(int width) {
		super.setWidth(width);
		while (textRenderer.getWidth("m".repeat(characterWidth)) < this.width) {
			characterWidth++;
		}
	}

	public void updateSuggestions(List<T> newSuggestions, String filter, @Nullable ParentElement parent) {
		updateSuggestions(newSuggestions, filter, true, parent);
	}

	// update the suggestion list based on filter
    public void updateSuggestions(List<T> newSuggestions, String filter, boolean strict, @Nullable ParentElement parent) {
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
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (suggestions.isEmpty()) return;

		if (textFieldWidget != null && !textFieldWidget.isFocused()) {
			this.suggestions.clear();
			this.setFocused(false);
		}

		context.createNewRootLayer();
		context.state.addSimpleElement(new SimpleGuiElementRenderState() {
			@Override
			public void setupVertices(VertexConsumer vertices, float depth) {
				Matrix3x2fStack matrix = context.getMatrices();
				vertices.vertex(matrix, 0, 0, depth).texture(0, 0).color(0xFFFFFFFF);
				vertices.vertex(matrix, 0, 1, depth).texture(0, 1).color(0xFFFFFFFF);
				vertices.vertex(matrix, 1, 1, depth).texture(1, 1).color(0xFFFFFFFF);
				vertices.vertex(matrix, 1, 0, depth).texture(1, 0).color(0xFFFFFFFF);
			}

			@Override
			public RenderPipeline pipeline() {
				return RenderPipelines.MOJANG_LOGO;
			}

			@Override
			public TextureSetup textureSetup() {
				return TextureSetup.withoutGlTexture(FRAMEBUFFER.getColorAttachmentView());
			}

			@Override
			public @Nullable ScreenRect scissorArea() {
				return null;
			}

			@Override
			public @Nullable ScreenRect bounds() {
				return ScreenRect.empty();
			}
		});

		context.createNewRootLayer();
        context.getMatrices().pushMatrix();

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

		context.drawTexturedQuad(RenderPipelines.GUI_TEXTURED, FRAMEBUFFER.getColorAttachmentView(), 0, 0, FRAMEBUFFER.textureWidth / this.client.getWindow().getScaleFactor(), FRAMEBUFFER.textureHeight / this.client.getWindow().getScaleFactor(), 0, 1, 0, 1, -1);

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
            if (textRenderer.getWidth(suggestionText) > (this.getWidth() - padding - 20)) {
                drawOverflowText(context, textRenderer, Text.literal(suggestionText), x + padding, suggestionY + padding - 2, x + listWidth - padding, suggestionY + adjustedLineHeight, 0xFFFFFFFF, hover);
            } else {
                context.drawTextWithShadow(textRenderer, Text.literal(suggestionText), x + padding, suggestionY + padding + 1, 0xFFFFFFFF);
            }
        }

        context.disableScissor();

        // scrollbar thingy
        if (scrollable) {
            int scrollbarWidth = 10;

            int sbX = x + listWidth + 5;

			context.enableScissor(sbX, y, sbX + scrollbarWidth, y + dynamicHeight);

			context.drawTexturedQuad(RenderPipelines.GUI_TEXTURED, FRAMEBUFFER.getColorAttachmentView(), 0, 0, FRAMEBUFFER.textureWidth / this.client.getWindow().getScaleFactor(), FRAMEBUFFER.textureHeight / this.client.getWindow().getScaleFactor(), 0, 1, 0, 1, -1);

            context.fill(sbX, y, sbX + scrollbarWidth, y + dynamicHeight, bgColor);
            context.disableScissor();

            drawOutline(context, sbX, y, scrollbarWidth, dynamicHeight, 0xFFFFFFFF);

            float visibleRatio = (float) rows / totalLines;
            int handleHeight = Math.max((int)(visibleRatio * (dynamicHeight - 2 * 2)), 4);

            int availableScroll = totalLines - rows;
            int handleYOffset = availableScroll > 0 ? (int)(((float)scrollOffset / availableScroll) * ((dynamicHeight - 2 * 2) - handleHeight)) : 0;
            int handleX = sbX + 2;
            int handleY = y + 2 + handleYOffset;
            int handleWidth = scrollbarWidth - 2 * 2;
            
            context.fill(handleX, handleY, handleX + handleWidth, handleY + handleHeight, 0xFFFFFFFF);
        }

        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean scrollable = suggestions.size() > maxRows;

        int adjustedLineHeight = baseLineHeight + padding * 2;
        int listWidth = scrollable ? this.getWidth() - 5 - 10 : this.getWidth();

        int relativeY = (int)mouseY - this.getY();
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScrollbar) {
            int adjustedLineHeight = baseLineHeight + padding * 2;
            int rows = Math.min(suggestions.size(), maxRows);

            int dynamicHeight = rows * adjustedLineHeight;
            int totalLines = suggestions.size();
            int availableScroll = totalLines - maxRows;

            float visibleRatio = (float) maxRows / totalLines;
            int handleHeight = Math.max((int)(visibleRatio * (dynamicHeight - 2 * 2)), 4);

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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
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
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {}
    
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

    private void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    // similar to drawScrollableText but not centered
    private void drawOverflowText(DrawContext context, TextRenderer textRenderer, Text text, int startX, int startY, int endX, int endY, int color, boolean hovered) {
        int textRendererWidth = textRenderer.getWidth(text);
        int availableWidth = endX - startX;
        int y = startY + ((endY - startY) - 9) / 2;
    
        // if hovered, we scroll
        if (hovered) {
            int extra = textRendererWidth - availableWidth;
            double time = Util.getMeasuringTimeMs() / 1000.0;
            double period = Math.max(extra / 8.0, 2.0);
            double scroll = 0.5 - 0.5 * Math.cos(2 * Math.PI * time / period);
            int offset = (int)(scroll * extra);
            
            context.enableScissor(startX, startY, endX, endY);
            context.drawTextWithShadow(textRenderer, text, startX - offset, y, color);
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

                    if (textRenderer.getWidth(collapsedText) > availableWidth) {
                        int prefixWidth = textRenderer.getWidth(prefix);
                        int allowedForLast = availableWidth - prefixWidth;

                        if (allowedForLast < 0) {
                            finalText = trimToWidth(collapsedText, availableWidth, textRenderer);
                        } else {
                            if (textRenderer.getWidth(lastPart) > allowedForLast) {
                                lastPart = trimToWidth(lastPart, allowedForLast, textRenderer);
                            }

                            finalText = prefix + lastPart;
                        }
                    }
                } else {
                    finalText = trimToWidth(collapsedText, availableWidth, textRenderer);
                }
            } else {
                if (textRenderer.getWidth(finalText) > availableWidth) {
                    finalText = trimToWidth(finalText, availableWidth, textRenderer);
                }
            }

            context.enableScissor(startX, startY, endX, endY);
            context.drawTextWithShadow(textRenderer, Text.literal(finalText), startX, y, color);
            context.disableScissor();
        }
    }

    private String trimToWidth(String rawText, int availableWidth, TextRenderer textRenderer) {
        if (textRenderer.getWidth(rawText) <= availableWidth) {
            return rawText;
        }

        int maxWidth = availableWidth - textRenderer.getWidth("...");
        int trimIndex = rawText.length();

        while (trimIndex > 0 && textRenderer.getWidth(rawText.substring(0, trimIndex)) > maxWidth) {
            trimIndex--;
        }
        
        return rawText.substring(0, trimIndex) + "...";
    }

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (suggestions.isEmpty()) {
			return false;
		}

		int rows = Math.min(suggestions.size(), maxRows);

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
			return textFieldWidget.keyPressed(keyCode, scanCode, modifiers);
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void setFocused(boolean focused) {
		if (!focused) {
			this.selectedItem = -1;
		}

		super.setFocused(focused);
	}
}
