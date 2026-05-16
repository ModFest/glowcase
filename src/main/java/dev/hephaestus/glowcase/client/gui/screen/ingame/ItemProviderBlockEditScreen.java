package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Longs;
import dev.hephaestus.glowcase.block.entity.ItemProviderBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.packet.C2SEditItemProviderBlock;
import dev.hephaestus.glowcase.util.InputFilters;
import dev.hephaestus.glowcase.util.TextUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

public class ItemProviderBlockEditScreen extends BlockEditorScreen<ItemProviderBlockEntity> {
	private Button givesItemButton;
	private GlowcaseEditBox cooldownWidget;
	private StringWidget secondsLabel;

	public ItemProviderBlockEditScreen(ItemProviderBlockEntity blockEntity) {
		super(blockEntity);
	}

	@Override
	public void init() {
		super.init();

		this.givesItemButton = Button.builder(Component.translatableEscape(
			"gui.glowcase.gives_item",
			this.blockEntity.getGivesItem()
		), (action) -> {
			this.blockEntity.cycleGiveType();
			this.givesItemButton.setMessage(Component.translatableEscape(
				"gui.glowcase.gives_item",
				this.blockEntity.getGivesItem()
			));
			this.cooldownWidget.setVisible(this.blockEntity.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED);
			this.secondsLabel.visible = this.blockEntity.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED;
			if (this.blockEntity.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED && (this.cooldownWidget.getValue()
																								   .isBlank() || this.cooldownWidget.getValue()
																								   .equals("0")))
				this.cooldownWidget.setValue(String.valueOf(60));
		}).bounds(width / 2 - 75, height / 2 - 25, 150, 20).build();

		this.cooldownWidget = new GlowcaseEditBox(this.font, width / 2 - 30, height / 2 + 5, 60, 20, Component.empty());
		this.cooldownWidget.setValue(this.blockEntity.cooldown == 0 ? "" : String.valueOf(this.blockEntity.cooldown));
		this.cooldownWidget.setHint(TextUtils.placeholder("gui.glowcase.cooldown"));
		this.cooldownWidget.setFilter(InputFilters::naturalNumber);
		this.cooldownWidget.setVisible(this.blockEntity.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED);

		this.secondsLabel = new StringWidget(width / 2 + 30, height / 2 + 5, 10, 20, Component.nullToEmpty("s"), this.font);
		this.secondsLabel.visible = this.blockEntity.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED;

		this.addRenderableWidget(this.givesItemButton);
		this.addRenderableWidget(this.cooldownWidget);
		this.addRenderableWidget(this.secondsLabel);
	}

	@Override
	public @Nullable CustomPacketPayload getUpdatePayload() {
		if (this.blockEntity.getGivesItem() != ItemProviderBlockEntity.GivesItem.TIMED) {
			this.blockEntity.cooldown = 0;
		} else if (Longs.tryParse(cooldownWidget.getValue()) instanceof Long l) {
			this.blockEntity.cooldown = Math.clamp(l, 0, 172800000 /* 48 Hours */);
		}

		return C2SEditItemProviderBlock.of(blockEntity);
	}
}
