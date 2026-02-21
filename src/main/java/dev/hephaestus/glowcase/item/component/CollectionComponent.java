package dev.hephaestus.glowcase.item.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.util.CollectableStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

public record CollectionComponent(ImmutableList<CollectableStack> collectables, int selected) {
	public static final Codec<CollectionComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.list(CollectableStack.CODEC).fieldOf("collectables").forGetter(CollectionComponent::collectables),
		Codec.INT.fieldOf("selected").forGetter(CollectionComponent::selected)
	).apply(instance, (c, s) -> new CollectionComponent(ImmutableList.copyOf(c), s)));
	public static final DataComponentType<CollectionComponent> TYPE = DataComponentType.<CollectionComponent>builder().persistent(CODEC).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(CODEC)).build();

	public CollectionComponent() {
		this(ImmutableList.of(), -1);
	}

	private ImmutableList<CollectableStack> alteredCollectables(Consumer<List<CollectableStack>> operation) {
		List<CollectableStack> mutableList = new ArrayList<>(collectables);
		operation.accept(mutableList);
		return ImmutableList.copyOf(mutableList);
	}

	private boolean hasSelection(int selected) {
		if (selected >= collectables.size() || selected < -1) {
			Glowcase.LOGGER.warn("Glowcase collection case has an out of bounds selection and is now stuck! Index was {} for size {}", selected, collectables.size());
			return false;
		}
		return selected != -1;
	}

	public boolean hasSelection() {
		return hasSelection(selected);
	}

	public ItemStack getSelectedCollectableStack() {
		return hasSelection() ? collectables.get(selected).getStack() : ItemStack.EMPTY;
	}

	public boolean isSelectedCollected() {
		return hasSelection() && collectables.get(selected).collected();
	}

	public CollectionComponent withStackAfterSelection(ItemStack stack) {
		if (stack.isEmpty()) return this;
		hasSelection();
		CollectableStack newCollectable = new CollectableStack(stack.getItemHolder(), stack.copy().getComponentsPatch(), stack.getCount(), false);
		return new CollectionComponent(alteredCollectables(l -> l.add(selected + 1, newCollectable)), selected + 1);
	}

	public CollectionComponent withoutSelectedStack() {
		if (!hasSelection()) return this;
		ImmutableList<CollectableStack> newCollection = alteredCollectables(l -> l.remove(selected));
		return new CollectionComponent(newCollection, selected == newCollection.size() ? selected - 1 : selected);
	}

	public int getCollectionIndex(ItemStack otherStack) {
		for (int i = 0; i < collectables.size(); i++) {
			CollectableStack collectable = collectables.get(i);
			if (!collectable.collected()) {
				ItemStack stackToCollect = collectable.getStack();
				if (ItemStack.isSameItemSameComponents(stackToCollect, otherStack) && stackToCollect.getCount() <= otherStack.getCount()) {
					return i;
				}
			}
		}
		return -1;
	}

	public CollectionComponent collectStack(int selected) {
		if (!hasSelection(selected)) return this;
		return new CollectionComponent(alteredCollectables(l -> l.set(selected, l.get(selected).asCollected())), selected);
	}

	private static int getPreviousCollected(ImmutableList<CollectableStack> collectables, int selected, boolean slotsAllowed) {
		if (collectables.isEmpty()) return -1;
		for (int i = (collectables.size() + selected - 1) % collectables.size(); i != selected && selected >= 0; i = (collectables.size() + i - 1) % collectables.size()) { // Wraparound Fori
			if (collectables.get(i).collected()) {
				return i;
			}
		}
		return selected >= 0 && collectables.get(selected).collected() ? selected : (slotsAllowed ? collectables.size() - 1 : -1);
	}

	private static int getNextCollected(ImmutableList<CollectableStack> collectables, int selected, boolean slotsAllowed) {
		if (collectables.isEmpty()) return -1;
		for (int i = (selected + 1) % collectables.size(); i != selected && selected >= 0; i = (i + 1) % collectables.size()) { // Wraparound Fori
			if (collectables.get(i).collected()) {
				return i;
			}
		}
		return selected >= 0 && collectables.get(selected).collected() ? selected : (slotsAllowed ? 0 : -1);
	}

	public CollectionComponent retrieveSelectedStack(boolean canSelectSlots) {
		if (!hasSelection()) return this;
		ImmutableList<CollectableStack> newCollection = alteredCollectables(l -> l.set(selected, l.get(selected).asRetrieved()));
		return new CollectionComponent(newCollection, getPreviousCollected(newCollection, selected, canSelectSlots));
	}

	public CollectionComponent selectNext(boolean collected) {
		if (collectables.isEmpty()) return this;
		return new CollectionComponent(collectables, collected ? getNextCollected(collectables, selected, false) : (selected + 1) % collectables.size());
	}

	public CollectionComponent selectPrevious(boolean collected) {
		if (collectables.isEmpty()) return this;
		return new CollectionComponent(collectables, collected ? getPreviousCollected(collectables, selected, false) : (collectables.size() + selected - 1) % collectables.size());
	}

	public int collected() {
		return (int) collectables.stream().filter(CollectableStack::collected).count();
	}
}