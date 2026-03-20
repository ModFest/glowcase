package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Ints;
import dev.hephaestus.glowcase.block.entity.ItemAcceptorBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditItemAcceptorBlock;
import dev.hephaestus.glowcase.util.TextUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ItemAcceptorBlockEditScreen extends GlowcaseScreen {
	private final ItemAcceptorBlockEntity itemAcceptorBlockEntity;

	private EditBox itemWidget;
	private EditBox countWidget;
	private EditBox pulseWidget;
	private Button outputDirectionToggle;

	public ItemAcceptorBlockEditScreen(ItemAcceptorBlockEntity itemAcceptorBlockEntity) {
		this.itemAcceptorBlockEntity = itemAcceptorBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		if (this.minecraft == null) return;

		Identifier item = this.itemAcceptorBlockEntity.getItem();

		this.itemWidget = new EditBox(this.font, width / 2 - 100, height / 2 - 25, 150, 20, Component.empty());
		this.itemWidget.setMaxLength(128);
		if (!item.equals(Identifier.withDefaultNamespace("air"))) {
			this.itemWidget.setValue((this.itemAcceptorBlockEntity.isItemTag ? "#" : "") + item);
		}
		this.itemWidget.setHint(TextUtils.placeholder("gui.glowcase.item_or_tag"));
//FIXME 26.1
		//		this.itemWidget.setFilter(s -> s.matches("#?[a-z0-9_.-]*:?[a-z0-9_./-]*"));

		this.countWidget = new EditBox(this.font, width / 2 + 60, height / 2 - 25, 40, 20, Component.empty());
		this.countWidget.setValue(String.valueOf(this.itemAcceptorBlockEntity.count));
		this.countWidget.setHint(TextUtils.placeholder("gui.glowcase.count"));
		//FIXME 26.1
//		this.countWidget.setFilter(s -> s.matches("\\d*"));

		this.outputDirectionToggle = Button.builder(Component.translatable("gui.glowcase.output_direction", this.itemAcceptorBlockEntity.outputDirection.toString()), action -> {
			switch (itemAcceptorBlockEntity.outputDirection) {
				case TOP -> itemAcceptorBlockEntity.outputDirection = ItemAcceptorBlockEntity.OutputDirection.BACK;
				case BACK -> itemAcceptorBlockEntity.outputDirection = ItemAcceptorBlockEntity.OutputDirection.BOTTOM;
				case BOTTOM -> itemAcceptorBlockEntity.outputDirection = ItemAcceptorBlockEntity.OutputDirection.TOP;
			}

			this.outputDirectionToggle.setMessage(Component.translatable("gui.glowcase.output_direction", this.itemAcceptorBlockEntity.outputDirection.toString()));
		}).bounds(width / 2 - 100, height / 2 + 5, 150, 20).build();

		this.pulseWidget = new EditBox(this.font, width / 2 + 60, height / 2 + 5, 40, 20, Component.empty());
		this.pulseWidget.setValue(String.valueOf(this.itemAcceptorBlockEntity.pulse));
		this.pulseWidget.setHint(TextUtils.placeholder("gui.glowcase.pulse"));
//FIXME 26.1
		//		this.pulseWidget.setFilter(s -> s.matches("\\d*"));

		this.addRenderableWidget(this.itemWidget);
		this.addRenderableWidget(this.countWidget);
		this.addRenderableWidget(this.outputDirectionToggle);
		this.addRenderableWidget(this.pulseWidget);
		this.addRenderableWidget(new StringWidget(width / 2 + 50, height / 2 - 25, 10, 20, Component.nullToEmpty("x"), font));
		this.addRenderableWidget(new StringWidget(width / 2 + 50, height / 2 + 5, 10, 20, Component.nullToEmpty("x"), font));
	}

	@Override
	public void onClose() {
		String text = itemWidget.getValue();
		boolean isItemTag = text.startsWith("#");
		if (isItemTag) {
			text = text.substring(1);
		}

		if (!text.isEmpty() && Identifier.tryParse(text) instanceof Identifier id) {
			this.itemAcceptorBlockEntity.setItem(id);
			this.itemAcceptorBlockEntity.isItemTag = isItemTag;
		} else {
			this.itemAcceptorBlockEntity.setItem(Identifier.withDefaultNamespace("air"));
		}

		if (Ints.tryParse(countWidget.getValue()) instanceof Integer integer) {
			this.itemAcceptorBlockEntity.count = Math.max(0, integer);
		}

		if (Ints.tryParse(pulseWidget.getValue()) instanceof Integer integer) {
			this.itemAcceptorBlockEntity.pulse = Math.max(0, integer);
		}

		C2SEditItemAcceptorBlock.of(this.itemAcceptorBlockEntity).send();
		super.onClose();
	}
}
