package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.HexColorEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.packet.C2SEditSpriteBlock;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SpriteBlockEditScreen extends BlockEditorScreen<SpriteBlockEntity> implements ColorPickerIncludedScreen {
	private EditBox spriteWidget;
	private Button spriteWidgetHelpButton;
	private Button rotationWidget;
	private Button zOffsetToggle;
	private HexColorEditBox colorEntryWidget;
	private EditBox scaleEntryWidget;

	private List<FormattedCharSequence> spriteHelpTooltipText;

	private ColorPickerWidget colorPickerWidget;
	private SuggestionListWidget<String> suggestionWidget;
    private List<String> validSprites = new ArrayList<>();

	public SpriteBlockEditScreen(SpriteBlockEntity blockEntity) {
		super(blockEntity);
	}

	@Override
	public void init() {
		super.init();

		this.spriteWidget = new GlowcaseEditBox(this.minecraft.font, width / 2 - 90, height / 2 - 55, 180, 20, Component.empty());
		this.spriteWidget.setMaxLength(255);
		this.spriteWidget.setValue(blockEntity.getSprite());
		this.spriteWidget.setResponder(string -> {
			this.blockEntity.setSprite(this.spriteWidget.getValue());
		});

		Tooltip spriteHelpTooltip =  Tooltip.create(Component.translatable("gui.glowcase.screen.sprite_edit.sprite"));

		this.spriteWidgetHelpButton = Button.builder(Component.literal("?"), action -> {})
			.bounds(spriteWidget.getX() + spriteWidget.getWidth() + 4, spriteWidget.getY(), spriteWidget.getHeight(), spriteWidget.getHeight())
			.tooltip(spriteHelpTooltip)
			.build();

		this.rotationWidget = Button.builder(Component.translatable("gui.glowcase.rotate"), (action) -> {
			this.blockEntity.rotation = (this.blockEntity.rotation + 45) % 360;
		}).bounds(width / 2 - 90, height / 2 - 25, 180, 20).build();

		this.zOffsetToggle = Button.builder(Component.literal(this.blockEntity.zOffset.name()), action -> {
			switch (blockEntity.zOffset) {
				case FRONT -> blockEntity.zOffset = TextBlockEntity.ZOffset.CENTER;
				case CENTER -> blockEntity.zOffset = TextBlockEntity.ZOffset.BACK;
				case BACK -> blockEntity.zOffset = TextBlockEntity.ZOffset.FRONT;
			}

			this.zOffsetToggle.setMessage(Component.literal(this.blockEntity.zOffset.name()));
		}).bounds(width / 2 - 90, height / 2 + 5, 180, 20).build();

		this.colorPickerWidget = this.createColorPickerWidget();

		this.colorEntryWidget = HexColorEditBox.builder(this.minecraft.font, width / 2 - 90, height / 2 + 35,
				() -> this.blockEntity.color, color -> this.blockEntity.color = color
			)
			.setWidth(180)
			.setColorPickerWidget(this.colorPickerWidget)
			.build();

		this.scaleEntryWidget = new EditBox(this.minecraft.font, width / 2 - 90, height / 2 + 65, 180, 20, Component.empty());
		this.scaleEntryWidget.setValue(String.valueOf(this.blockEntity.scale));
		this.scaleEntryWidget.setResponder(string -> {
			 try {
				 this.blockEntity.scale = Float.parseFloat(string);
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
			.map(Identifier::toString)
			.forEach(validSprites::add);

		// And you can use any modid to display its icon
		FabricLoader.getInstance().getAllMods().forEach(mod -> {
			validSprites.add("mod:"+mod.getMetadata().getId());
		});

		return validSprites;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		// Tooltip is handled this way, since setting the tooltip directly on the help button widget causes the tooltip
		// to clip off-screen at higher GUI scales.
		/*if (this.spriteWidgetHelpButton.isHovered() || (this.spriteWidgetHelpButton.isFocused() && this.client.getNavigationType().isKeyboard())) {
			setTooltip(this.spriteHelpTooltipText);
		}*/

		this.extractColorPicker(graphics, mouseX, mouseY, delta);
		// render the list over everything
		suggestionWidget.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();

		if (mouseClickedColorPicker(event, doubleClick)) return true;

        if (suggestionWidget.isMouseOver(mouseX, mouseY) && spriteWidget.isFocused()) {
            return suggestionWidget.mouseClicked(event, doubleClick);
        } else {
            suggestionWidget.updateSuggestions(new ArrayList<>(), "", this);
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
        if (suggestionWidget.isMouseOver(mouseX, mouseY) && spriteWidget.isFocused()) {
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
		if (keyPressedColorPicker(event)) return true;
		return super.keyPressed(event);
	}

	@Override
	public CustomPacketPayload getUpdatePayload() {
		blockEntity.setSprite(spriteWidget.getValue());
		blockEntity.setChanged();
		return C2SEditSpriteBlock.of(blockEntity);
	}

	@Override
	public ColorPickerWidget getColorPickerWidget() {
		return this.colorPickerWidget;
	}
}
