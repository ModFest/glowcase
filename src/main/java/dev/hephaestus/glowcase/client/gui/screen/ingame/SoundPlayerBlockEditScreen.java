package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.SoundPlayerBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseTextFieldWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.Vec3FieldsWidget;
import dev.hephaestus.glowcase.packet.C2SEditSoundBlock;
import dev.hephaestus.glowcase.util.ParseUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SoundPlayerBlockEditScreen extends GlowcaseScreen {
	private final SoundPlayerBlockEntity soundBlock;

	private EditBox soundId;
	private Button categoryButton;
	private Button cancelOthersButton;

	private EditBox volume;
	private EditBox pitch;
	private EditBox repeatDelay;

	private EditBox distance;
	private Button relativeButton;
	private Vec3FieldsWidget offset;

	private SuggestionListWidget<String> suggestionWidget;
	private List<String> validSounds = new ArrayList<>();

	public SoundPlayerBlockEditScreen(SoundPlayerBlockEntity soundBlock) {
		this.soundBlock = soundBlock;
	}

	@Override
	protected void init() {
		super.init();
		Objects.requireNonNull(this.minecraft);
//		RegistryWrapper.WrapperLookup lookup = Objects.requireNonNull(client.world).getRegistryManager();

		this.soundId = new GlowcaseTextFieldWidget(
			this.minecraft.font,
			width / 10, height / 2 - 110,
			8 * width / 10, 20,
			Component.empty());
		this.soundId.setMaxLength(1024);
		this.soundId.setValue(soundBlock.soundId.toString());
		this.addRenderableWidget(soundId);

		this.categoryButton = new Button.Builder(Component.translatableEscape("gui.glowcase.sound_category", this.soundBlock.category.getName()), (action) -> {
			soundBlock.cycleCategory();
			this.categoryButton.setMessage(Component.translatableEscape("gui.glowcase.sound_category", this.soundBlock.category.getName()));
		}).bounds(width / 10, height / 2 - 60, (4 * width / 10) - 6, 20).build();
		this.addRenderableWidget(this.categoryButton);

		this.cancelOthersButton = new Button.Builder(Component.nullToEmpty(Boolean.toString(soundBlock.cancelOthers)), (action) -> {
			soundBlock.cancelOthers = !soundBlock.cancelOthers;
			this.cancelOthersButton.setMessage(Component.nullToEmpty(Boolean.toString(soundBlock.cancelOthers)));
		}).bounds(width / 10 + (4 * width / 10) + 6, height / 2 - 60, (4 * width / 10) - 6, 20).build();
		this.addRenderableWidget(this.cancelOthersButton);

		this.volume = new EditBox(
			this.minecraft.font,
			width / 10, height / 2 - 10,
			(4 * width / 10) - 6, 20,
			Component.empty());
		this.volume.setMaxLength(16);
		this.volume.setValue(String.valueOf(soundBlock.volume));
		//FIXME 26.1
//		this.volume.setFilter(ParseUtil::canParseDouble);
		this.addRenderableWidget(this.volume);

		this.pitch = new EditBox(
			this.minecraft.font,
			width / 10 + (4 * width / 10) + 6, height / 2 - 10,
			(4 * width / 10) - 6, 20,
			Component.empty());
		this.pitch.setMaxLength(16);
		this.pitch.setValue(String.valueOf(soundBlock.pitch));
		//FIXME 26.1
//		this.pitch.setFilter(ParseUtil::canParseDouble);
		this.addRenderableWidget(this.pitch);

		this.repeatDelay = new EditBox(
			this.minecraft.font,
			width / 10, height / 2 + 40,
			(4 * width / 10) - 6, 20,
			Component.empty());
		this.repeatDelay.setMaxLength(16);
		this.repeatDelay.setValue(String.valueOf(soundBlock.repeatDelay));
		//FIXME 26.1
//		this.repeatDelay.setFilter(ParseUtil::canParseInt);
		this.addRenderableWidget(this.repeatDelay);

		this.distance = new EditBox(
			this.minecraft.font,
			width / 10 + (4 * width / 10) + 6, height / 2 + 40,
			(4 * width / 10) - 6, 20,
			Component.empty());
		this.distance.setMaxLength(16);
		this.distance.setValue(String.valueOf(soundBlock.distance));
		//FIXME 26.1
//		this.distance.setFilter(ParseUtil::canParseDouble);
		this.addRenderableWidget(this.distance);

		this.relativeButton = new Button.Builder(Component.translatableEscape("gui.glowcase.sound_positioning", soundBlock.relative), (action) -> {
			soundBlock.relative = !soundBlock.relative;
			this.relativeButton.setMessage(Component.translatableEscape("gui.glowcase.sound_positioning", soundBlock.relative));
		}).bounds(width / 10, height / 2 + 90, (4 * width / 10) - 6, 20).build();
		this.addRenderableWidget(this.relativeButton);

		this.offset = new Vec3FieldsWidget(
			width / 10 + (4 * width / 10) + 6, height / 2 + 90,
			(4 * width / 10) - 6, 20,
			this.minecraft,
			soundBlock.offset);
		this.addRenderableWidget(this.offset);

		validSounds = BuiltInRegistries.SOUND_EVENT.stream()
			.map(BuiltInRegistries.SOUND_EVENT::getKey)
			.filter(Objects::nonNull)
			.map(Identifier::toString)
			.collect(Collectors.toList());

		suggestionWidget = SuggestionListWidget.forTextFieldWithStaticSuggestions(soundId, minecraft.font, validSounds, Function.identity(), this);
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawString(
			this.minecraft.font,
			Component.translatable("gui.glowcase.sound_category_no_arg"),
			this.categoryButton.getX(), this.categoryButton.getY() - 20,
			0xFFFFFFFF
		);

		context.drawString(
			this.minecraft.font,
			Component.translatable("gui.glowcase.cancel_others"),
			this.cancelOthersButton.getX(), this.cancelOthersButton.getY() - 20,
			0xFFFFFFFF
		);

		context.drawString(
			this.minecraft.font,
			Component.translatable("gui.glowcase.volume"),
			this.volume.getX(), this.volume.getY() - 20,
			0xFFFFFFFF
		);

		context.drawString(
			this.minecraft.font,
			Component.translatable("gui.glowcase.pitch"),
			this.pitch.getX(), this.pitch.getY() - 20,
			0xFFFFFFFF
		);

		context.drawString(
			this.minecraft.font,
			Component.translatable("gui.glowcase.repeat_delay"),
			this.repeatDelay.getX(), this.repeatDelay.getY() - 20,
			0xFFFFFFFF
		);

		context.drawString(
			this.minecraft.font,
			Component.translatable("gui.glowcase.distance"),
			this.distance.getX(), this.distance.getY() - 20,
			0xFFFFFFFF
		);

		context.drawString(
			this.minecraft.font,
			Component.translatable("gui.glowcase.sound_positioning_no_arg"),
			this.relativeButton.getX(), this.relativeButton.getY() - 20,
			0xFFFFFFFF
		);

		context.drawString(
			this.minecraft.font,
			Component.translatable("gui.glowcase.offset"),
			this.offset.getX(), this.offset.getY() - 20,
			0xFFFFFFFF
		);

		// render the list over everything
		suggestionWidget.renderWidget(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (suggestionWidget.isMouseOver(mouseX, mouseY) && soundId.isFocused()) {
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
		if (suggestionWidget.isMouseOver(mouseX, mouseY) && soundId.isFocused()) {
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
		soundBlock.volume = (float) ParseUtil.parseOrDefault(this.volume.getValue(), soundBlock.volume);
		soundBlock.pitch = (float) ParseUtil.parseOrDefault(this.pitch.getValue(), soundBlock.pitch);
		soundBlock.repeatDelay = ParseUtil.parseOrDefault(this.repeatDelay.getValue(), soundBlock.repeatDelay);

		soundBlock.distance = (float) ParseUtil.parseOrDefault(this.distance.getValue(), soundBlock.distance);
		soundBlock.offset = this.offset.value();

		setSound();

		super.onClose();
	}

	private void setSound() {
		Objects.requireNonNull(this.minecraft);

		String idText = this.soundId.getValue();
		Identifier id = Identifier.tryParse(idText);

		if (id != null) {
			soundBlock.soundId = id;
		}

		C2SEditSoundBlock.of(soundBlock).send();
	}
}
