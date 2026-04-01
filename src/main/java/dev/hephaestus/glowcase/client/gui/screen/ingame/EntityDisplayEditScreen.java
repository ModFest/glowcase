package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.DisplayBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditEntityDisplayBlock;

public class EntityDisplayEditScreen extends DisplayBlockEditScreen {
	public EntityDisplayEditScreen(DisplayBlockEntity displayBlock) {
		super(displayBlock);
	}

	@Override
	protected void editDisplayBlock() {
		C2SEditEntityDisplayBlock.of(displayBlock).send();
	}
}
