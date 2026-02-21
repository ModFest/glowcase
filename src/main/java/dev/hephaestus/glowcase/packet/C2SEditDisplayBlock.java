package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.block.entity.DisplayBlockEntity;
import dev.hephaestus.glowcase.util.DisplayBlockSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface C2SEditDisplayBlock {
	DisplayBlockSettings settings();

	default void receive(ServerLevel world, BlockEntity blockEntity) {
		if ((blockEntity instanceof DisplayBlockEntity be)) {
			be.loadSettings(settings());
		}
	}
}
