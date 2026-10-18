package dev.hephaestus.glowcase.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hephaestus.glowcase.util.TextJustify;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.List;
import java.util.Optional;

public record NoteComponent(
	List<Component> lines,
	TextJustify alignment,
	Optional<String> title,
	Optional<String> author
) {
	public static final int LINES_LIMIT = 10;
	public static final int TITLE_LIMIT = 32;

	public static final Codec<NoteComponent> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			ComponentSerialization.CODEC.sizeLimitedListOf(LINES_LIMIT).fieldOf("lines").forGetter(NoteComponent::lines),
			TextJustify.LEGACY_CODEC.fieldOf("alignment").forGetter(NoteComponent::alignment),
			Codec.STRING.optionalFieldOf("title").forGetter(NoteComponent::title),
			Codec.STRING.optionalFieldOf("author").forGetter(NoteComponent::author)
		).apply(instance, NoteComponent::new)
	);

	public static final DataComponentType<NoteComponent> TYPE = DataComponentType.<NoteComponent>builder()
		.persistent(CODEC)
		.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(CODEC))
		.build();
}
