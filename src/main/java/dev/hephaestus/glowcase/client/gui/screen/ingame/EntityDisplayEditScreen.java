package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.DisplayBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditEntityDisplayBlock;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class EntityDisplayEditScreen extends DisplayBlockEditScreen {
	public EntityDisplayEditScreen(DisplayBlockEntity displayBlock) {
		super(displayBlock);
	}

	@Override
	public CustomPacketPayload getUpdatePayload() {
		return C2SEditEntityDisplayBlock.of(blockEntity);
	}
}
