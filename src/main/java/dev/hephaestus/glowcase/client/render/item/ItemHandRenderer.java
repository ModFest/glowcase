package dev.hephaestus.glowcase.client.render.item;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Called when the item is being held in hand.
 */
public abstract class ItemHandRenderer {
	/**
	 * Called on each frame when the player is holding the given stack.
	 */
	public abstract void render(PoseStack matrices, SubmitNodeCollector collector, int light, ItemStack stack);

	/**
	 * Whenever the item should be rendered or not.
	 * Returning false falls back to the default rendering method of the item.
	 */
	public boolean visible(ItemStack stack) {
		return true;
	}

	// End of class

	private static final Map<Item, ItemHandRenderer> RENDERER = Maps.newHashMap();

	public static void register(Item item, ItemHandRenderer factory) {
		RENDERER.put(item, factory);
	}

	/**
	 * Returns the item hand renderer of the given stack or null,
	 * if not found or if it should be skipped.
	 */
	public static @Nullable ItemHandRenderer getRenderer(ItemStack stack) {
		for (Item item : RENDERER.keySet())
			if (stack.is(item))
				return RENDERER.get(item);

		return null;
	}
}
