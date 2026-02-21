package dev.hephaestus.glowcase.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;

public record CollectableStack(Holder<Item> item, DataComponentPatch changes, int count, boolean collected) {
	public static final Codec<CollectableStack> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Item.CODEC.fieldOf("item").forGetter(CollectableStack::item),
			DataComponentPatch.CODEC.fieldOf("components").forGetter(CollectableStack::changes),
			Codec.INT.fieldOf("count").forGetter(CollectableStack::count),
			Codec.BOOL.fieldOf("collected").forGetter(CollectableStack::collected)
		).apply(instance, CollectableStack::new)
	);

	public ItemStack getStack() {
		return new ItemStack(item, count, changes);
	}

	public MutableComponent getCollectableName(HolderLookup.Provider lookup, boolean selected) {
		ItemStack stack = getStack();
		Component name = stack.getHoverName();
		JukeboxPlayable songComponent = stack.get(DataComponents.JUKEBOX_PLAYABLE);
		if (songComponent != null) {
			JukeboxSong song = songComponent.song().value();
			if (song != null) {
				name = song.description();
			}
		}
		return Component.literal("%s %dx ".formatted(selected ? ">" : "-", count)).append(name).withStyle(collected ? ChatFormatting.AQUA : ChatFormatting.GRAY).withStyle(selected ? s -> s.withBold(true) : s -> s);
	}

	public CollectableStack asCollected() {
		return new CollectableStack(item, changes, count, true);
	}

	public CollectableStack asRetrieved() {
		return new CollectableStack(item, changes, count, false);
	}
}
