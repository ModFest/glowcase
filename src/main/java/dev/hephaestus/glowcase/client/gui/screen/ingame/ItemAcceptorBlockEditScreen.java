package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Ints;
import dev.hephaestus.glowcase.block.entity.ItemAcceptorBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.packet.C2SEditItemAcceptorBlock;
import dev.hephaestus.glowcase.util.InputFilters;
import dev.hephaestus.glowcase.util.TextUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class ItemAcceptorBlockEditScreen extends BlockEditorScreen<ItemAcceptorBlockEntity> {
	private GlowcaseEditBox itemWidget;
	private GlowcaseEditBox countWidget;
	private GlowcaseEditBox pulseWidget;
	private Button outputDirectionToggle;

	public ItemAcceptorBlockEditScreen(ItemAcceptorBlockEntity blockEntity) {
		super(blockEntity);
	}

	@Override
	public void init() {
		super.init();

		Identifier item = this.blockEntity.getItem();

		this.itemWidget = new GlowcaseEditBox(this.font, width / 2 - 100, height / 2 - 25, 150, 20, Component.empty());
		this.itemWidget.setMaxLength(128);
		if (!item.equals(Identifier.withDefaultNamespace("air"))) {
			this.itemWidget.setValue((this.blockEntity.isItemTag ? "#" : "") + item);
		}
		this.itemWidget.setHint(TextUtils.placeholder("gui.glowcase.item_or_tag"));
		this.itemWidget.setFilter((currentValue, newChar, cursorPos, highlightPos) -> {
			if (!InputFilters.assertOptionalPrefix('#', currentValue, newChar, cursorPos, highlightPos)) return false;
			if (newChar == '#' && cursorPos == 0) return true;

			return this.isValidCharacterForName(currentValue, newChar, cursorPos);
		});

		this.countWidget = new GlowcaseEditBox(this.font, width / 2 + 60, height / 2 - 25, 40, 20, Component.empty());
		this.countWidget.setValue(String.valueOf(this.blockEntity.count));
		this.countWidget.setHint(TextUtils.placeholder("gui.glowcase.count"));
		this.countWidget.setFilter(InputFilters::integerNumber);

		this.outputDirectionToggle = Button.builder(Component.translatable(
			"gui.glowcase.output_direction",
			this.blockEntity.outputDirection.toString()
		), action -> {
			switch (blockEntity.outputDirection) {
				case TOP -> blockEntity.outputDirection = ItemAcceptorBlockEntity.OutputDirection.BACK;
				case BACK -> blockEntity.outputDirection = ItemAcceptorBlockEntity.OutputDirection.BOTTOM;
				case BOTTOM -> blockEntity.outputDirection = ItemAcceptorBlockEntity.OutputDirection.TOP;
			}

			this.outputDirectionToggle.setMessage(Component.translatable(
				"gui.glowcase.output_direction",
				this.blockEntity.outputDirection.toString()
			));
		}).bounds(width / 2 - 100, height / 2 + 5, 150, 20).build();

		this.pulseWidget = new GlowcaseEditBox(this.font, width / 2 + 60, height / 2 + 5, 40, 20, Component.empty());
		this.pulseWidget.setValue(String.valueOf(this.blockEntity.pulse));
		this.pulseWidget.setHint(TextUtils.placeholder("gui.glowcase.pulse"));
		this.pulseWidget.setFilter(InputFilters::integerNumber);

		this.addRenderableWidget(this.itemWidget);
		this.addRenderableWidget(this.countWidget);
		this.addRenderableWidget(this.outputDirectionToggle);
		this.addRenderableWidget(this.pulseWidget);
		this.addRenderableWidget(new StringWidget(width / 2 + 50, height / 2 - 25, 10, 20, Component.nullToEmpty("x"), font));
		this.addRenderableWidget(new StringWidget(width / 2 + 50, height / 2 + 5, 10, 20, Component.nullToEmpty("x"), font));
	}

	@Override
	public CustomPacketPayload getUpdatePayload() {
		String text = itemWidget.getValue();
		boolean isItemTag = text.startsWith("#");
		if (isItemTag) {
			text = text.substring(1);
		}

		if (!text.isEmpty() && Identifier.tryParse(text) instanceof Identifier id) {
			this.blockEntity.setItem(id);
			this.blockEntity.isItemTag = isItemTag;
		} else {
			this.blockEntity.setItem(Identifier.withDefaultNamespace("air"));
		}

		if (Ints.tryParse(countWidget.getValue()) instanceof Integer integer) {
			this.blockEntity.count = Math.max(0, integer);
		}

		if (Ints.tryParse(pulseWidget.getValue()) instanceof Integer integer) {
			this.blockEntity.pulse = Math.max(0, integer);
		}

		return C2SEditItemAcceptorBlock.of(this.blockEntity);
	}
}
