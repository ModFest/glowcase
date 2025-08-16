package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Floats;

import dev.hephaestus.glowcase.block.entity.RecipeBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.GlowcaseClient;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseTextFieldWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import dev.hephaestus.glowcase.packet.C2SEditRecipeBlock;
import dev.hephaestus.glowcase.client.util.EmiClientUtils;
import dev.hephaestus.glowcase.util.EmiUtils;
import dev.hephaestus.glowcase.util.RequiresEmiLoaded;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RecipeBlockEditScreen extends GlowcaseScreen {
	private static final List<Identifier> NO_SUGGESTIONS = List.of();
	private final RecipeBlockEntity recipeBlockEntity;

	private TextFieldWidget recipeWidget;
	private TextFieldWidget rotationXWidget;
	private TextFieldWidget rotationYWidget;

	private SuggestionListWidget<Identifier> suggestionWidget;

	// Can't use GlowcaseWidgetHolder as that can crash if EMI is not present
	@NotNull
	private final AtomicReference<RequiresEmiLoaded> glowcaseWidgetHolder = new AtomicReference<>(null);

	private ButtonWidget zOffsetToggle;
	private int fontHeight = -1;

	private int baseY;

	public RecipeBlockEditScreen(RecipeBlockEntity recipeBlockEntity) {
		this.recipeBlockEntity = recipeBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		if (this.client == null) return;

		if (fontHeight == -1) {
			fontHeight = this.client.textRenderer.fontHeight;
			baseY = height / 2 - ((2 * fontHeight + 95) / 2) + fontHeight - (GlowcaseClient.EMI_LOADED ? 46 : 0);
		}


		this.recipeWidget = new GlowcaseTextFieldWidget(this.client.textRenderer, width / 2 - 150, baseY + 10, 300, 20, Text.empty());
		this.recipeWidget.setMaxLength(1024);
		this.recipeWidget.setText(recipeBlockEntity.recipe);

		this.rotationXWidget = new TextFieldWidget(this.client.textRenderer, (width - 145) / 2, baseY + fontHeight + 45, 70, 20, Text.empty());
		this.rotationXWidget.setMaxLength(1024);
		this.rotationXWidget.setText(Float.toString(recipeBlockEntity.rotationX));
		this.rotationXWidget.setChangedListener(s -> {
			if (Floats.tryParse(s) instanceof Float parsed) {
				recipeBlockEntity.rotationX = parsed;
			}
		});

		this.rotationYWidget = new TextFieldWidget(this.client.textRenderer, (width - 145) / 2 + 75, baseY + fontHeight + 45, 70, 20, Text.empty());
		this.rotationYWidget.setMaxLength(1024);
		this.rotationYWidget.setText(Float.toString(recipeBlockEntity.rotationY));
		this.rotationYWidget.setChangedListener(s -> {
			if (Floats.tryParse(s) instanceof Float parsed) {
				recipeBlockEntity.rotationY = parsed;
			}
		});

		this.zOffsetToggle = ButtonWidget.builder(Text.literal(this.recipeBlockEntity.zOffset.name()), action -> {
			switch (recipeBlockEntity.zOffset) {
				case FRONT -> recipeBlockEntity.zOffset = TextBlockEntity.ZOffset.CENTER;
				case CENTER -> recipeBlockEntity.zOffset = TextBlockEntity.ZOffset.BACK;
				case BACK -> recipeBlockEntity.zOffset = TextBlockEntity.ZOffset.FRONT;
			}

			this.zOffsetToggle.setMessage(Text.literal(this.recipeBlockEntity.zOffset.name()));
		}).dimensions(width / 2 - 75, baseY + fontHeight + 75, 150, 20).build();

		suggestionWidget = SuggestionListWidget.forTextField(recipeWidget, client.textRenderer, Identifier::toString);

		recipeWidget.setChangedListener((text) -> {
			if (Identifier.tryParse(this.recipeWidget.getText()) != null) {
				this.recipeBlockEntity.recipe = this.recipeWidget.getText();
			}

			if (GlowcaseClient.EMI_LOADED) {
				suggestionWidget.updateSuggestions(EmiUtils.RECIPE_LIST.get(), text, false, this);

				EmiClientUtils.updateWidgetHolder(recipeWidget.getText(), glowcaseWidgetHolder);
			}
		});

		this.addDrawableChild(this.recipeWidget);
		this.addDrawableChild(this.rotationXWidget);
		this.addDrawableChild(this.rotationYWidget);
		this.addDrawableChild(this.zOffsetToggle);

		if (GlowcaseClient.EMI_LOADED && glowcaseWidgetHolder.get() == null) {
			EmiClientUtils.updateWidgetHolder(recipeWidget.getText(), glowcaseWidgetHolder);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		if (this.client == null) return;

		if (fontHeight == -1) {
			fontHeight = this.client.textRenderer.fontHeight;
			baseY = height / 2 - ((2 * fontHeight + 95) / 2) + fontHeight - 46;
		}

		context.drawTextWithShadow(
			this.client.textRenderer,
			Text.translatable("gui.glowcase.recipe"),
			width / 2 - (this.client.textRenderer.getWidth(Text.translatable("gui.glowcase.recipe")) / 2),
			baseY - fontHeight,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			this.client.textRenderer,
			Text.translatable("gui.glowcase.pitch"),
			((width - 145) / 2) + 35 - (this.client.textRenderer.getWidth(Text.translatable("gui.glowcase.pitch")) / 2),
			baseY + 40,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			this.client.textRenderer,
			Text.translatable("gui.glowcase.yaw"),
			((width - 145) / 2) + 75 + 35 - (this.client.textRenderer.getWidth(Text.translatable("gui.glowcase.yaw")) / 2),
			baseY + 40,
			0xFFFFFFFF
		);
		// render the list over everything
		suggestionWidget.renderWidget(context, mouseX, mouseY, delta);

		if (GlowcaseClient.EMI_LOADED && glowcaseWidgetHolder.get() != null) {
			int baseYForRecipe = (baseY + fontHeight + 95);
			int spaceForRecipe = height - baseYForRecipe;

			RequiresEmiLoaded widgetHolder = glowcaseWidgetHolder.get();

			int holderWidth = EmiClientUtils.getHolderWidth(widgetHolder);
			int holderHeight = EmiClientUtils.getHolderHeight(widgetHolder);

			Matrix3x2fStack matrixStack = context.getMatrices();
			matrixStack.pushMatrix();
			matrixStack.translate(width / 2f - holderWidth / 2f, baseYForRecipe + spaceForRecipe / 2f - holderHeight / 2f);

			EmiClientUtils.renderEmiRecipe(widgetHolder, context, delta);

			matrixStack.popMatrix();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (suggestionWidget.isMouseOver(mouseX, mouseY) && recipeWidget.isFocused()) {
			return suggestionWidget.mouseClicked(mouseX, mouseY, button);
		} else {
			suggestionWidget.updateSuggestions(NO_SUGGESTIONS, "", this);
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (suggestionWidget.draggingScrollbar) {
			if (suggestionWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY))
				return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (suggestionWidget.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void close() {
		recipeBlockEntity.setRecipe(recipeWidget.getText());
		C2SEditRecipeBlock.of(recipeBlockEntity).send();
		super.close();
	}

}
