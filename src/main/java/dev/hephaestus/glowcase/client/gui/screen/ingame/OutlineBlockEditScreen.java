package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Ints;
import dev.hephaestus.glowcase.block.entity.OutlineBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditOutlineBlock;
import dev.hephaestus.glowcase.util.TextUtils;
import java.util.function.Predicate;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public class OutlineBlockEditScreen extends GlowcaseScreen {
	private static final Predicate<String> TEXT_PREDICATE = s -> s.matches("-?\\d*");

	private final OutlineBlockEntity outlineBlockEntity;

	private StringWidget offsetWidget;
	private StringWidget scaleWidget;
	private EditBox xOffsetWidget;
	private EditBox yOffsetWidget;
	private EditBox zOffsetWidget;
	private EditBox xScaleWidget;
	private EditBox yScaleWidget;
	private EditBox zScaleWidget;
	private EditBox colorEntryWidget;

	public OutlineBlockEditScreen(OutlineBlockEntity outlineBlockEntity) {
		this.outlineBlockEntity = outlineBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		if (this.minecraft == null) return;

		this.offsetWidget = new StringWidget(width / 2 - 110, height / 2 - 25, 40, 20, Component.translatable("gui.glowcase.offset"), this.font);
		this.scaleWidget = new StringWidget(width / 2 - 110, height / 2 + 5, 40, 20, Component.translatable("gui.glowcase.scale"), this.font);

		this.xOffsetWidget = new EditBox(this.font, width / 2 - 65, height / 2 - 25, 40, 20, Component.empty());
		this.yOffsetWidget = new EditBox(this.font, width / 2 - 20, height / 2 - 25, 40, 20, Component.empty());
		this.zOffsetWidget = new EditBox(this.font, width / 2 + 25, height / 2 - 25, 40, 20, Component.empty());

		this.xScaleWidget = new EditBox(this.font, width / 2 - 65, height / 2 + 5, 40, 20, Component.empty());
		this.yScaleWidget = new EditBox(this.font, width / 2 - 20, height / 2 + 5, 40, 20, Component.empty());
		this.zScaleWidget = new EditBox(this.font, width / 2 + 25, height / 2 + 5, 40, 20, Component.empty());

		this.xOffsetWidget.setValue(String.valueOf(this.outlineBlockEntity.offset.getX()));
		this.yOffsetWidget.setValue(String.valueOf(this.outlineBlockEntity.offset.getY()));
		this.zOffsetWidget.setValue(String.valueOf(this.outlineBlockEntity.offset.getZ()));
		this.xScaleWidget.setValue(String.valueOf(this.outlineBlockEntity.scale.getX()));
		this.yScaleWidget.setValue(String.valueOf(this.outlineBlockEntity.scale.getY()));
		this.zScaleWidget.setValue(String.valueOf(this.outlineBlockEntity.scale.getZ()));

//		FIXME 26.1
//		this.xOffsetWidget.setFilter(TEXT_PREDICATE);
//		this.yOffsetWidget.setFilter(TEXT_PREDICATE);
//		this.zOffsetWidget.setFilter(TEXT_PREDICATE);
//		this.xScaleWidget.setFilter(TEXT_PREDICATE);
//		this.yScaleWidget.setFilter(TEXT_PREDICATE);
//		this.zScaleWidget.setFilter(TEXT_PREDICATE);

		this.xOffsetWidget.setResponder(string -> {
			if (Ints.tryParse(string) instanceof Integer x) {
				Vec3i offset = this.outlineBlockEntity.offset;
				this.outlineBlockEntity.offset = new Vec3i(x, offset.getY(), offset.getZ());
			}
		});
		this.yOffsetWidget.setResponder(string -> {
			if (Ints.tryParse(string) instanceof Integer y) {
				Vec3i offset = this.outlineBlockEntity.offset;
				this.outlineBlockEntity.offset = new Vec3i(offset.getX(), y, offset.getZ());
			}
		});
		this.zOffsetWidget.setResponder(string -> {
			if (Ints.tryParse(string) instanceof Integer z) {
				Vec3i offset = this.outlineBlockEntity.offset;
				this.outlineBlockEntity.offset = new Vec3i(offset.getX(), offset.getY(), z);
			}
		});

		this.xScaleWidget.setResponder(string -> {
			if (Ints.tryParse(string) instanceof Integer x) {
				Vec3i scale = this.outlineBlockEntity.scale;
				this.outlineBlockEntity.scale = new Vec3i(x, scale.getY(), scale.getZ());
			}
		});
		this.yScaleWidget.setResponder(string -> {
			if (Ints.tryParse(string) instanceof Integer y) {
				Vec3i scale = this.outlineBlockEntity.scale;
				this.outlineBlockEntity.scale = new Vec3i(scale.getX(), y, scale.getZ());
			}
		});
		this.zScaleWidget.setResponder(string -> {
			if (Ints.tryParse(string) instanceof Integer z) {
				Vec3i scale = this.outlineBlockEntity.scale;
				this.outlineBlockEntity.scale = new Vec3i(scale.getX(), scale.getY(), z);
			}
		});

		this.xOffsetWidget.setHint(TextUtils.placeholder("gui.glowcase.x"));
		this.yOffsetWidget.setHint(TextUtils.placeholder("gui.glowcase.y"));
		this.zOffsetWidget.setHint(TextUtils.placeholder("gui.glowcase.z"));
		this.xScaleWidget.setHint(TextUtils.placeholder("gui.glowcase.x"));
		this.yScaleWidget.setHint(TextUtils.placeholder("gui.glowcase.y"));
		this.zScaleWidget.setHint(TextUtils.placeholder("gui.glowcase.z"));

		this.colorEntryWidget = new EditBox(this.minecraft.font, width / 2 - 25, height / 2 + 35, 50, 20, Component.empty());
		this.colorEntryWidget.setValue("#" + String.format("%1$06X", this.outlineBlockEntity.color & 0x00FFFFFF));
		this.colorEntryWidget.setResponder(string -> {
			TextColor.parseColor(this.colorEntryWidget.getValue()).ifSuccess(color -> {
				this.outlineBlockEntity.color = color == null ? 0xFFFFFFFF : color.getValue() | 0xFF000000;
			});
		});

		this.addRenderableWidget(this.offsetWidget);
		this.addRenderableWidget(this.scaleWidget);
		this.addRenderableWidget(this.xOffsetWidget);
		this.addRenderableWidget(this.yOffsetWidget);
		this.addRenderableWidget(this.zOffsetWidget);
		this.addRenderableWidget(this.xScaleWidget);
		this.addRenderableWidget(this.yScaleWidget);
		this.addRenderableWidget(this.zScaleWidget);
		this.addRenderableWidget(this.colorEntryWidget);
	}

		@Override
	public void onClose() {
		C2SEditOutlineBlock.of(outlineBlockEntity).send();
		super.onClose();
	}
}
