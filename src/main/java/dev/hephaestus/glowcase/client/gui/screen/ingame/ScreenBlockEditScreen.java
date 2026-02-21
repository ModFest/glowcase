package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Floats;
import dev.hephaestus.glowcase.block.entity.ScreenBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditScreenBlock;
import dev.hephaestus.glowcase.util.TextUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

public class ScreenBlockEditScreen extends GlowcaseScreen {
	private final ScreenBlockEntity screenBlockEntity;

	private EditBox widthEntryWidget;
	private EditBox heightEntryWidget;
	private Button zOffsetToggle;
	private Button[] alignment;

	private Checkbox renderBackfaceWidget;
	private Checkbox einkCheckWidget;
	private Checkbox stretchCheckWidget;

	private EditBox urlEntryWidget;
	private EditBox altEntryWidget;

	private EditBox yawEntryWidget;
    private EditBox pitchEntryWidget;

	private EditBox offsetXField;
	private EditBox offsetYField;
	private EditBox offsetZField;

	public ScreenBlockEditScreen(ScreenBlockEntity screenBlockEntity) {
		this.screenBlockEntity = screenBlockEntity;
	}

	@Override
	protected void init() {
		super.init();
		if (this.minecraft == null) return;

		// dimension constants
		int gap = 5;
		int leftX = width / 10;
		int availableWidth = width - (2 * (width / 10));
		int fieldWidth = (availableWidth - (2 * gap)) / 3;
		int fieldY = (height / 2) - 110;

		this.widthEntryWidget = new EditBox(this.minecraft.font, leftX, fieldY + 40 + 20 + 5, 2 * leftX, 20, Component.empty());
		this.widthEntryWidget.setValue(""+this.screenBlockEntity.width);
		this.widthEntryWidget.setHint(TextUtils.placeholder("gui.glowcase.width"));
		this.widthEntryWidget.setResponder(string -> {
			if (Floats.tryParse(string) instanceof Float parsed)
				screenBlockEntity.width = parsed;
		});

		MutableComponent timesLiteral = Component.literal("×");
		StringWidget timesLabel = new StringWidget(3 * leftX + gap, fieldY + 40 + 20 + 5, font.width(timesLiteral), 20, timesLiteral, this.minecraft.font);

		this.heightEntryWidget = new EditBox(this.minecraft.font, 3 * leftX + 10 + font.width(timesLiteral), fieldY + 40 + 20 + 5, 2 * leftX, 20, Component.empty());
		this.heightEntryWidget.setValue(""+this.screenBlockEntity.height);
		this.heightEntryWidget.setHint(TextUtils.placeholder("gui.glowcase.height"));
		this.heightEntryWidget.setResponder(string -> {
			if (Floats.tryParse(string) instanceof Float parsed)
				screenBlockEntity.height = parsed;
		});

		this.yawEntryWidget = new EditBox(this.minecraft.font, leftX, fieldY + 40, (4 * leftX + 10 + font.width(timesLiteral)) / 2 - 5, 20, Component.empty());
        if (this.screenBlockEntity.yaw == 0.0f) {
            this.yawEntryWidget.setValue("");
            this.yawEntryWidget.setHint(TextUtils.placeholder("gui.glowcase.yaw"));
        } else {
            this.yawEntryWidget.setValue(String.valueOf(this.screenBlockEntity.yaw));
        }
        this.yawEntryWidget.setResponder(string -> {
			if (string.isEmpty()) {
				screenBlockEntity.yaw = 0f;
			} else if (Floats.tryParse(string) instanceof Float parsed) {
				screenBlockEntity.yaw = parsed;
			}
        });

        this.pitchEntryWidget = new EditBox(this.minecraft.font, leftX + (4 * leftX + 10 + font.width(timesLiteral)) / 2, fieldY + 40, (4 * leftX + 10 + font.width(timesLiteral)) / 2, 20, Component.empty());
        if (this.screenBlockEntity.pitch == 0.0f) {
            this.pitchEntryWidget.setValue("");
            this.pitchEntryWidget.setHint(TextUtils.placeholder("gui.glowcase.pitch"));
        } else {
            this.pitchEntryWidget.setValue(String.valueOf(this.screenBlockEntity.pitch));
        }
        this.pitchEntryWidget.setResponder(string -> {
			if (string.isEmpty()) {
				screenBlockEntity.pitch = 0f;
			} else if (Floats.tryParse(string) instanceof Float parsed) {
				screenBlockEntity.pitch = parsed;
			}
        });

		StringWidget offsetXLabel = new StringWidget(leftX, fieldY - 5, fieldWidth, 20, Component.translatable("gui.glowcase.x_offset_label"), this.minecraft.font);
		StringWidget offsetYLabel = new StringWidget(leftX + fieldWidth + gap, fieldY - 5, fieldWidth, 20, Component.translatable("gui.glowcase.y_offset_label"), this.minecraft.font);
		StringWidget offsetZLabel = new StringWidget(leftX + 2 * (fieldWidth + gap), fieldY - 5, fieldWidth, 20, Component.translatable("gui.glowcase.z_offset_label"), this.minecraft.font);

		this.offsetXField = new EditBox(this.minecraft.font, leftX, fieldY + 15, fieldWidth, 20, Component.empty());
		this.offsetXField.setValue("" + this.screenBlockEntity.preciseX);
		this.offsetXField.setResponder(string -> {
			if (Floats.tryParse(string) instanceof Float parsed)
				screenBlockEntity.preciseX = parsed;
		});

		this.offsetYField = new EditBox(this.minecraft.font, leftX + fieldWidth + gap, fieldY + 15, fieldWidth, 20, Component.empty());
		this.offsetYField.setValue("" + this.screenBlockEntity.preciseY);
		this.offsetYField.setResponder(string -> {
			if (Floats.tryParse(string) instanceof Float parsed)
				screenBlockEntity.preciseY = parsed;
		});

		this.offsetZField = new EditBox(this.minecraft.font, leftX + 2 * (fieldWidth + gap), fieldY + 15, fieldWidth, 20, Component.empty());
		this.offsetZField.setValue("" + this.screenBlockEntity.preciseZ);
		this.offsetZField.setResponder(string -> {
			if (Floats.tryParse(string) instanceof Float parsed)
				screenBlockEntity.preciseZ = parsed;
		});

		this.zOffsetToggle = Button.builder(Component.translatable(switch (this.screenBlockEntity.zOffset) {
			case NEGATIVE -> "gui.glowcase.back";
			case NULL -> "gui.glowcase.center";
			case POSITIVE -> "gui.glowcase.front";
		}), action -> {
			switch (screenBlockEntity.zOffset) {
				case POSITIVE -> screenBlockEntity.zOffset = ScreenBlockEntity.Offset.NULL;
				case NULL -> screenBlockEntity.zOffset = ScreenBlockEntity.Offset.NEGATIVE;
				case NEGATIVE -> screenBlockEntity.zOffset = ScreenBlockEntity.Offset.POSITIVE;
			}
			this.zOffsetToggle.setMessage(Component.translatable(switch (this.screenBlockEntity.zOffset) {
				case NEGATIVE -> "gui.glowcase.back";
				case NULL -> "gui.glowcase.center";
				case POSITIVE -> "gui.glowcase.front";
			}));
		}).bounds(7 * width / 10, height / 2 - 70, 2 * width / 10, 20).build();

		{ // We create a button for each alignment possibility of the screen on a 2D canvas (top-left to bottom-right)
			int xoff = 7 * width / 10;
            int yoff = height / 2 - 65+20+10;

            int sub_width = 2 * width / 10;

			this.addRenderableWidget(new StringWidget(
				xoff, yoff,
				sub_width, this.minecraft.font.lineHeight,
				Component.translatableWithFallback("gui.glowcase.screen.alignment", "%s", this.screenBlockEntity.macaddress),
				this.minecraft.font)
			);

			xoff += sub_width/2 - (15 * 2 + 10)/2;
			yoff += this.minecraft.font.lineHeight + 5;

			int count = 0;
			alignment = new Button[9];
			for (int y = 0; y < 3; y++)
				for (int x = 0; x < 3; x++) {
					Button button = Button.builder(Component.literal(""), action -> {
						for (Button buttonWidget : alignment)
							buttonWidget.active = true;
						action.active = false;

						// Update x and y offset
						int cur_x = -1, cur_y = 0;
						for (Button buttonWidget : alignment) {
							cur_x++;
							if (cur_x > 2) {
								cur_x = 0;
								cur_y++;
							}

							if (!buttonWidget.active) {
								screenBlockEntity.xOffset = ScreenBlockEntity.Offset.fromOffset(cur_x - 1);
								screenBlockEntity.yOffset = ScreenBlockEntity.Offset.fromOffset(cur_y - 1);
								break;
							}
						}
					}).bounds(xoff + (x*15), yoff + (y*15), 10, 10).build();

					// Current Alignment
					if (screenBlockEntity.xOffset.offset+1 == x && screenBlockEntity.yOffset.offset+1 == y)
						button.active = false;

					alignment[count] = button;
					count++;
				}
		}

		this.renderBackfaceWidget = Checkbox.builder(Component.translatable("gui.glowcase.screen.backface"), this.minecraft.font)
			.selected(this.screenBlockEntity.renderBackface)
			.onValueChange((checkbox, checked) -> this.screenBlockEntity.renderBackface = checked)
			.pos(width / 10, height / 2 - 30 + 11)
			.build();

		this.einkCheckWidget = Checkbox.builder(Component.translatable("gui.glowcase.screen.eink"), this.minecraft.font)
			.selected(this.screenBlockEntity.eink)
			.onValueChange((checkbox, checked) -> this.screenBlockEntity.eink = checked)
			.pos(width / 10, height / 2 - 10 + 11)
			.build();

		this.stretchCheckWidget = Checkbox.builder(Component.translatable("gui.glowcase.screen.stretch"), this.minecraft.font)
			.selected(this.screenBlockEntity.stretch)
			.onValueChange((checkbox, checked) -> this.screenBlockEntity.stretch = checked)
			.pos(width / 10, height / 2 + 10 + 11)
			.build();

		this.urlEntryWidget = new EditBox(this.minecraft.font, width / 10, height / 2 + 45, 7 * width / 10, 20, Component.empty());
		this.urlEntryWidget.setMaxLength(ScreenBlockEntity.URL_MAX_LENGTH);
		this.urlEntryWidget.setValue(this.screenBlockEntity.url);
		this.urlEntryWidget.setHint(TextUtils.placeholder("gui.glowcase.url"));
		// We don't change the url on the fly here as that would cause many fetch requests which we don't want

		this.altEntryWidget = new EditBox(this.minecraft.font, width / 10, height / 2 + 65 + 5, 7 * width / 10, 40, Component.empty());
		this.altEntryWidget.setMaxLength(ScreenBlockEntity.ALT_MAX_LENGTH);
		this.altEntryWidget.setValue(this.screenBlockEntity.alt);
		this.altEntryWidget.setHint(TextUtils.placeholder("gui.glowcase.alt"));
		this.altEntryWidget.setResponder(string -> screenBlockEntity.alt = string);

		if (this.minecraft.options.advancedItemTooltips)
			this.addRenderableWidget(new StringWidget(
				3, height - this.minecraft.font.lineHeight - 1,
				width, this.minecraft.font.lineHeight,
				Component.translatableWithFallback("gui.glowcase.screen.mac_address", "%s", this.screenBlockEntity.macaddress),
				this.minecraft.font).alignLeft().setColor(0x696969)
			);

		this.addRenderableWidget(this.widthEntryWidget);
		this.addRenderableWidget(timesLabel);
		this.addRenderableWidget(this.heightEntryWidget);
		this.addRenderableWidget(this.zOffsetToggle);

		this.addRenderableWidget(offsetXLabel);
		this.addRenderableWidget(offsetYLabel);
		this.addRenderableWidget(offsetZLabel);
		this.addRenderableWidget(offsetXField);
		this.addRenderableWidget(offsetYField);
		this.addRenderableWidget(offsetZField);

		for (Button buttonWidget : alignment)
			this.addRenderableWidget(buttonWidget);

		this.addRenderableWidget(this.renderBackfaceWidget);
		this.addRenderableWidget(this.einkCheckWidget);
		this.addRenderableWidget(this.stretchCheckWidget);

		this.addRenderableWidget(this.urlEntryWidget);
		this.addRenderableWidget(this.altEntryWidget);

		this.addRenderableWidget(this.yawEntryWidget);
		this.addRenderableWidget(this.pitchEntryWidget);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int keyCode = event.key();
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
			this.onClose();
			return true;
		} else if (this.widthEntryWidget.canConsumeInput()) {
			return this.widthEntryWidget.keyPressed(event);
		} else if (this.heightEntryWidget.canConsumeInput()) {
			return this.heightEntryWidget.keyPressed(event);
		} else if (this.urlEntryWidget.canConsumeInput()) {
			return this.urlEntryWidget.keyPressed(event);
		} else if (this.altEntryWidget.canConsumeInput()) {
			return this.altEntryWidget.keyPressed(event);
		} else if (this.offsetXField.canConsumeInput()) {
			return this.offsetXField.keyPressed(event);
		} else if (this.offsetYField.canConsumeInput()) {
			return this.offsetYField.keyPressed(event);
		} else if (this.offsetZField.canConsumeInput()) {
			return this.offsetZField.keyPressed(event);
		} else if (this.pitchEntryWidget.canConsumeInput()) {
			return this.pitchEntryWidget.keyPressed(event);
		} else if (this.yawEntryWidget.canConsumeInput()) {
			return this.yawEntryWidget.keyPressed(event);
		} else {
			return false;
		}
	}

	@Override
	public void onClose() {
		Float parsedX = Floats.tryParse(this.offsetXField.getValue());
		Float parsedY = Floats.tryParse(this.offsetYField.getValue());
		Float parsedZ = Floats.tryParse(this.offsetZField.getValue());

		screenBlockEntity.preciseX = (parsedX != null) ? parsedX : 0f;
		screenBlockEntity.preciseY = (parsedY != null) ? parsedY : 0f;
		screenBlockEntity.preciseZ = (parsedZ != null) ? parsedZ : 0f;

		screenBlockEntity.eink = einkCheckWidget.selected();
		screenBlockEntity.stretch = stretchCheckWidget.selected();
		screenBlockEntity.setImage(
			urlEntryWidget.getValue(),
			altEntryWidget.getValue(),
			null
		);

		C2SEditScreenBlock.of(screenBlockEntity).send();
		super.onClose();
	}
}
