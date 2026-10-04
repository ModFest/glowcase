package dev.hephaestus.glowcase.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;

import dev.hephaestus.glowcase.block.TextBlock;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;

public record NoteComponent(List<Component> lines, Alignment alignment, Optional<String> title, Optional<String> author) {
	public static final int LINES_LIMIT = 10;
	public static final int TITLE_LIMIT = 32;

	public static final Codec<NoteComponent> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			ComponentSerialization.CODEC.sizeLimitedListOf(LINES_LIMIT).fieldOf("lines").forGetter(NoteComponent::lines),
			Codec.BYTE.fieldOf("alignment").forGetter((note) -> (byte) note.alignment.ordinal()),
			Codec.STRING.optionalFieldOf("title").forGetter(NoteComponent::title),
			Codec.STRING.optionalFieldOf("author").forGetter(NoteComponent::author)
		).apply(instance, (lines, alignment, title, author)
			-> new NoteComponent(lines, Alignment.values()[alignment], title, author))
	);

	public static final DataComponentType<NoteComponent> TYPE = DataComponentType.<NoteComponent>builder()
		.persistent(CODEC)
		.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(CODEC))
		.build();

	public enum Alignment {
		LEFT, CENTER, RIGHT;
	}
}
