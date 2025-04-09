package dev.hephaestus.glowcase.client.gui.widget.ingame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import org.jetbrains.annotations.NotNull;

public class SuggestionListWidget<T> extends ClickableWidget {
    private final TextRenderer textRenderer;
    private final MinecraftClient client;

    private final List<T> suggestions = new ArrayList<>();
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

    public SuggestionListWidget(TextRenderer textRenderer, int x, int y, int width, int height, int baseLineHeight, int padding, int maxRows, Consumer<T> onSelect, Function<T, String> toStringFunction) {
        super(x, y, width, height, Text.empty());

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

	@Override
	public void setWidth(int width) {
		super.setWidth(width);
		while (textRenderer.getWidth("m".repeat(characterWidth)) < this.width) {
			characterWidth++;
		}
	}

	// update the suggestion list based on filter
    public void updateSuggestions(List<T> newSuggestions, String filter) {
        suggestions.clear();
		this.filter = filter;

        for (T suggestion : newSuggestions) {
            String text = toStringFunction.apply(suggestion);

            if (text.startsWith(filter)) {
                suggestions.add(suggestion);
            }
        }

        // if there is only 1 suggestion & it's equal to input, hide the list
        if (suggestions.size() == 1 && toStringFunction.apply(suggestions.get(0)).equals(filter)) {
            suggestions.clear();
        }

        scrollOffset = 0;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (suggestions.isEmpty()) return;
        
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 1);
        
        int adjustedLineHeight = baseLineHeight + padding * 2;
        int rows = Math.min(suggestions.size(), maxRows);
        int dynamicHeight = rows * adjustedLineHeight;

        boolean scrollable = suggestions.size() > maxRows;
        int totalLines = suggestions.size();
        int maxLines = rows;

        int listWidth = scrollable ? this.getWidth() - 5 - 10 : this.getWidth();

        context.enableScissor(this.getX(), this.getY(), this.getX() + listWidth, this.getY() + dynamicHeight);
        
        float blurValue = (float) MinecraftClient.getInstance().options.getMenuBackgroundBlurrinessValue();
        if (blurValue >= 1.0F) {
            client.gameRenderer.renderBlur(delta);
        }

        client.getFramebuffer().beginWrite(false);
        context.fill(this.getX(), this.getY(), this.getX() + listWidth, this.getY() + dynamicHeight, 0x90000000);

        drawOutline(context, this.getX(), this.getY(), listWidth, dynamicHeight, 0xFFFFFFFF);

        if (scrollOffset > totalLines - maxLines) {
            scrollOffset = Math.max(0, totalLines - maxLines);
        }
        
        // render each suggestion
        for (int i = 0; i < maxLines; i++) {
            int suggestionIndex = i + scrollOffset;
            if (suggestionIndex >= totalLines) break;
            
            T suggestion = suggestions.get(suggestionIndex);
            String suggestionText = toStringFunction.apply(suggestion);
            int suggestionY = getY() + i * adjustedLineHeight;
            
            // highlight hovered suggestion
            if (mouseX >= this.getX() && mouseX <= this.getX() + listWidth && mouseY >= suggestionY && mouseY < suggestionY + adjustedLineHeight) {
                context.fill(this.getX(), suggestionY, this.getX() + listWidth, suggestionY + adjustedLineHeight, 0xFF217C08);
                drawOutline(context, this.getX(), suggestionY, listWidth, adjustedLineHeight, 0xFFFFFFFF);
            }

            // detect if the text is too long AND if the item is hovered, then scroll, otherwise don't
            boolean suggestionHovered = (mouseX >= this.getX() && mouseX <= this.getX() + listWidth && mouseY >= suggestionY && mouseY < suggestionY + adjustedLineHeight);
            if (textRenderer.getWidth(suggestionText) > (this.getWidth() - padding)) {
                drawOverflowText(context, textRenderer, Text.literal(suggestionText), this.getX() + padding, suggestionY + padding - 2, this.getX() + listWidth - padding, suggestionY + adjustedLineHeight, 0xFFFFFF, suggestionHovered);
            } else {
                context.drawTextWithShadow(textRenderer, Text.literal(suggestionText), this.getX() + padding, suggestionY + padding + 1, 0xFFFFFF);
            }
        }

        context.disableScissor();

        // scrollbar thingy
        if (scrollable) {
            int scrollbarWidth = 10;

            int sbX = getX() + listWidth + 5;
            int sbY = getY();

            int scrollbarHeight = dynamicHeight;
            int scrollBarBgColor = 0x90000000;

            context.enableScissor(sbX, sbY, sbX + scrollbarWidth, sbY + scrollbarHeight);

            float blurScrollbar = (float) client.options.getMenuBackgroundBlurrinessValue();
            if (blurScrollbar >= 1.0F) {
                client.gameRenderer.renderBlur(delta);
            }
            client.getFramebuffer().beginWrite(false);

            context.fill(sbX, sbY, sbX + scrollbarWidth, sbY + scrollbarHeight, scrollBarBgColor);
            context.disableScissor();

            drawOutline(context, sbX, sbY, scrollbarWidth, scrollbarHeight, 0xFFFFFFFF);

            float visibleRatio = (float) rows / totalLines;
            int handleHeight = Math.max((int)(visibleRatio * (dynamicHeight - 2 * 2)), 4);

            int availableScroll = totalLines - rows;
            int handleYOffset = availableScroll > 0 ? (int)(((float)scrollOffset / availableScroll) * ((dynamicHeight - 2 * 2) - handleHeight)) : 0;
            int handleX = sbX + 2;
            int handleY = sbY + 2 + handleYOffset;
            int handleWidth = scrollbarWidth - 2 * 2;
            
            context.fill(handleX, handleY, handleX + handleWidth, handleY + handleHeight, 0xFFFFFFFF);
        }

        context.getMatrices().pop();
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

        if (textRendererWidth > availableWidth) {
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
                context.drawTextWithShadow(textRenderer, text, startX, y, color);
            }
        } else {
            context.drawTextWithShadow(textRenderer, text, startX, y, color);
        }
    }    
}
