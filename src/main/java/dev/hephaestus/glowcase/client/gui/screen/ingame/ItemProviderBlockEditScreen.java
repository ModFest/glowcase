package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Longs;
import dev.hephaestus.glowcase.block.entity.ItemProviderBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditItemProviderBlock;
import dev.hephaestus.glowcase.util.TextUtils;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

public class ItemProviderBlockEditScreen extends GlowcaseScreen {

	private final ItemProviderBlockEntity providerBlock;
	private ButtonWidget givesItemButton;
	private TextFieldWidget cooldownWidget;
	private TextWidget secondsLabel;
	public ItemProviderBlockEditScreen(ItemProviderBlockEntity providerBlock) {
		this.providerBlock = providerBlock;
	}

	@Override
	public void init() {
		super.init();

		if (this.client != null) {
			this.givesItemButton = ButtonWidget.builder(Text.stringifiedTranslatable("gui.glowcase.gives_item", this.providerBlock.getGivesItem()), (action) -> {
				this.providerBlock.cycleGiveType();
				this.givesItemButton.setMessage(Text.stringifiedTranslatable("gui.glowcase.gives_item", this.providerBlock.getGivesItem()));
				this.cooldownWidget.setVisible(this.providerBlock.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED);
				this.secondsLabel.visible = this.providerBlock.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED;
				if (this.providerBlock.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED && (this.cooldownWidget.getText().isBlank() || this.cooldownWidget.getText().equals("0"))) this.cooldownWidget.setText(String.valueOf(60));
			}).dimensions(width / 2 - 75, height / 2 - 25, 150, 20).build();

			this.cooldownWidget = new TextFieldWidget(this.textRenderer, width / 2 - 30, height / 2 + 5, 60, 20, Text.empty());
			this.cooldownWidget.setText(this.providerBlock.cooldown == 0 ? "" : String.valueOf(this.providerBlock.cooldown));
			this.cooldownWidget.setPlaceholder(TextUtils.placeholder("gui.glowcase.cooldown"));
			this.cooldownWidget.setTextPredicate(s -> s.matches("\\d*"));
			this.cooldownWidget.setVisible(this.providerBlock.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED);

			this.secondsLabel = new TextWidget(width / 2 + 30, height / 2 + 5, 10, 20, Text.of("s"), this.textRenderer);
			this.secondsLabel.visible = this.providerBlock.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED;

			this.addDrawableChild(this.givesItemButton);
			this.addDrawableChild(this.cooldownWidget);
			this.addDrawableChild(this.secondsLabel);
		}
	}

	@Override
	public void close() {
		if (this.providerBlock.getGivesItem() != ItemProviderBlockEntity.GivesItem.TIMED) {
			this.providerBlock.cooldown = 0;
		} else if (Longs.tryParse(cooldownWidget.getText()) instanceof Long l) {
			this.providerBlock.cooldown = Math.clamp(l, 0, 172800000 /* 48 Hours */);
		}

		C2SEditItemProviderBlock.of(providerBlock).send();
		super.close();
	}
}
