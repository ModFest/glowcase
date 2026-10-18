package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.DisplayBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.number.Vec3FieldsWidget;
import dev.hephaestus.glowcase.client.util.Tooltips;
import dev.hephaestus.glowcase.util.Components;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public abstract class DisplayBlockEditScreen extends BlockEditorScreen<DisplayBlockEntity> {

	protected static final int SPACING = 5;
	protected static final int BUTTON_HEIGHT = 20;
	protected static final int ROW = SPACING + BUTTON_HEIGHT;
	protected int headedRow;
	protected int rowOffset;

	protected Vec3FieldsWidget scaleField;
	protected Vec3FieldsWidget offsetField;
	protected Vec3FieldsWidget rotationField;

	protected Button decreaseSize;
	protected Button increaseSize;

	private final float scaleOffsetChange = 0.125F;

	public DisplayBlockEditScreen(DisplayBlockEntity blockEntity) {
		super(blockEntity);
	}

	@Override
	public void init() {
		super.init();

		final int R = this.headedRow = this.font.lineHeight + ROW;

		final int X = 10, WIDTH = 180;
		int y = -20;

		this.rotationField = Vec3FieldsWidget.builder(this.font, this.blockEntity.getRotation())
			.setPos(X, y += R)
			.setWidth(WIDTH)
			.setRotation(true)
			.setEditBoxCharacterLimit(5)
			.setOnValueChange(this.blockEntity::setRotation)
			.build();

		this.offsetField = Vec3FieldsWidget.builder(this.font, this.getBlockEntity().getOffset())
			.setPos(X, y += R)
			.setWidth(WIDTH)
			.setTooltips(Tooltips.OFFSET_AXES)
			.setEditBoxCharacterLimit(5)
			.setOnValueChange(offset -> this.blockEntity.setOffset(offset.toVector3f()))
			.build();

		this.scaleField = Vec3FieldsWidget.builder(this.font, this.blockEntity.getScale())
			.setPos(X, y += R)
			.setWidth(WIDTH)
			.setTooltips(Tooltips.SCALE_AXES)
			.setEditBoxCharacterLimit(5)
			.setOnValueChange(scale -> this.blockEntity.setScale(scale.toVector3f()))
			.build();

		// Admittedly this is just a humorous case of 'we need a way to lock the fields together'
		// TODO: add linking support in the vec3, perhaps a way to default?
		this.decreaseSize = Button.builder(Component.literal("-"), _ -> {
			this.blockEntity.getScale().sub(scaleOffsetChange, scaleOffsetChange, scaleOffsetChange);
			editDisplayBlock();
			this.scaleField.setVec(new Vec3(this.blockEntity.getScale()));
		}).bounds(X + WIDTH + 5, y, 20, 20).build();

		this.increaseSize = Button.builder(Component.literal("+"), _ -> {
			this.blockEntity.getScale().add(scaleOffsetChange, scaleOffsetChange, scaleOffsetChange);
			editDisplayBlock();
			this.scaleField.setVec(new Vec3(this.blockEntity.getScale()));
		}).bounds(X + WIDTH + 5 + 20, y, 20, 20).build();

		this.addRenderableWidget(this.scaleField);
		this.addRenderableWidget(this.offsetField);
		this.addRenderableWidget(this.rotationField);
		this.addRenderableWidget(this.decreaseSize);
		this.addRenderableWidget(this.increaseSize);

		this.rowOffset = y;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		final int height = -this.font.lineHeight - 1; // 7
		graphics.text(this.font, Components.scale, 10, this.scaleField.getY() + height, 0xFFFFFFFF);
		graphics.text(this.font, Components.offset, 10, this.offsetField.getY() + height, 0xFFFFFFFF);
		graphics.text(this.font, Components.rotation, 10, this.rotationField.getY() + height, 0xFFFFFFFF);
	}

	protected final void editDisplayBlock() {
		this.sendUpdatePacket();
	}
}
