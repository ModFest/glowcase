package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.hephaestus.glowcase.block.entity.ParticleDisplayBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseTextFieldWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.Vec3FieldsWidget;
import dev.hephaestus.glowcase.packet.C2SEditParticleDisplayBlock;
import dev.hephaestus.glowcase.util.DeviatedInteger;
import dev.hephaestus.glowcase.util.DeviatedVec3d;
import dev.hephaestus.glowcase.util.InputFilters;
import dev.hephaestus.glowcase.util.ParseUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ParticleDisplayEditScreen extends GlowcaseScreen {
	private final ParticleDisplayBlockEntity blockEntity;
	private EditBox particleId;

	private Vec3FieldsWidget positionMean;
	private Vec3FieldsWidget positionStdDev;

	private Vec3FieldsWidget velocityMean;
	private Vec3FieldsWidget velocityStdDev;

	private EditBox countMean;
	private EditBox countStdDev;

	private EditBox tickRateMean;
	private EditBox tickRateStdDev;

	private SuggestionListWidget<Identifier> suggestionWidget;
	private List<Identifier> validParticles = new ArrayList<>();

	public ParticleDisplayEditScreen(ParticleDisplayBlockEntity blockEntity) {
		this.blockEntity = blockEntity;
	}

	@Override
	protected void init() {
		super.init();
		Objects.requireNonNull(this.minecraft);
		HolderLookup.Provider lookup = Objects.requireNonNull(minecraft.level).registryAccess();


		// region Particle ID
		particleId = new GlowcaseTextFieldWidget(
			this.minecraft.font,
			width / 10, height / 2 - 110,
			8 * width / 10, 20,
			Component.empty()
		);

		particleId.setMaxLength(9999);

		String optionsString = effectToTag(blockEntity.particle, lookup.createSerializationContext(NbtOps.INSTANCE)).toString();
		if (optionsString.equals("{}")) optionsString = "";

		particleId.setValue(BuiltInRegistries.PARTICLE_TYPE.getKey(blockEntity.particle.getType()) + optionsString);

		this.addRenderableWidget(particleId);

		validParticles = BuiltInRegistries.PARTICLE_TYPE.stream()
			.map(BuiltInRegistries.PARTICLE_TYPE::getKey)
			.collect(Collectors.toList());

		suggestionWidget = SuggestionListWidget.forTextFieldWithStaticSuggestions(particleId, minecraft.font, validParticles, Identifier::toString, this);

		particleId.setResponder((text) -> suggestionWidget.updateSuggestions(validParticles, text, this));
		// endregion

		// region Position
		positionMean = new Vec3FieldsWidget(
			width / 10, height / 2 - 60,
			(4 * width / 10) - 6, 20,
			this.minecraft,
			blockEntity.position.mean()
		);

		this.addRenderableWidget(positionMean);

		positionStdDev = new Vec3FieldsWidget(
			width / 10 + (4 * width / 10) + 6, height / 2 - 60,
			(4 * width / 10) - 6, 20,
			this.minecraft,
			blockEntity.position.stdDev()
		);

		this.addRenderableWidget(positionStdDev);
		// endregion

		// region Velocity
		velocityMean = new Vec3FieldsWidget(
			width / 10, (height / 2) - 10,
			(4 * width / 10) - 6, 20,
			this.minecraft,
			blockEntity.velocity.mean()
		);

		this.addRenderableWidget(velocityMean);

		velocityStdDev = new Vec3FieldsWidget(
			width / 10 + (4 * width / 10) + 6, (height / 2) - 10,
			(4 * width / 10) - 6, 20,
			this.minecraft,
			blockEntity.velocity.stdDev()
		);

		this.addRenderableWidget(velocityStdDev);
		// endregion

		// region Count
		countMean = new EditBox(
			this.minecraft.font,
			width / 10, height / 2 + 40,
			(4 * width / 10) - 6, 20,
			Component.empty()
		);

		countMean.setValue(String.valueOf(blockEntity.count.mean()));
		//FIXME 26.1
//		countMean.setFilter(ParseUtil::canParseInt);

		this.addRenderableWidget(countMean);

		countStdDev = new EditBox(
			this.minecraft.font,
			width / 10 + (4 * width / 10) + 6, height / 2 + 40,
			(4 * width / 10) - 6, 20,
			Component.empty()
		);

		countStdDev.setValue(String.valueOf(blockEntity.count.stdDev()));
		//FIXME 26.1
//		countStdDev.setFilter(ParseUtil::canParseInt);

		this.addRenderableWidget(countStdDev);
		// endregion

		// region Tick Rate
		tickRateMean = new EditBox(
			this.minecraft.font,
			width / 10, height / 2 + 90,
			(4 * width / 10) - 6, 20,
			Component.empty()
		);

		tickRateMean.setValue(String.valueOf(blockEntity.tickRate.mean()));
		//FIXME 26.1
//		tickRateMean.setFilter(ParseUtil::canParseInt);

		this.addRenderableWidget(tickRateMean);

		tickRateStdDev = new EditBox(
			this.minecraft.font,
			width / 10 + (4 * width / 10) + 6, height / 2 + 90,
			(4 * width / 10) - 6, 20,
			Component.empty()
		);

		tickRateStdDev.setValue(String.valueOf(blockEntity.tickRate.stdDev()));
		//FIXME 26.1
//		tickRateStdDev.setFilter(ParseUtil::canParseInt);

		this.addRenderableWidget(tickRateStdDev);
		// endregion
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		Objects.requireNonNull(this.minecraft);

		graphics.text(
			minecraft.font,
			Component.translatable("gui.glowcase.position_mean"),
			width / 10, (height / 2 - 60) - 20,
			0xFFFFFFFF
		);

		graphics.text(
			minecraft.font,
			Component.translatable("gui.glowcase.position_std_dev"),
			width / 10 + (4 * width / 10) + 6, (height / 2 - 60) - 20,
			0xFFFFFFFF
		);

		graphics.text(
			minecraft.font,
			Component.translatable("gui.glowcase.velocity_mean"),
			width / 10, (height / 2 - 10) - 20,
			0xFFFFFFFF
		);

		graphics.text(
			minecraft.font,
			Component.translatable("gui.glowcase.velocity_std_dev"),
			width / 10 + (4 * width / 10) + 6, (height / 2 - 10) - 20,
			0xFFFFFFFF
		);

		graphics.text(
			minecraft.font,
			Component.translatable("gui.glowcase.count_mean"),
			width / 10, (height / 2 + 40) - 20,
			0xFFFFFFFF
		);

		graphics.text(
			minecraft.font,
			Component.translatable("gui.glowcase.count_std_dev"),
			width / 10 + (4 * width / 10) + 6, (height / 2 + 40) - 20,
			0xFFFFFFFF
		);

		graphics.text(
			minecraft.font,
			Component.translatable("gui.glowcase.tick_rate_mean"),
			width / 10, (height / 2 + 90) - 20,
			0xFFFFFFFF
		);

		graphics.text(
			minecraft.font,
			Component.translatable("gui.glowcase.tick_rate_std_dev"),
			width / 10 + (4 * width / 10) + 6, (height / 2 + 90) - 20,
			0xFFFFFFFF
		);

		// render the list over everything
		suggestionWidget.extractRenderState(graphics, mouseX, mouseY, delta);
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
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (suggestionWidget.draggingScrollbar) {
			if (suggestionWidget.mouseDragged(event, dx, dy))
				return true;
		}

		return super.mouseDragged(event, dx, dy);
	}
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (suggestionWidget.isMouseOver(mouseX, mouseY) && particleId.isFocused()) {
			return suggestionWidget.mouseClicked(event, doubleClick);
		} else {
			suggestionWidget.updateSuggestions(new ArrayList<>(), "", this);
		}

		return super.mouseClicked(event, doubleClick);
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
		setParticle();

		blockEntity.position = new DeviatedVec3d(positionMean.value(), positionStdDev.value());
		blockEntity.velocity = new DeviatedVec3d(velocityMean.value(), velocityStdDev.value());

		blockEntity.count = new DeviatedInteger(
			ParseUtil.parseOrDefault(countMean.getValue(), blockEntity.count.mean()),
			ParseUtil.parseOrDefault(countStdDev.getValue(), blockEntity.count.stdDev())
		);

		blockEntity.tickRate = new DeviatedInteger(
			ParseUtil.parseOrDefault(tickRateMean.getValue(), blockEntity.tickRate.mean()),
			ParseUtil.parseOrDefault(tickRateStdDev.getValue(), blockEntity.tickRate.stdDev())
		);

		C2SEditParticleDisplayBlock.of(blockEntity).send();
		super.onClose();
	}

	@SuppressWarnings("unchecked")
	private void setParticle() {
		Objects.requireNonNull(this.minecraft);

		String idText = particleId.getValue();

		int paramStart = idText.indexOf('{');

		Identifier id = Identifier.tryParse(
			paramStart == -1 ? idText : idText.substring(0, paramStart));
		if (id == null) return;

		HolderLookup.Provider lookup = Objects.requireNonNull(this.minecraft.level).registryAccess();

		ResourceKey<ParticleType<?>> key = ResourceKey.create(Registries.PARTICLE_TYPE, id);

		Optional<Holder.Reference<ParticleType<?>>> optionalType = lookup.lookupOrThrow(Registries.PARTICLE_TYPE).get(key);
		if (optionalType.isEmpty()) return;

		ParticleType<ParticleOptions> type = (ParticleType<ParticleOptions>) optionalType.get().value();

		CompoundTag nbtCompound;
		try {
			nbtCompound = paramStart == -1 ? new CompoundTag() : TagParser.parseCompoundFully(idText.substring(paramStart));
		} catch (CommandSyntaxException e) {
			return;
		}


		DataResult<ParticleOptions> effect = type.codec().codec().parse(lookup.createSerializationContext(NbtOps.INSTANCE), nbtCompound);

		if (effect.result().isEmpty()) return;

		blockEntity.particle = effect.result().get();
	}

	@SuppressWarnings("unchecked")
	private <T extends ParticleOptions> Tag effectToTag(T effect, DynamicOps<Tag> ops) {
		Codec<T> codec = (Codec<T>) effect.getType().codec().codec();
		return codec.encodeStart(ops, effect).getOrThrow();
	}
}
