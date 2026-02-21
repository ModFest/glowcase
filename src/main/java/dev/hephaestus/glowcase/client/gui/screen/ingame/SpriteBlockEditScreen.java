package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseTextFieldWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import dev.hephaestus.glowcase.packet.C2SEditSpriteBlock;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SpriteBlockEditScreen extends GlowcaseScreen {
	private final SpriteBlockEntity spriteBlockEntity;

	private EditBox spriteWidget;
	private Button spriteWidgetHelpButton;
	private Button rotationWidget;
	private Button zOffsetToggle;
	private EditBox colorEntryWidget;
	private EditBox scaleEntryWidget;

	private List<FormattedCharSequence> spriteHelpTooltipText;

	private SuggestionListWidget<String> suggestionWidget;
    private List<String> validSprites = new ArrayList<>();

	public SpriteBlockEditScreen(SpriteBlockEntity spriteBlockEntity) {
		this.spriteBlockEntity = spriteBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		if (this.minecraft == null) return;

		this.spriteWidget = new GlowcaseTextFieldWidget(this.minecraft.font, width / 2 - 90, height / 2 - 55, 180, 20, Component.empty());
		this.spriteWidget.setMaxLength(255);
		this.spriteWidget.setValue(spriteBlockEntity.getSprite());
		this.spriteWidget.setResponder(string -> {
			this.spriteBlockEntity.setSprite(this.spriteWidget.getValue());
		});

		Tooltip spriteHelpTooltip =  Tooltip.create(Component.translatable("gui.glowcase.screen.sprite_edit.sprite"));

		this.spriteWidgetHelpButton = Button.builder(Component.literal("?"), action -> {})
			.bounds(spriteWidget.getX() + spriteWidget.getWidth() + 4, spriteWidget.getY(), spriteWidget.getHeight(), spriteWidget.getHeight())
			.tooltip(spriteHelpTooltip)
			.build();

		this.rotationWidget = Button.builder(Component.translatable("gui.glowcase.rotate"), (action) -> {
			this.spriteBlockEntity.rotation = (this.spriteBlockEntity.rotation + 45) % 360;
		}).bounds(width / 2 - 90, height / 2 - 25, 180, 20).build();

		this.zOffsetToggle = Button.builder(Component.literal(this.spriteBlockEntity.zOffset.name()), action -> {
			switch (spriteBlockEntity.zOffset) {
				case FRONT -> spriteBlockEntity.zOffset = TextBlockEntity.ZOffset.CENTER;
				case CENTER -> spriteBlockEntity.zOffset = TextBlockEntity.ZOffset.BACK;
				case BACK -> spriteBlockEntity.zOffset = TextBlockEntity.ZOffset.FRONT;
			}

			this.zOffsetToggle.setMessage(Component.literal(this.spriteBlockEntity.zOffset.name()));
		}).bounds(width / 2 - 90, height / 2 + 5, 180, 20).build();

		this.colorEntryWidget = new EditBox(this.minecraft.font, width / 2 - 90, height / 2 + 35, 180, 20, Component.empty());
		this.colorEntryWidget.setValue("#" + String.format("%1$06X", this.spriteBlockEntity.color & 0x00FFFFFF));
		this.colorEntryWidget.setResponder(string -> {
			TextColor.parseColor(this.colorEntryWidget.getValue()).ifSuccess(color -> {
				this.spriteBlockEntity.color = color == null ? 0xFFFFFFFF : color.getValue() | 0xFF000000;
			});
		});

		this.scaleEntryWidget = new EditBox(this.minecraft.font, width / 2 - 90, height / 2 + 65, 180, 20, Component.empty());
		this.scaleEntryWidget.setValue(String.valueOf(this.spriteBlockEntity.scale));
		this.scaleEntryWidget.setResponder(string -> {
			 try {
				 this.spriteBlockEntity.scale = Float.parseFloat(string);
			 } catch (NumberFormatException ignored) {}
		});

		this.addRenderableWidget(this.spriteWidget);
		this.addRenderableWidget(this.spriteWidgetHelpButton);
		this.addRenderableWidget(this.rotationWidget);
		this.addRenderableWidget(this.zOffsetToggle);
		this.addRenderableWidget(this.colorEntryWidget);
		this.addRenderableWidget(this.scaleEntryWidget);

		ResourceManager resourceManager = this.minecraft.getResourceManager();
		validSprites = allValidSprites(resourceManager);

		suggestionWidget = SuggestionListWidget.forTextFieldWithStaticSuggestions(spriteWidget, minecraft.font, validSprites, Function.identity(), this);
	}

	/**
	 * A list of all valid entries for {@link #spriteWidget}. Used for suggestions.
	 */
	public static List<String> allValidSprites(ResourceManager resourceManager) {
		var validSprites = new ArrayList<String>();

		// Add all sprites inside /textures/sprite, these are explicitly meant for the sprite block
		// and can be used with just their filename. As these are intended to be used here, we'll list them first
		resourceManager.listResources("textures/sprite", id -> id.getPath().endsWith(".png")).forEach((sprite, res) -> {
			validSprites.add(sprite.getPath().substring("textures/sprite/".length(), sprite.getPath().length() - 4));
		});

		// You can use any texture. Technically I think you can also use ones outside of texture
		// But findResources requires us to filter
		resourceManager.listResources("textures", id -> id.getPath().endsWith(".png")).forEach((sprite, res) -> {
			validSprites.add(sprite.toString());
		});

		// You can also display any item
		BuiltInRegistries.ITEM.stream()
			.map(BuiltInRegistries.ITEM::getKey)
			.map(ResourceLocation::toString)
			.forEach(validSprites::add);

		// And you can use any modid to display its icon
		FabricLoader.getInstance().getAllMods().forEach(mod -> {
			validSprites.add("mod:"+mod.getMetadata().getId());
		});

		return validSprites;
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		// Tooltip is handled this way, since setting the tooltip directly on the help button widget causes the tooltip
		// to clip off-screen at higher GUI scales.
		/*if (this.spriteWidgetHelpButton.isHovered() || (this.spriteWidgetHelpButton.isFocused() && this.client.getNavigationType().isKeyboard())) {
			setTooltip(this.spriteHelpTooltipText);
		}*/

		// render the list over everything
		suggestionWidget.renderWidget(context, mouseX, mouseY, delta);
	}

	@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (suggestionWidget.isMouseOver(mouseX, mouseY) && spriteWidget.isFocused()) {
            return suggestionWidget.mouseClicked(mouseX, mouseY, button);
        } else {
            suggestionWidget.updateSuggestions(new ArrayList<>(), "", this);
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
        if (suggestionWidget.isMouseOver(mouseX, mouseY) && spriteWidget.isFocused()) {
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
	public void onClose() {
		spriteBlockEntity.setSprite(spriteWidget.getValue());
		spriteBlockEntity.setChanged();
		C2SEditSpriteBlock.of(spriteBlockEntity).send();
		super.onClose();
	}
}
