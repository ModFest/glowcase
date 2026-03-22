package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Floats;
import dev.hephaestus.glowcase.block.entity.DisplayBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;

public abstract class DisplayBlockEditScreen extends GlowcaseScreen {
	protected final DisplayBlockEntity displayBlock;

	protected EditBox scaleField;
    protected EditBox xOffsetField;
    protected EditBox yOffsetField;
    protected EditBox zOffsetField;
    protected EditBox pitchField;
    protected EditBox yawField;

	protected Button decreaseSize;
	protected Button increaseSize;

	protected Button decreaseXOffset;
	protected Button increaseXOffset;
	protected Button decreaseYOffset;
	protected Button increaseYOffset;
	protected Button decreaseZOffset;
	protected Button increaseZOffset;
	protected Button decreasePitch;
	protected Button increasePitch;
	protected Button decreaseYaw;
	protected Button increaseYaw;

	private final float pitchYawChange = 15F;
	private final float scaleOffsetChange = 0.125F;

	public DisplayBlockEditScreen(DisplayBlockEntity displayBlock) {
		this.displayBlock = displayBlock;
	}

	@Override
	public void init() {
		super.init();

		this.scaleField = new EditBox(this.minecraft.font, 90, 10, 60, 20, Component.empty());
		this.scaleField.setValue(String.valueOf(this.displayBlock.getScale().x()));
		this.scaleField.setResponder(string -> {
			if (Floats.tryParse(string) instanceof Float parsed) {
				this.displayBlock.setScale(new Vector3f(parsed, parsed, parsed));
				editDisplayBlock();
			}
		});

		this.decreaseSize = Button.builder(Component.literal("-"), _ -> {
			this.displayBlock.getScale().sub(scaleOffsetChange, scaleOffsetChange, scaleOffsetChange);
			editDisplayBlock();
			this.scaleField.setValue(String.valueOf(this.displayBlock.getScale().x()));
		}).bounds(90 + 60 + 5, 10, 20, 20).build();

		this.increaseSize = Button.builder(Component.literal("+"), _ -> {
			this.displayBlock.getScale().add(scaleOffsetChange, scaleOffsetChange, scaleOffsetChange);
			editDisplayBlock();
			this.scaleField.setValue(String.valueOf(this.displayBlock.getScale().x()));
		}).bounds(90 + 60 + 5 + 20, 10, 20, 20).build();

		this.xOffsetField = new EditBox(this.minecraft.font, 90, 40, 60, 20, Component.empty());
		this.xOffsetField.setValue(String.valueOf(this.displayBlock.getOffset().x()));
		this.xOffsetField.setResponder(string -> {
			if (Floats.tryParse(string) instanceof Float parsed) {
				Vector3f offset = this.displayBlock.getOffset();
				offset.x = parsed;
				this.displayBlock.setOffset(offset);
				editDisplayBlock();
			}
		});

		this.decreaseXOffset = Button.builder(Component.literal("-"), action -> {
			this.displayBlock.getOffset().sub(scaleOffsetChange, 0, 0);
			editDisplayBlock();
			this.xOffsetField.setValue(String.valueOf(this.displayBlock.getOffset().x()));
		}).bounds(90 + 60 + 5, 40, 20, 20).build();

		this.increaseXOffset = Button.builder(Component.literal("+"), action -> {
			this.displayBlock.getOffset().add(scaleOffsetChange, 0, 0);
			editDisplayBlock();
			this.xOffsetField.setValue(String.valueOf(this.displayBlock.getOffset().x()));
		}).bounds(90 + 60 + 5 + 20, 40, 20, 20).build();

		this.yOffsetField = new EditBox(this.minecraft.font, 90, 70, 60, 20, Component.empty());
		this.yOffsetField.setValue(String.valueOf(this.displayBlock.getOffset().y()));
		this.yOffsetField.setResponder(string -> {
			if (Floats.tryParse(string) instanceof Float parsed) {
				Vector3f offset = this.displayBlock.getOffset();
				offset.y = parsed;
				this.displayBlock.setOffset(offset);
				editDisplayBlock();
			}
		});

		this.decreaseYOffset = Button.builder(Component.literal("-"), action -> {
			this.displayBlock.getOffset().sub(0, scaleOffsetChange, 0);
			editDisplayBlock();
			this.yOffsetField.setValue(String.valueOf(this.displayBlock.getOffset().y()));
		}).bounds(90 + 60 + 5, 70, 20, 20).build();

		this.increaseYOffset = Button.builder(Component.literal("+"), action -> {
			this.displayBlock.getOffset().add(0, scaleOffsetChange, 0);
			editDisplayBlock();
			this.yOffsetField.setValue(String.valueOf(this.displayBlock.getOffset().y()));
		}).bounds(90 + 60 + 5 + 20, 70, 20, 20).build();

		this.zOffsetField = new EditBox(this.minecraft.font, 90, 100, 60, 20, Component.empty());
		this.zOffsetField.setValue(String.valueOf(this.displayBlock.getOffset().z()));
		this.zOffsetField.setResponder(string -> {
			if (Floats.tryParse(string) instanceof Float parsed) {
				Vector3f offset = this.displayBlock.getOffset();
				offset.z = parsed;
				this.displayBlock.setOffset(offset);
				editDisplayBlock();
			}
		});

		this.decreaseZOffset = Button.builder(Component.literal("-"), action -> {
			this.displayBlock.getOffset().sub(0, 0, scaleOffsetChange);
			editDisplayBlock();
			this.zOffsetField.setValue(String.valueOf(this.displayBlock.getOffset().z()));
		}).bounds(90 + 60 + 5, 100, 20, 20).build();

		this.increaseZOffset = Button.builder(Component.literal("+"), action -> {
			this.displayBlock.getOffset().add(0, 0, scaleOffsetChange);
			editDisplayBlock();
			this.zOffsetField.setValue(String.valueOf(this.displayBlock.getOffset().z()));
		}).bounds(90 + 60 + 5 + 20, 100, 20, 20).build();

		this.pitchField = new EditBox(this.minecraft.font, 90, 130, 60, 20, Component.empty());
		this.pitchField.setValue(String.valueOf(this.displayBlock.getPitch()));
		this.pitchField.setResponder(string -> {
			if (Floats.tryParse(string) instanceof Float parsed) {
				this.displayBlock.setPitch(parsed);
				editDisplayBlock();
			}
		});

		this.decreasePitch = Button.builder(Component.literal("-"), action -> {
			this.displayBlock.setPitch(this.displayBlock.getPitch() - pitchYawChange);
			editDisplayBlock();
			this.pitchField.setValue(String.valueOf(this.displayBlock.getPitch()));
		}).bounds(90 + 60 + 5, 130, 20, 20).build();

		this.increasePitch = Button.builder(Component.literal("+"), action -> {
			this.displayBlock.setPitch(this.displayBlock.getPitch() + pitchYawChange);
			editDisplayBlock();
			this.pitchField.setValue(String.valueOf(this.displayBlock.getPitch()));
		}).bounds(90 + 60 + 5 + 20, 130, 20, 20).build();

		this.yawField = new EditBox(this.minecraft.font, 90, 160, 60, 20, Component.empty());
		this.yawField.setValue(String.valueOf(this.displayBlock.getYaw()));
		this.yawField.setResponder(string -> {
			if (Floats.tryParse(string) instanceof Float parsed) {
				this.displayBlock.setYaw(parsed);
				editDisplayBlock();
			}
		});

		this.decreaseYaw = Button.builder(Component.literal("-"), action -> {
			this.displayBlock.setYaw(this.displayBlock.getYaw() - pitchYawChange);
			editDisplayBlock();
			this.yawField.setValue(String.valueOf(this.displayBlock.getYaw()));
		}).bounds(90 + 60 + 5, 160, 20, 20).build();

		this.increaseYaw = Button.builder(Component.literal("+"), action -> {
			this.displayBlock.setYaw(this.displayBlock.getYaw() + pitchYawChange);
			editDisplayBlock();
			this.yawField.setValue(String.valueOf(this.displayBlock.getYaw()));
		}).bounds(90 + 60 + 5 + 20, 160, 20, 20).build();

		this.addRenderableWidget(this.scaleField);
		this.addRenderableWidget(this.xOffsetField);
		this.addRenderableWidget(this.yOffsetField);
		this.addRenderableWidget(this.zOffsetField);
		this.addRenderableWidget(this.pitchField);
		this.addRenderableWidget(this.yawField);
		this.addRenderableWidget(this.decreaseSize);
		this.addRenderableWidget(this.increaseSize);
		this.addRenderableWidget(this.decreaseXOffset);
		this.addRenderableWidget(this.increaseXOffset);
		this.addRenderableWidget(this.decreaseYOffset);
		this.addRenderableWidget(this.increaseYOffset);
		this.addRenderableWidget(this.decreaseZOffset);
		this.addRenderableWidget(this.increaseZOffset);
		this.addRenderableWidget(this.decreasePitch);
		this.addRenderableWidget(this.increasePitch);
		this.addRenderableWidget(this.decreaseYaw);
		this.addRenderableWidget(this.increaseYaw);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.text(minecraft.font, Component.translatable("gui.glowcase.scale_label"), 20, 17, 0xFFFFFFFF);
		graphics.text(minecraft.font, Component.translatable("gui.glowcase.x_offset_label"), 20, 47, 0xFFFFFFFF);
		graphics.text(minecraft.font, Component.translatable("gui.glowcase.y_offset_label"), 20, 77, 0xFFFFFFFF);
		graphics.text(minecraft.font, Component.translatable("gui.glowcase.z_offset_label"), 20, 107, 0xFFFFFFFF);
		graphics.text(minecraft.font, Component.translatable("gui.glowcase.pitch_value"), 20, 137, 0xFFFFFFFF);
		graphics.text(minecraft.font, Component.translatable("gui.glowcase.yaw_value"), 20, 167, 0xFFFFFFFF);
	}

	protected void editDisplayBlock() {}
}
