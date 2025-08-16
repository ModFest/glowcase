package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.hephaestus.glowcase.block.entity.ParticleDisplayBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseTextFieldWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.Vec3FieldsWidget;
import dev.hephaestus.glowcase.util.DeviatedInteger;
import dev.hephaestus.glowcase.util.DeviatedVec3d;
import dev.hephaestus.glowcase.util.ParseUtil;
import dev.hephaestus.glowcase.packet.C2SEditParticleDisplayBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ParticleDisplayEditScreen extends GlowcaseScreen {
	private final ParticleDisplayBlockEntity blockEntity;
	private TextFieldWidget particleId;

	private Vec3FieldsWidget positionMean;
	private Vec3FieldsWidget positionStdDev;

	private Vec3FieldsWidget velocityMean;
	private Vec3FieldsWidget velocityStdDev;

	private TextFieldWidget countMean;
	private TextFieldWidget countStdDev;

	private TextFieldWidget tickRateMean;
	private TextFieldWidget tickRateStdDev;

	private SuggestionListWidget<Identifier> suggestionWidget;
	private List<Identifier> validParticles = new ArrayList<>();

	public ParticleDisplayEditScreen(ParticleDisplayBlockEntity blockEntity) {
		this.blockEntity = blockEntity;
	}

	@Override
	protected void init() {
		super.init();
		Objects.requireNonNull(this.client);
		RegistryWrapper.WrapperLookup lookup = Objects.requireNonNull(client.world).getRegistryManager();


		// region Particle ID
		particleId = new GlowcaseTextFieldWidget(
			this.client.textRenderer,
			width / 10, height / 2 - 110,
			8 * width / 10, 20,
			Text.empty()
		);

		particleId.setMaxLength(9999);

		String optionsString = effectToTag(blockEntity.particle, lookup.getOps(NbtOps.INSTANCE)).toString();
		if (optionsString.equals("{}")) optionsString = "";

		particleId.setText(Registries.PARTICLE_TYPE.getId(blockEntity.particle.getType()) + optionsString);

		this.addDrawableChild(particleId);

		validParticles = Registries.PARTICLE_TYPE.stream()
			.map(Registries.PARTICLE_TYPE::getId)
			.collect(Collectors.toList());

		suggestionWidget = SuggestionListWidget.forTextFieldWithStaticSuggestions(particleId, client.textRenderer, validParticles, Identifier::toString, this);

		particleId.setChangedListener((text) -> {
			suggestionWidget.updateSuggestions(validParticles, text, this);
		});
		// endregion

		// region Position
		positionMean = new Vec3FieldsWidget(
			width / 10, height / 2 - 60,
			(4 * width / 10) - 6, 20,
			this.client,
			blockEntity.position.mean()
		);

		this.addDrawableChild(positionMean);

		positionStdDev = new Vec3FieldsWidget(
			width / 10 + (4 * width / 10) + 6, height / 2 - 60,
			(4 * width / 10) - 6, 20,
			this.client,
			blockEntity.position.stdDev()
		);

		this.addDrawableChild(positionStdDev);
		// endregion

		// region Velocity
		velocityMean = new Vec3FieldsWidget(
			width / 10, (height / 2) - 10,
			(4 * width / 10) - 6, 20,
			this.client,
			blockEntity.velocity.mean()
		);

		this.addDrawableChild(velocityMean);

		velocityStdDev = new Vec3FieldsWidget(
			width / 10 + (4 * width / 10) + 6, (height / 2) - 10,
			(4 * width / 10) - 6, 20,
			this.client,
			blockEntity.velocity.stdDev()
		);

		this.addDrawableChild(velocityStdDev);
		// endregion

		// region Count
		countMean = new TextFieldWidget(
			this.client.textRenderer,
			width / 10, height / 2 + 40,
			(4 * width / 10) - 6, 20,
			Text.empty()
		);

		countMean.setText(String.valueOf(blockEntity.count.mean()));
		countMean.setTextPredicate(ParseUtil::canParseInt);

		this.addDrawableChild(countMean);

		countStdDev = new TextFieldWidget(
			this.client.textRenderer,
			width / 10 + (4 * width / 10) + 6, height / 2 + 40,
			(4 * width / 10) - 6, 20,
			Text.empty()
		);

		countStdDev.setText(String.valueOf(blockEntity.count.stdDev()));
		countStdDev.setTextPredicate(ParseUtil::canParseInt);

		this.addDrawableChild(countStdDev);
		// endregion

		// region Tick Rate
		tickRateMean = new TextFieldWidget(
			this.client.textRenderer,
			width / 10, height / 2 + 90,
			(4 * width / 10) - 6, 20,
			Text.empty()
		);

		tickRateMean.setText(String.valueOf(blockEntity.tickRate.mean()));
		tickRateMean.setTextPredicate(ParseUtil::canParseInt);

		this.addDrawableChild(tickRateMean);

		tickRateStdDev = new TextFieldWidget(
			this.client.textRenderer,
			width / 10 + (4 * width / 10) + 6, height / 2 + 90,
			(4 * width / 10) - 6, 20,
			Text.empty()
		);

		tickRateStdDev.setText(String.valueOf(blockEntity.tickRate.stdDev()));
		tickRateStdDev.setTextPredicate(ParseUtil::canParseInt);

		this.addDrawableChild(tickRateStdDev);
		// endregion
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		Objects.requireNonNull(this.client);

		context.drawTextWithShadow(
			client.textRenderer,
			Text.translatable("gui.glowcase.position_mean"),
			width / 10, (height / 2 - 60) - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			client.textRenderer,
			Text.translatable("gui.glowcase.position_std_dev"),
			width / 10 + (4 * width / 10) + 6, (height / 2 - 60) - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			client.textRenderer,
			Text.translatable("gui.glowcase.velocity_mean"),
			width / 10, (height / 2 - 10) - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			client.textRenderer,
			Text.translatable("gui.glowcase.velocity_std_dev"),
			width / 10 + (4 * width / 10) + 6, (height / 2 - 10) - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			client.textRenderer,
			Text.translatable("gui.glowcase.count_mean"),
			width / 10, (height / 2 + 40) - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			client.textRenderer,
			Text.translatable("gui.glowcase.count_std_dev"),
			width / 10 + (4 * width / 10) + 6, (height / 2 + 40) - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			client.textRenderer,
			Text.translatable("gui.glowcase.tick_rate_mean"),
			width / 10, (height / 2 + 90) - 20,
			0xFFFFFFFF
		);

		context.drawTextWithShadow(
			client.textRenderer,
			Text.translatable("gui.glowcase.tick_rate_std_dev"),
			width / 10 + (4 * width / 10) + 6, (height / 2 + 90) - 20,
			0xFFFFFFFF
		);

		// render the list over everything
		suggestionWidget.renderWidget(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (suggestionWidget.isMouseOver(mouseX, mouseY) && particleId.isFocused()) {
			suggestionWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (suggestionWidget.isMouseOver(mouseX, mouseY) && particleId.isFocused()) {
			return suggestionWidget.mouseClicked(mouseX, mouseY, button);
		} else {
			suggestionWidget.updateSuggestions(new ArrayList<>(), "", this);
		}

		return super.mouseClicked(mouseX, mouseY, button);
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
		setParticle();

		blockEntity.position = new DeviatedVec3d(positionMean.value(), positionStdDev.value());
		blockEntity.velocity = new DeviatedVec3d(velocityMean.value(), velocityStdDev.value());

		blockEntity.count = new DeviatedInteger(
			ParseUtil.parseOrDefault(countMean.getText(), blockEntity.count.mean()),
			ParseUtil.parseOrDefault(countStdDev.getText(), blockEntity.count.stdDev())
		);

		blockEntity.tickRate = new DeviatedInteger(
			ParseUtil.parseOrDefault(tickRateMean.getText(), blockEntity.tickRate.mean()),
			ParseUtil.parseOrDefault(tickRateStdDev.getText(), blockEntity.tickRate.stdDev())
		);

		C2SEditParticleDisplayBlock.of(blockEntity).send();
		super.close();
	}

	@SuppressWarnings("unchecked")
	private void setParticle() {
		Objects.requireNonNull(this.client);

		String idText = particleId.getText();

		int paramStart = idText.indexOf('{');

		Identifier id = Identifier.tryParse(
			paramStart == -1 ? idText : idText.substring(0, paramStart));
		if (id == null) return;

		RegistryWrapper.WrapperLookup lookup = Objects.requireNonNull(this.client.world).getRegistryManager();

		RegistryKey<ParticleType<?>> key = RegistryKey.of(RegistryKeys.PARTICLE_TYPE, id);

		Optional<RegistryEntry.Reference<ParticleType<?>>> optionalType = lookup.getOrThrow(RegistryKeys.PARTICLE_TYPE).getOptional(key);
		if (optionalType.isEmpty()) return;

		ParticleType<ParticleEffect> type = (ParticleType<ParticleEffect>) optionalType.get().value();

		NbtCompound nbtCompound;
		try {
			nbtCompound = paramStart == -1 ? new NbtCompound() : StringNbtReader.readCompound(idText.substring(paramStart));
		} catch (CommandSyntaxException e) {
			return;
		}


		DataResult<ParticleEffect> effect = type.getCodec().codec().parse(lookup.getOps(NbtOps.INSTANCE), nbtCompound);

		if (effect.result().isEmpty()) return;

		blockEntity.particle = effect.result().get();
	}

	@SuppressWarnings("unchecked")
	private <T extends ParticleEffect> NbtElement effectToTag(T effect, DynamicOps<NbtElement> ops) {
		Codec<T> codec = (Codec<T>) effect.getType().getCodec().codec();
		return codec.encodeStart(ops, effect).getOrThrow();
	}
}
