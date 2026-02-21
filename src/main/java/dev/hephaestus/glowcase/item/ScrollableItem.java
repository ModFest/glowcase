package dev.hephaestus.glowcase.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface ScrollableItem {
	void scroll(ItemStack caseStack, Player player, int amount);
}
