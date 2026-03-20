package dev.hephaestus.glowcase.block.entity;

import net.minecraft.world.item.ItemStack;

public interface StackInteractable {
	boolean matchesStack(ItemStack stack);
	void setFromStack(ItemStack stack);
	void unsetFromStack();
}
