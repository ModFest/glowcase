package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.SoundPlayerBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseTextFieldWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.Vec3FieldsWidget;
import dev.hephaestus.glowcase.util.ParseUtil;
import dev.hephaestus.glowcase.packet.C2SEditSoundBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SoundPlayerBlockEditScreen extends GlowcaseScreen {
	private final SoundPlayerBlockEntity soundBlock;

	private TextFieldWidget soundId;
	private ButtonWidget categoryButton;
	private ButtonWidget cancelOthersButton;

	private TextFieldWidget volume;
	private TextFieldWidget pitch;
	private TextFieldWidget repeatDelay;

	private TextFieldWidget distance;
	private ButtonWidget relativeButton;
	private Vec3FieldsWidget offset;

	private SuggestionListWidget<String> suggestionWidget;
	private List<String> validSounds = new ArrayList<>();

	public SoundPlayerBlockEditScreen(SoundPlayerBlockEntity soundBlock) {
		this.soundBlock = soundBlock;
	}

	@Override
	protected void init() {
		super.init();
		Objects.requireNonNull(this.client);
//		RegistryWrapper.WrapperLookup lookup = Objects.requireNonNull(client.world).getRegistryManager();

		this.soundId = new GlowcaseTextFieldWidget(
			this.client.textRenderer,
			width / 10, height / 2 - 110,
			8 * width / 10, 20,
			Text.empty());
		this.soundId.setMaxLength(1024);
		this.soundId.setText(soundBlock.soundId.toString());
		this.addDrawableChild(soundId);

		this.categoryButton = new ButtonWidget.Builder(Text.stringifiedTranslatable("gui.glowcase.sound_category", this.soundBlock.category.getName()), (action) -> {
			soundBlock.cycleCategory();
			this.categoryButton.setMessage(Text.stringifiedTranslatable("gui.glowcase.sound_category", this.soundBlock.category.getName()));
		}).dimensions(width / 10, height / 2 - 60, (4 * width / 10) - 6, 20).build();
		this.addDrawableChild(this.categoryButton);

		this.cancelOthersButton = new ButtonWidget.Builder(Text.of(Boolean.toString(soundBlock.cancelOthers)), (action) -> {
			soundBlock.cancelOthers = !soundBlock.cancelOthers;
			this.cancelOthersButton.setMessage(Text.of(Boolean.toString(soundBlock.cancelOthers)));
		}).dimensions(width / 10 + (4 * width / 10) + 6, height / 2 - 60, (4 * width / 10) - 6, 20).build();
		this.addDrawableChild(this.cancelOthersButton);

		this.volume = new TextFieldWidget(
			this.client.textRenderer,
			width / 10, height / 2 - 10,
			(4 * width / 10) - 6, 20,
			Text.empty());
		this.volume.setMaxLength(16);
		this.volume.setText(String.valueOf(soundBlock.volume));
		this.volume.setTextPredicate(ParseUtil::canParseDouble);
		this.addDrawableChild(this.volume);

		this.pitch = new TextFieldWidget(
			this.client.textRenderer,
			width / 10 + (4 * width / 10) + 6, height / 2 - 10,
			(4 * width / 10) - 6, 20,
			Text.empty());
		this.pitch.setMaxLength(16);
		this.pitch.setText(String.valueOf(soundBlock.pitch));
		this.pitch.setTextPredicate(ParseUtil::canParseDouble);
		this.addDrawableChild(this.pitch);

		this.repeatDelay = new TextFieldWidget(
			this.client.textRenderer,
			width / 10, height / 2 + 40,
			(4 * width / 10) - 6, 20,
			Text.empty());
		this.repeatDelay.setMaxLength(16);
		this.repeatDelay.setText(String.valueOf(soundBlock.repeatDelay));
		this.repeatDelay.setTextPredicate(ParseUtil::canParseInt);
		this.addDrawableChild(this.repeatDelay);

		this.distance = new TextFieldWidget(
			this.client.textRenderer,
			width / 10 + (4 * width / 10) + 6, height / 2 + 40,
			(4 * width / 10) - 6, 20,
			Text.empty());
		this.distance.setMaxLength(16);
		this.distance.setText(String.valueOf(soundBlock.distance));
		this.distance.setTextPredicate(ParseUtil::canParseDouble);
		this.addDrawableChild(this.distance);

		this.relativeButton = new ButtonWidget.Builder(Text.stringifiedTranslatable("gui.glowcase.sound_positioning", soundBlock.relative), (action) -> {
			soundBlock.relative = !soundBlock.relative;
			this.relativeButton.setMessage(Text.stringifiedTranslatable("gui.glowcase.sound_positioning", soundBlock.relative));
		}).dimensions(width / 10, height / 2 + 90, (4 * width / 10) - 6, 20).build();
		this.addDrawableChild(this.relativeButton);

		this.offset = new Vec3FieldsWidget(
			width / 10 + (4 * width / 10) + 6, height / 2 + 90,
			(4 * width / 10) - 6, 20,
			this.client,
			soundBlock.offset);
		this.addDrawableChild(this.offset);

		validSounds = Registries.SOUND_EVENT.stream()
			.map(Registries.SOUND_EVENT::getId)
			.filter(Objects::nonNull)
			.map(Identifier::toString)
			.collect(Collectors.toList());

		suggestionWidget = SuggestionListWidget.forTextFieldWithStaticSuggestions(soundId, client.textRenderer, validSounds, Function.identity(), this);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawTextWithShadow(
			this.client.textRenderer,
			Text.translatable("gui.glowcase.sound_category_no_arg"),
			this.categoryButton.getX(), this.categoryButton.getY() - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			this.client.textRenderer,
			Text.translatable("gui.glowcase.cancel_others"),
			this.cancelOthersButton.getX(), this.cancelOthersButton.getY() - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			this.client.textRenderer,
			Text.translatable("gui.glowcase.volume"),
			this.volume.getX(), this.volume.getY() - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			this.client.textRenderer,
			Text.translatable("gui.glowcase.pitch"),
			this.pitch.getX(), this.pitch.getY() - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			this.client.textRenderer,
			Text.translatable("gui.glowcase.repeat_delay"),
			this.repeatDelay.getX(), this.repeatDelay.getY() - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			this.client.textRenderer,
			Text.translatable("gui.glowcase.distance"),
			this.distance.getX(), this.distance.getY() - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			this.client.textRenderer,
			Text.translatable("gui.glowcase.sound_positioning_no_arg"),
			this.relativeButton.getX(), this.relativeButton.getY() - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			this.client.textRenderer,
			Text.translatable("gui.glowcase.offset"),
			this.offset.getX(), this.offset.getY() - 20,
			0xFFFFFFFF
		);

		// render the list over everything
		suggestionWidget.renderWidget(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (suggestionWidget.isMouseOver(mouseX, mouseY) && soundId.isFocused()) {
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
		if (suggestionWidget.isMouseOver(mouseX, mouseY) && soundId.isFocused()) {
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
		soundBlock.volume = (float) ParseUtil.parseOrDefault(this.volume.getText(), soundBlock.volume);
		soundBlock.pitch = (float) ParseUtil.parseOrDefault(this.pitch.getText(), soundBlock.pitch);
		soundBlock.repeatDelay = ParseUtil.parseOrDefault(this.repeatDelay.getText(), soundBlock.repeatDelay);

		soundBlock.distance = (float) ParseUtil.parseOrDefault(this.distance.getText(), soundBlock.distance);
		soundBlock.offset = this.offset.value();

		setSound();

		super.close();
	}

	private void setSound() {
		Objects.requireNonNull(this.client);

		String idText = this.soundId.getText();
		Identifier id = Identifier.tryParse(idText);

		if (id != null) {
			soundBlock.soundId = id;
		}

		C2SEditSoundBlock.of(soundBlock).send();
	}
}
