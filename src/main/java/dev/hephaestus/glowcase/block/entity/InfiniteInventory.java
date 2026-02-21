package dev.hephaestus.glowcase.block.entity;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface InfiniteInventory extends Container {
	ItemStack getStack();

	default boolean hasItem() {
		return getStack() != null && !getStack().isEmpty();
	}

	@Override
	default int getContainerSize() {
		return 1;
	}

	@Override
	default boolean isEmpty() {
		return getStack().isEmpty();
	}

	@Override
	default ItemStack getItem(int slot) {
		return getStack().copyWithCount(1);
	}

	@Override
	default ItemStack removeItem(int slot, int amount) {
		return getStack().copyWithCount(1);
	}

	@Override
	default ItemStack removeItemNoUpdate(int slot) {
		return getStack().copyWithCount(1);
	}

	@Override
	default void setItem(int slot, ItemStack stack) {
	}

	@Override
	default boolean stillValid(Player player) {
		return false;
	}

	@Override
	default void clearContent() {
	}
}
