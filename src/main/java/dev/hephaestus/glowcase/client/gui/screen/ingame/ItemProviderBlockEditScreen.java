package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Longs;
import dev.hephaestus.glowcase.block.entity.ItemProviderBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.packet.C2SEditItemProviderBlock;
import dev.hephaestus.glowcase.util.InputFilters;
import dev.hephaestus.glowcase.util.TextUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;

public class ItemProviderBlockEditScreen extends GlowcaseScreen {

	private final ItemProviderBlockEntity providerBlock;
	private Button givesItemButton;
	private GlowcaseEditBox cooldownWidget;
	private StringWidget secondsLabel;
	public ItemProviderBlockEditScreen(ItemProviderBlockEntity providerBlock) {
		this.providerBlock = providerBlock;
	}

	@Override
	public void init() {
		super.init();

		this.givesItemButton = Button.builder(Component.translatableEscape("gui.glowcase.gives_item", this.providerBlock.getGivesItem()), (action) -> {
			this.providerBlock.cycleGiveType();
			this.givesItemButton.setMessage(Component.translatableEscape("gui.glowcase.gives_item", this.providerBlock.getGivesItem()));
			this.cooldownWidget.setVisible(this.providerBlock.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED);
			this.secondsLabel.visible = this.providerBlock.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED;
			if (this.providerBlock.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED && (this.cooldownWidget.getValue().isBlank() || this.cooldownWidget.getValue().equals("0")))
				this.cooldownWidget.setValue(String.valueOf(60));
		}).bounds(width / 2 - 75, height / 2 - 25, 150, 20).build();

		this.cooldownWidget = new GlowcaseEditBox(this.font, width / 2 - 30, height / 2 + 5, 60, 20, Component.empty());
		this.cooldownWidget.setValue(this.providerBlock.cooldown == 0 ? "" : String.valueOf(this.providerBlock.cooldown));
		this.cooldownWidget.setHint(TextUtils.placeholder("gui.glowcase.cooldown"));
		this.cooldownWidget.setFilter(InputFilters::naturalNumber);
		this.cooldownWidget.setVisible(this.providerBlock.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED);

		this.secondsLabel = new StringWidget(width / 2 + 30, height / 2 + 5, 10, 20, Component.nullToEmpty("s"), this.font);
		this.secondsLabel.visible = this.providerBlock.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED;

		this.addRenderableWidget(this.givesItemButton);
		this.addRenderableWidget(this.cooldownWidget);
		this.addRenderableWidget(this.secondsLabel);
	}

	@Override
	public void onClose() {
		if (this.providerBlock.getGivesItem() != ItemProviderBlockEntity.GivesItem.TIMED) {
			this.providerBlock.cooldown = 0;
		} else if (Longs.tryParse(cooldownWidget.getValue()) instanceof Long l) {
			this.providerBlock.cooldown = Math.clamp(l, 0, 172800000 /* 48 Hours */);
		}

		C2SEditItemProviderBlock.of(providerBlock).send();
		super.onClose();
	}
}
