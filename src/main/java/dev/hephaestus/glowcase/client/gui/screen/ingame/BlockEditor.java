package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.GlowcaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * @author Ampflower
 */
public interface BlockEditor<E extends GlowcaseBlockEntity> extends Editor {
	E getBlockEntity();

	default boolean contextMatches(ResourceKey<Level> dimension, BlockPos pos) {
		final Level level = this.getBlockEntity().getLevel();
		return level != null
			   && level.dimension().equals(dimension)
			   && this.getBlockEntity().getBlockPos().equals(pos);
	}
}
