package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.AnchorPositionGridWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.IconButtonWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.number.Vec3FieldsWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.HexColorEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.slider.EditableSliderWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.tab.GlowcaseTab;
import dev.hephaestus.glowcase.client.gui.widget.ingame.tab.GlowcaseTabNavBar;
import dev.hephaestus.glowcase.client.gui.widget.ingame.text.GlowcaseMultilineEditBox;
import dev.hephaestus.glowcase.packet.C2SEditTextBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TextBlockEditScreen extends TextEditorScreen implements BlockEditor<TextBlockEntity> {
	private final TextBlockEntity textBlockEntity;

	private GlowcaseMultilineEditBox glowcaseEditBox;
	private HexColorEditBox colorEntryWidget;
	private HexColorEditBox backgroundColorEntryWidget;

	private ColorPickerWidget colorPickerWidget;
	private SuggestionListWidget<Identifier> fontSuggestionWidget;

	private Button zFrontButton;
	private Button zCenterButton;
	private Button zBackButton;
	private IconButtonWidget justifyLeftButton;
	private IconButtonWidget justifyCenterButton;
	private IconButtonWidget justifyRightButton;
	private Button insertFontButton;

	public TextBlockEditScreen(TextBlockEntity textBlockEntity) {
		this.textBlockEntity = textBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		this.glowcaseEditBox = GlowcaseMultilineEditBox.builder(
			this.font, this.textBlockEntity.lines, 2, 25, this.width - 4, this.height - 28,
			parsedLines -> {
				this.textBlockEntity.lines = new ArrayList<>(parsedLines);
				this.textBlockEntity.rebake(true);
			}
		).build();
		this.glowcaseEditBox.updateSettings(this.textBlockEntity.color, this.textBlockEntity.shadow, this.textBlockEntity.textAlignment);
		this.glowcaseEditBox.textField.seekCursor(Whence.ABSOLUTE, 0);
		this.addRenderableWidget(this.glowcaseEditBox);

		this.colorPickerWidget = this.createColorPickerWidget();

		List<AbstractWidget> editTabWidgets = this.initEditTabWidgets();
		List<AbstractWidget> viewTabWidgets = this.initViewTabWidgets();

		GlowcaseTabNavBar tabNavBar = GlowcaseTabNavBar.builder(
			this.width, height -> {
				this.glowcaseEditBox.setY(height + 3);
			})
			.setY(2)
			.setWidgetAreaPadding(2)
			.addTabs(
				new GlowcaseTab(
					Component.literal("Edit"), 20,
					editTabWidgets
				),
				new GlowcaseTab(
					Component.literal("View"), 42,
					viewTabWidgets
				)
			).build();
		this.addRenderableWidget(tabNavBar);
	}

	public List<AbstractWidget> initEditTabWidgets() {
		// nav bar y padding (2) + tab button height (20) + widget area padding (1) = 23;
		int firstRowY = 23;

		EditableSliderWidget scaleSlider = EditableSliderWidget.builder(
			this.font, this.textBlockEntity.scale, 0.125f, 16,
			aFloat -> Component.translatable("gui.glowcase.scale_value", aFloat),
			aFloat -> {
				this.textBlockEntity.scale = aFloat;
				this.textBlockEntity.rebake(true);
			})
			.setStep(0.125f)
			.setWidth(113)
			.build();
		scaleSlider.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.text_scale_slider")));

		this.colorEntryWidget = HexColorEditBox.builder(this.minecraft.font, 0, 0,
				() -> this.textBlockEntity.color, color -> {
					this.textBlockEntity.color = color;
					this.textBlockEntity.rebake(true);
					this.glowcaseEditBox.setTextColor(color);
				}
			)
			.setEditableAlpha(true)
			.setColorPickerWidget(this.colorPickerWidget)
			.build();
		this.colorEntryWidget.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.text_color")));

		this.backgroundColorEntryWidget = HexColorEditBox.builder(this.minecraft.font, 0, 0,
				() -> this.textBlockEntity.backgroundColor, color -> {
					this.textBlockEntity.backgroundColor = color;
					this.textBlockEntity.rebake(true);
				}
			)
			.setEditableAlpha(true)
			.setColorPickerWidget(this.colorPickerWidget)
			.build();
		this.backgroundColorEntryWidget.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.background_color_argb")));

		this.addRenderableWidget(scaleSlider);
		this.initFormattingButtons(0, 0, 0);
		this.addRenderableWidget(this.colorEntryWidget);
		this.addRenderableWidget(this.backgroundColorEntryWidget);

		LinearLayout editTabLayout = LinearLayout.horizontal().spacing(2);
		editTabLayout.addChild(scaleSlider, LayoutSettings.defaults().paddingRight(6));
		for (Button formattingButton : this.formattingButtons) {
			editTabLayout.addChild(formattingButton);
		}
		editTabLayout.addChild(this.colorEntryWidget, LayoutSettings.defaults().paddingLeft(6));
		editTabLayout.addChild(this.backgroundColorEntryWidget, LayoutSettings.defaults().paddingLeft(4));

		editTabLayout.arrangeElements(); // Setup initial positioning
		FrameLayout.centerInRectangle(editTabLayout, 0, firstRowY, this.width, firstRowY); // Finish positioning

		List<AbstractWidget> editTabWidgets = new ArrayList<>(this.formattingButtons);
		editTabWidgets.add(scaleSlider);
		editTabWidgets.add(this.colorEntryWidget);
		editTabWidgets.add(this.backgroundColorEntryWidget);
		return editTabWidgets;
	}

	public List<AbstractWidget> initViewTabWidgets() {
		int firstRowY = 24;
		int secondRowY = firstRowY + 22;

		this.justifyLeftButton = IconButtonWidget.builder(
				Glowcase.id("text_alignment/left"), button -> {
					this.textBlockEntity.textAlignment = TextBlockEntity.TextAlignment.LEFT;
					this.textBlockEntity.rebake(true);
					this.glowcaseEditBox.setTextAlignment(TextBlockEntity.TextAlignment.LEFT);
					this.updateSelectedJustifyButton();
				})
				.dimensions(0, 0, 20, 20, 16, 16)
				.build();
		this.justifyLeftButton.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.justify_left")));
		this.justifyCenterButton = IconButtonWidget.builder(
				Glowcase.id("text_alignment/center"), button -> {
					this.textBlockEntity.textAlignment = TextBlockEntity.TextAlignment.CENTER;
					this.textBlockEntity.rebake(true);
					this.glowcaseEditBox.setTextAlignment(TextBlockEntity.TextAlignment.CENTER);
					this.updateSelectedJustifyButton();
				})
			.dimensions(0, 0, 20, 20, 16, 16)
			.build();
		this.justifyCenterButton.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.justify_center")));
		this.justifyRightButton = IconButtonWidget.builder(
				Glowcase.id("text_alignment/right"), button -> {
					this.textBlockEntity.textAlignment = TextBlockEntity.TextAlignment.RIGHT;
					this.textBlockEntity.rebake(true);
					this.glowcaseEditBox.setTextAlignment(TextBlockEntity.TextAlignment.RIGHT);
					this.updateSelectedJustifyButton();
				})
			.dimensions(0, 0, 20, 20, 16, 16)
			.build();
		this.justifyRightButton.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.justify_right")));
		this.updateSelectedJustifyButton();

		this.zFrontButton = Button.builder(Component.translatable("gui.glowcase.front"), button -> {
			this.textBlockEntity.zOffset = TextBlockEntity.ZOffset.FRONT;
			this.textBlockEntity.rebake(true);
			this.updateSelectedZButton();
		}).size(50, 20).build();
		this.zCenterButton = Button.builder(Component.translatable("gui.glowcase.center"), button -> {
			this.textBlockEntity.zOffset = TextBlockEntity.ZOffset.CENTER;
			this.textBlockEntity.rebake(true);
			this.updateSelectedZButton();
		}).size(50, 20).build();
		this.zBackButton = Button.builder(Component.translatable("gui.glowcase.back"), button -> {
			this.textBlockEntity.zOffset = TextBlockEntity.ZOffset.BACK;
			this.textBlockEntity.rebake(true);
			this.updateSelectedZButton();
		}).size(50, 20).build();
		this.updateSelectedZButton();

		// TODO (AC) - Use block entity anchor value instead of horizontal alignment
		TextBlockEntity.Anchor fakeAnchor = TextBlockEntity.Anchor.fromHorizontalAlignment(this.textBlockEntity.horizontalAlignment);
		AnchorPositionGridWidget anchorGrid = new AnchorPositionGridWidget(0, 0, fakeAnchor, anchor -> {
			// TODO (AC) - Set block anchor variables here
			if (anchor.getY() == 0) {
				this.textBlockEntity.horizontalAlignment = TextBlockEntity.HorizontalAlignment.values()[anchor.getX() + 1];
				this.textBlockEntity.rebake(true);
			}
		});

		CycleButton<Boolean> textShadowButton = CycleButton.onOffBuilder(this.textBlockEntity.shadow).create(
			Component.translatable("gui.glowcase.text_shadow"),
			(button, shadow) -> {
				this.textBlockEntity.shadow = shadow;
				this.textBlockEntity.rebake(true);
				this.glowcaseEditBox.setTextShadow(shadow);
			}
		);
		textShadowButton.setWidth(100);

		this.insertFontButton = IconButtonWidget.builder(Component.literal("Aa"), button -> {
			this.fontSuggestionWidget.setPosition(this.insertFontButton.getRight() - 200, this.insertFontButton.getBottom());
			if (this.fontSuggestionWidget.hasSuggestions()) {
				this.fontSuggestionWidget.updateSuggestions(new ArrayList<>(), "", this);
			} else {
				this.fontSuggestionWidget.updateSuggestions(getAvailableFontIds(), "", this);
			}
		}).bounds(0, 0, 20, 20)
			.tooltip(Tooltip.create(Component.translatable("gui.glowcase.insert_font")))
			.build();

		this.fontSuggestionWidget = new SuggestionListWidget<>(
			this.insertFontButton, this.font,
			0, 0, 200, 200,
			10, 4, 10,
			identifier -> {
				// TODO - The logic behind this will need some updating if a Block Font button is ever added
				this.insertTag("font '" + identifier.toString() + "'");
				this.fontSuggestionWidget.updateSuggestions(new ArrayList<>(), "", this);
			},
			Identifier::toString
		);
		this.fontSuggestionWidget.updateSuggestions(new ArrayList<>(), "", this);

		Vec3FieldsWidget offsetWidgets = Vec3FieldsWidget.builder(this.font, this.textBlockEntity.offset)
			.setWidth(106)
			.setEditBoxCharacterLimit(5)
			.setTooltips(
				Tooltip.create(Component.translatable("gui.glowcase.x_offset_label")),
				Tooltip.create(Component.translatable("gui.glowcase.y_offset_label")),
				Tooltip.create(Component.translatable("gui.glowcase.z_offset_label"))
			)
			.setOnValueChange(vec3 -> {
				this.textBlockEntity.offset = vec3;
				this.textBlockEntity.rebake(true);
			})
			.build();
		Vec3FieldsWidget rotationWidgets = Vec3FieldsWidget.builder(this.font, this.textBlockEntity.rotation)
			.setWidth(106)
			.setRotation(true)
			.setEditBoxCharacterLimit(5)
			.setTooltips(
				Tooltip.create(Component.translatable("gui.glowcase.yaw")),
				Tooltip.create(Component.translatable("gui.glowcase.pitch")),
				Tooltip.create(Component.translatable("gui.glowcase.roll"))
			)
			.setOnValueChange(vec3 -> {
				this.textBlockEntity.rotation = vec3;
				this.textBlockEntity.rebake(true);
			})
			.build();

		this.addRenderableWidget(this.justifyLeftButton);
		this.addRenderableWidget(this.justifyCenterButton);
		this.addRenderableWidget(this.justifyRightButton);
		this.addRenderableWidget(zFrontButton);
		this.addRenderableWidget(zCenterButton);
		this.addRenderableWidget(zBackButton);
		this.addRenderableWidget(anchorGrid);
		this.addRenderableWidget(textShadowButton);

		this.addRenderableWidget(offsetWidgets);
		this.addRenderableWidget(rotationWidgets);
		this.addRenderableWidget(insertFontButton);

		LinearLayout viewTabFirstRowLayout = LinearLayout.horizontal().spacing(2);
		viewTabFirstRowLayout.addChild(this.justifyLeftButton);
		viewTabFirstRowLayout.addChild(this.justifyCenterButton, LayoutSettings.defaults().paddingLeft(-2));
		viewTabFirstRowLayout.addChild(this.justifyRightButton, LayoutSettings.defaults().paddingLeft(-2).paddingRight(4));
		viewTabFirstRowLayout.addChild(zFrontButton);
		viewTabFirstRowLayout.addChild(zCenterButton, LayoutSettings.defaults().paddingLeft(-2));
		viewTabFirstRowLayout.addChild(zBackButton, LayoutSettings.defaults().paddingLeft(-2).paddingRight(2));
		viewTabFirstRowLayout.addChild(anchorGrid, LayoutSettings.defaults().paddingRight(4));
		viewTabFirstRowLayout.addChild(textShadowButton);

		viewTabFirstRowLayout.arrangeElements(); // Setup initial positioning
		FrameLayout.centerInRectangle(viewTabFirstRowLayout, 0, firstRowY, this.width, 42); // Finish positioning

		rotationWidgets.setPosition(anchorGrid.getX() - 4 - rotationWidgets.getWidth(), secondRowY);
		offsetWidgets.setPosition(rotationWidgets.getX() - 4 - offsetWidgets.getWidth(), secondRowY);
		insertFontButton.setPosition(anchorGrid.getRight() + 6, secondRowY);

		return List.of(
			justifyRightButton, justifyCenterButton, justifyLeftButton,
			zFrontButton, zCenterButton, zBackButton,
			anchorGrid, textShadowButton,
			offsetWidgets, rotationWidgets, insertFontButton
		);
	}

	public void updateSelectedJustifyButton() {
		TextBlockEntity.TextAlignment justify = this.textBlockEntity.textAlignment;
		this.justifyLeftButton.active = justify != TextBlockEntity.TextAlignment.LEFT;
		this.justifyCenterButton.active = justify != TextBlockEntity.TextAlignment.CENTER;
		this.justifyRightButton.active = justify != TextBlockEntity.TextAlignment.RIGHT;
	}

	public void updateSelectedZButton() {
		TextBlockEntity.ZOffset zOffset = this.textBlockEntity.zOffset;
		this.zFrontButton.active = zOffset != TextBlockEntity.ZOffset.FRONT;
		this.zCenterButton.active = zOffset != TextBlockEntity.ZOffset.CENTER;
		this.zBackButton.active = zOffset != TextBlockEntity.ZOffset.BACK;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		this.fontSuggestionWidget.extractRenderState(graphics, mouseX, mouseY, delta);
		this.extractColorPicker(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.keyPressedColorPicker(event)) return true;
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (mouseClickedColorPicker(event, doubleClick)) return true;
		if (this.fontSuggestionWidget.mouseClicked(event, doubleClick)) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (this.fontSuggestionWidget.draggingScrollbar && this.fontSuggestionWidget.mouseDragged(event, dx, dy)) return true;
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (this.fontSuggestionWidget.isFocused() && this.fontSuggestionWidget.isMouseOver(x, y)
			&& this.fontSuggestionWidget.mouseScrolled(x, y, scrollX, scrollY)) return true;
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public ColorPickerWidget getColorPickerWidget() {
		return this.colorPickerWidget;
	}

	@Override
	GlowcaseMultilineEditBox getGlowcaseMultilineEditBox() {
		return this.glowcaseEditBox;
	}

	@Override
	public TextBlockEntity getBlockEntity() {
		return this.textBlockEntity;
	}

	@Override
	public @Nullable CustomPacketPayload getUpdatePayload() {
		return C2SEditTextBlock.of(textBlockEntity);
	}

	// This is so cursed but it gives the best order of fonts given the amount of repetitive entries
	// Some number of fonts have duplicate entries within an "include" folder, which may or may not work with QuickText
	// Minecraft fonts are listed first (predefined order), while remaining fonts come afterward
	// Any fonts within the "include" folder, Minecraft or external, are omitted
	public static List<Identifier> getAvailableFontIds() {
		ResourceManager manager = Minecraft.getInstance().getResourceManager();
		FileToIdConverter converter = FileToIdConverter.json("font");
		List<Identifier> availableFonts = new ArrayList<>(); // All available fonts, including "include" folder entries
		List<Identifier> fontsInOrder = new ArrayList<>(); // List of fonts to return
		fontsInOrder.add(Identifier.withDefaultNamespace("default")); // Set proper order of Vanilla's builtin fonts
		fontsInOrder.add(Identifier.withDefaultNamespace("alt"));
		fontsInOrder.add(Identifier.withDefaultNamespace("illageralt"));
		fontsInOrder.add(Identifier.withDefaultNamespace("uniform"));

		for (Map.Entry<Identifier, List<Resource>> fontEntry : converter.listMatchingResourceStacks(manager).entrySet()) {
			Identifier fontName = converter.fileToId(fontEntry.getKey());
			availableFonts.add(fontName);
		}

		for (Iterator<Identifier> iterator = availableFonts.iterator(); iterator.hasNext(); ) {
			Identifier id = iterator.next();
			if (id.getNamespace().equals("minecraft") && !id.getPath().contains("include/")) {
				iterator.remove();
				if (fontsInOrder.contains(id)) continue; // Likely because we added the builtin fonts already
				fontsInOrder.add(id);
			}
		}

		for (Iterator<Identifier> iterator = availableFonts.iterator(); iterator.hasNext(); ) {
			Identifier id =  iterator.next();
			if (!id.getPath().contains("include/")) {
				iterator.remove();
				if (fontsInOrder.contains(id)) continue; // Shouldn't happen but just in case
				fontsInOrder.add(id);
			}
		}

		return fontsInOrder;
	}
}
