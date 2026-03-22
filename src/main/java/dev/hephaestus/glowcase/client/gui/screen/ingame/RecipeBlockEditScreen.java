package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Floats;
import dev.hephaestus.glowcase.block.entity.RecipeBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.GlowcaseClient;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import dev.hephaestus.glowcase.client.util.EmiClientUtils;
import dev.hephaestus.glowcase.packet.C2SEditRecipeBlock;
import dev.hephaestus.glowcase.util.EmiUtils;
import dev.hephaestus.glowcase.util.RequiresEmiLoaded;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RecipeBlockEditScreen extends GlowcaseScreen {
	private static final List<Identifier> NO_SUGGESTIONS = List.of();
	private final RecipeBlockEntity recipeBlockEntity;

	private EditBox recipeWidget;
	private EditBox rotationXWidget;
	private EditBox rotationYWidget;

	private SuggestionListWidget<Identifier> suggestionWidget;

	// Can't use GlowcaseWidgetHolder as that can crash if EMI is not present
	@NotNull
	private final AtomicReference<RequiresEmiLoaded> glowcaseWidgetHolder = new AtomicReference<>(null);

	private Button zOffsetToggle;
	private int fontHeight = -1;

	private int baseY;

	public RecipeBlockEditScreen(RecipeBlockEntity recipeBlockEntity) {
		this.recipeBlockEntity = recipeBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		if (this.minecraft == null) return;

		if (fontHeight == -1) {
			fontHeight = this.minecraft.font.lineHeight;
			baseY = height / 2 - ((2 * fontHeight + 95) / 2) + fontHeight - (GlowcaseClient.EMI_LOADED ? 46 : 0);
		}


		this.recipeWidget = new GlowcaseEditBox(this.minecraft.font, width / 2 - 150, baseY + 10, 300, 20, Component.empty());
		this.recipeWidget.setMaxLength(1024);
		this.recipeWidget.setValue(recipeBlockEntity.recipe);

		this.rotationXWidget = new EditBox(this.minecraft.font, (width - 145) / 2, baseY + fontHeight + 45, 70, 20, Component.empty());
		this.rotationXWidget.setMaxLength(1024);
		this.rotationXWidget.setValue(Float.toString(recipeBlockEntity.rotationX));
		this.rotationXWidget.setResponder(s -> {
			if (Floats.tryParse(s) instanceof Float parsed) {
				recipeBlockEntity.rotationX = parsed;
			}
		});

		this.rotationYWidget = new EditBox(this.minecraft.font, (width - 145) / 2 + 75, baseY + fontHeight + 45, 70, 20, Component.empty());
		this.rotationYWidget.setMaxLength(1024);
		this.rotationYWidget.setValue(Float.toString(recipeBlockEntity.rotationY));
		this.rotationYWidget.setResponder(s -> {
			if (Floats.tryParse(s) instanceof Float parsed) {
				recipeBlockEntity.rotationY = parsed;
			}
		});

		this.zOffsetToggle = Button.builder(Component.literal(this.recipeBlockEntity.zOffset.name()), action -> {
			switch (recipeBlockEntity.zOffset) {
				case FRONT -> recipeBlockEntity.zOffset = TextBlockEntity.ZOffset.CENTER;
				case CENTER -> recipeBlockEntity.zOffset = TextBlockEntity.ZOffset.BACK;
				case BACK -> recipeBlockEntity.zOffset = TextBlockEntity.ZOffset.FRONT;
			}

			this.zOffsetToggle.setMessage(Component.literal(this.recipeBlockEntity.zOffset.name()));
		}).bounds(width / 2 - 75, baseY + fontHeight + 75, 150, 20).build();

		suggestionWidget = SuggestionListWidget.forTextField(recipeWidget, minecraft.font, Identifier::toString);

		recipeWidget.setResponder((text) -> {
			if (Identifier.tryParse(this.recipeWidget.getValue()) != null) {
				this.recipeBlockEntity.recipe = this.recipeWidget.getValue();
			}

			if (GlowcaseClient.EMI_LOADED) {
				suggestionWidget.updateSuggestions(EmiUtils.RECIPE_LIST.get(), text, false, this);

				EmiClientUtils.updateWidgetHolder(recipeWidget.getValue(), glowcaseWidgetHolder);
			}
		});

		this.addRenderableWidget(this.recipeWidget);
		this.addRenderableWidget(this.rotationXWidget);
		this.addRenderableWidget(this.rotationYWidget);
		this.addRenderableWidget(this.zOffsetToggle);

		if (GlowcaseClient.EMI_LOADED && glowcaseWidgetHolder.get() == null) {
			EmiClientUtils.updateWidgetHolder(recipeWidget.getValue(), glowcaseWidgetHolder);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		if (fontHeight == -1) {
			fontHeight = this.minecraft.font.lineHeight;
			baseY = height / 2 - ((2 * fontHeight + 95) / 2) + fontHeight - 46;
		}

		graphics.text(
			this.minecraft.font,
			Component.translatable("gui.glowcase.recipe"),
			width / 2 - (this.minecraft.font.width(Component.translatable("gui.glowcase.recipe")) / 2),
			baseY - fontHeight,
			0xFFFFFFFF
		);

		graphics.text(
			this.minecraft.font,
			Component.translatable("gui.glowcase.pitch"),
			((width - 145) / 2) + 35 - (this.minecraft.font.width(Component.translatable("gui.glowcase.pitch")) / 2),
			baseY + 40,
			0xFFFFFFFF
		);

		graphics.text(
			this.minecraft.font,
			Component.translatable("gui.glowcase.yaw"),
			((width - 145) / 2) + 75 + 35 - (this.minecraft.font.width(Component.translatable("gui.glowcase.yaw")) / 2),
			baseY + 40,
			0xFFFFFFFF
		);
		// render the list over everything
		suggestionWidget.extractRenderState(graphics, mouseX, mouseY, delta);

		if (GlowcaseClient.EMI_LOADED && glowcaseWidgetHolder.get() != null) {
			int baseYForRecipe = (baseY + fontHeight + 95);
			int spaceForRecipe = height - baseYForRecipe;

			RequiresEmiLoaded widgetHolder = glowcaseWidgetHolder.get();

			int holderWidth = EmiClientUtils.getHolderWidth(widgetHolder);
			int holderHeight = EmiClientUtils.getHolderHeight(widgetHolder);

			Matrix3x2fStack matrixStack = graphics.pose();
			matrixStack.pushMatrix();
			matrixStack.translate(width / 2f - holderWidth / 2f, baseYForRecipe + spaceForRecipe / 2f - holderHeight / 2f);

			EmiClientUtils.renderEmiRecipe(widgetHolder, graphics, delta);

			matrixStack.popMatrix();
		}
	}
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (suggestionWidget.isMouseOver(mouseX, mouseY) && recipeWidget.isFocused()) {
			return suggestionWidget.mouseClicked(event, doubleClick);
		} else {
			suggestionWidget.updateSuggestions(NO_SUGGESTIONS, "", this);
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (suggestionWidget.draggingScrollbar) {
			if (suggestionWidget.mouseDragged(event, dx, dy))
				return true;
		}

		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (suggestionWidget.isMouseOver(mouseX, mouseY) && recipeWidget.isFocused()) {
			suggestionWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (suggestionWidget.keyPressed(event)) {
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		recipeBlockEntity.setRecipe(recipeWidget.getValue());
		C2SEditRecipeBlock.of(recipeBlockEntity).send();
		super.onClose();
	}

}
