package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Ints;
import dev.hephaestus.glowcase.block.entity.ItemAcceptorBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditItemAcceptorBlock;
import dev.hephaestus.glowcase.util.TextUtils;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ItemAcceptorBlockEditScreen extends GlowcaseScreen {
	private final ItemAcceptorBlockEntity itemAcceptorBlockEntity;

	private TextFieldWidget itemWidget;
	private TextFieldWidget countWidget;
	private TextFieldWidget pulseWidget;
	private ButtonWidget outputDirectionToggle;

	public ItemAcceptorBlockEditScreen(ItemAcceptorBlockEntity itemAcceptorBlockEntity) {
		this.itemAcceptorBlockEntity = itemAcceptorBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		if (this.client == null) return;

		Identifier item = this.itemAcceptorBlockEntity.getItem();

		this.itemWidget = new TextFieldWidget(this.textRenderer, width / 2 - 100, height / 2 - 25, 150, 20, Text.empty());
		this.itemWidget.setMaxLength(128);
		if (!item.equals(Identifier.ofVanilla("air"))) {
			this.itemWidget.setText((this.itemAcceptorBlockEntity.isItemTag ? "#" : "") + item);
		}
		this.itemWidget.setPlaceholder(TextUtils.placeholder("gui.glowcase.item_or_tag"));
		this.itemWidget.setTextPredicate(s -> s.matches("#?[a-z0-9_.-]*:?[a-z0-9_./-]*"));

		this.countWidget = new TextFieldWidget(this.textRenderer, width / 2 + 60, height / 2 - 25, 40, 20, Text.empty());
		this.countWidget.setText(String.valueOf(this.itemAcceptorBlockEntity.count));
		this.countWidget.setPlaceholder(TextUtils.placeholder("gui.glowcase.count"));
		this.countWidget.setTextPredicate(s -> s.matches("\\d*"));

		this.outputDirectionToggle = ButtonWidget.builder(Text.translatable("gui.glowcase.output_direction", this.itemAcceptorBlockEntity.outputDirection.toString()), action -> {
			switch (itemAcceptorBlockEntity.outputDirection) {
				case TOP -> itemAcceptorBlockEntity.outputDirection = ItemAcceptorBlockEntity.OutputDirection.BACK;
				case BACK -> itemAcceptorBlockEntity.outputDirection = ItemAcceptorBlockEntity.OutputDirection.BOTTOM;
				case BOTTOM -> itemAcceptorBlockEntity.outputDirection = ItemAcceptorBlockEntity.OutputDirection.TOP;
			}

			this.outputDirectionToggle.setMessage(Text.translatable("gui.glowcase.output_direction", this.itemAcceptorBlockEntity.outputDirection.toString()));
		}).dimensions(width / 2 - 100, height / 2 + 5, 150, 20).build();

		this.pulseWidget = new TextFieldWidget(this.textRenderer, width / 2 + 60, height / 2 + 5, 40, 20, Text.empty());
		this.pulseWidget.setText(String.valueOf(this.itemAcceptorBlockEntity.pulse));
		this.pulseWidget.setPlaceholder(TextUtils.placeholder("gui.glowcase.pulse"));
		this.pulseWidget.setTextPredicate(s -> s.matches("\\d*"));

		this.addDrawableChild(this.itemWidget);
		this.addDrawableChild(this.countWidget);
		this.addDrawableChild(this.outputDirectionToggle);
		this.addDrawableChild(this.pulseWidget);
		this.addDrawableChild(new TextWidget(width / 2 + 50, height / 2 - 25, 10, 20, Text.of("x"), textRenderer));
		this.addDrawableChild(new TextWidget(width / 2 + 50, height / 2 + 5, 10, 20, Text.of("x"), textRenderer));
	}

	@Override
	public void close() {
		String text = itemWidget.getText();
		boolean isItemTag = text.startsWith("#");
		if (isItemTag) {
			text = text.substring(1);
		}

		if (!text.isEmpty() && Identifier.tryParse(text) instanceof Identifier id) {
			this.itemAcceptorBlockEntity.setItem(id);
			this.itemAcceptorBlockEntity.isItemTag = isItemTag;
		} else {
			this.itemAcceptorBlockEntity.setItem(Identifier.ofVanilla("air"));
		}

		if (Ints.tryParse(countWidget.getText()) instanceof Integer integer) {
			this.itemAcceptorBlockEntity.count = Math.max(0, integer);
		}

		if (Ints.tryParse(pulseWidget.getText()) instanceof Integer integer) {
			this.itemAcceptorBlockEntity.pulse = Math.max(0, integer);
		}

		C2SEditItemAcceptorBlock.of(this.itemAcceptorBlockEntity).send();
		super.close();
	}
}
