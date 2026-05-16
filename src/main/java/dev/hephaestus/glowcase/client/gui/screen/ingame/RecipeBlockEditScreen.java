package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Floats;
import dev.hephaestus.glowcase.block.entity.RecipeBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.GlowcaseClient;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import dev.hephaestus.glowcase.client.util.RRVClientUtils;
import dev.hephaestus.glowcase.packet.C2SEditRecipeBlock;
import dev.hephaestus.glowcase.util.RRVUtils;
import dev.hephaestus.glowcase.util.RequiresRRVLoaded;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RecipeBlockEditScreen extends BlockEditorScreen<RecipeBlockEntity> {
	private static final List<Identifier> NO_SUGGESTIONS = List.of();

	private EditBox recipeWidget;
	private EditBox rotationXWidget;
	private EditBox rotationYWidget;

	private SuggestionListWidget<Identifier> suggestionWidget;

	// Can't use GlowcaseWidgetHolder as that can crash if EMI is not present
	@NotNull
	private final AtomicReference<RequiresRRVLoaded> glowcaseWidgetHolder = new AtomicReference<>(null);

	private Button zOffsetToggle;
	private int fontHeight = -1;

	private int baseY;

	public RecipeBlockEditScreen(RecipeBlockEntity blockEntity) {
		super(blockEntity);
	}

	@Override
	public void init() {
		super.init();

		if (this.minecraft == null) return;

		if (fontHeight == -1) {
			fontHeight = this.minecraft.font.lineHeight;
			baseY = height / 2 - ((2 * fontHeight + 95) / 2) + fontHeight - (GlowcaseClient.RRV_LOADED ? 46 : 0);
		}


		this.recipeWidget = new GlowcaseEditBox(this.minecraft.font, width / 2 - 150, baseY + 10, 300, 20, Component.empty());
		this.recipeWidget.setMaxLength(1024);
		this.recipeWidget.setValue(blockEntity.recipe);

		this.rotationXWidget = new EditBox(this.minecraft.font, (width - 145) / 2, baseY + fontHeight + 45, 70, 20, Component.empty());
		this.rotationXWidget.setMaxLength(1024);
		this.rotationXWidget.setValue(Float.toString(blockEntity.rotationX));
		this.rotationXWidget.setResponder(s -> {
			if (Floats.tryParse(s) instanceof Float parsed) {
				blockEntity.rotationX = parsed;
			}
		});

		this.rotationYWidget = new EditBox(this.minecraft.font, (width - 145) / 2 + 75, baseY + fontHeight + 45, 70, 20, Component.empty());
		this.rotationYWidget.setMaxLength(1024);
		this.rotationYWidget.setValue(Float.toString(blockEntity.rotationY));
		this.rotationYWidget.setResponder(s -> {
			if (Floats.tryParse(s) instanceof Float parsed) {
				blockEntity.rotationY = parsed;
			}
		});

		this.zOffsetToggle = Button.builder(Component.literal(this.blockEntity.zOffset.name()), action -> {
			switch (blockEntity.zOffset) {
				case FRONT -> blockEntity.zOffset = TextBlockEntity.ZOffset.CENTER;
				case CENTER -> blockEntity.zOffset = TextBlockEntity.ZOffset.BACK;
				case BACK -> blockEntity.zOffset = TextBlockEntity.ZOffset.FRONT;
			}

			this.zOffsetToggle.setMessage(Component.literal(this.blockEntity.zOffset.name()));
		}).bounds(width / 2 - 75, baseY + fontHeight + 75, 150, 20).build();

		suggestionWidget = SuggestionListWidget.forTextField(recipeWidget, minecraft.font, Identifier::toString);

		recipeWidget.setResponder((text) -> {
			if (Identifier.tryParse(this.recipeWidget.getValue()) != null) {
				this.blockEntity.recipe = this.recipeWidget.getValue();
			}

			if (GlowcaseClient.RRV_LOADED) {
				suggestionWidget.updateSuggestions(RRVUtils.RECIPE_LIST.get(), text, false, this);

				RRVClientUtils.updateWidgetHolder(recipeWidget.getValue(), glowcaseWidgetHolder);
			}
		});

		this.addRenderableWidget(this.recipeWidget);
		this.addRenderableWidget(this.rotationXWidget);
		this.addRenderableWidget(this.rotationYWidget);
		this.addRenderableWidget(this.zOffsetToggle);

		if (GlowcaseClient.RRV_LOADED && glowcaseWidgetHolder.get() == null) {
			RRVClientUtils.updateWidgetHolder(recipeWidget.getValue(), glowcaseWidgetHolder);
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

		if (GlowcaseClient.RRV_LOADED && glowcaseWidgetHolder.get() != null) {
			int baseYForRecipe = (baseY + fontHeight + 95);
			int spaceForRecipe = height - baseYForRecipe;

			RequiresRRVLoaded widgetHolder = glowcaseWidgetHolder.get();

			int holderWidth = RRVClientUtils.getHolderWidth(widgetHolder);
			int holderHeight = RRVClientUtils.getHolderHeight(widgetHolder);

			Matrix3x2fStack matrixStack = graphics.pose();
			matrixStack.pushMatrix();
			matrixStack.translate(width / 2f - holderWidth / 2f, baseYForRecipe + spaceForRecipe / 2f - holderHeight / 2f);

			RRVClientUtils.renderEmiRecipe(widgetHolder, graphics, delta);

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
	public @Nullable CustomPacketPayload getUpdatePayload() {
		blockEntity.setRecipe(recipeWidget.getValue());
		return C2SEditRecipeBlock.of(blockEntity);
	}

}
