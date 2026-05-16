package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.DisplayBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditItemDisplayBlock;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class ItemDisplayEditScreen extends DisplayBlockEditScreen {
	private Checkbox renderAsBlockWidget;

	public ItemDisplayEditScreen(DisplayBlockEntity displayBlock) {
		super(displayBlock);
	}

	@Override
    public void init() {
        super.init();

        this.renderAsBlockWidget = Checkbox.builder(Component.translatable("gui.glowcase.render_as_block"), this.minecraft.font)
			.selected(this.blockEntity.getRenderAsBlock())
			.onValueChange((checkbox, checked) -> this.blockEntity.setRenderAsBlock(checked))
			.pos(20, 197)
			.build();
        
        this.addRenderableWidget(this.renderAsBlockWidget);
    }

	@Override
	public CustomPacketPayload getUpdatePayload() {
		return C2SEditItemDisplayBlock.of(blockEntity);
	}
}
