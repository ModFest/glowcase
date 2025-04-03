package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditSpriteBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.List;

public class SpriteBlockEditScreen extends GlowcaseScreen {
	private final SpriteBlockEntity spriteBlockEntity;

	private TextFieldWidget spriteWidget;
	private ButtonWidget rotationWidget;
	private ButtonWidget zOffsetToggle;
	private TextFieldWidget colorEntryWidget;
	private TextFieldWidget scaleEntryWidget;

	private TooltipPositioner spriteHelpTooltipPositioner;
	private List<OrderedText> spriteHelpTooltipText;

	public SpriteBlockEditScreen(SpriteBlockEntity spriteBlockEntity) {
		this.spriteBlockEntity = spriteBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		if (this.client == null) return;

		this.spriteWidget = new TextFieldWidget(this.client.textRenderer, width / 2 - 75, height / 2 - 55, 150, 20, Text.empty());
		this.spriteWidget.setMaxLength(255);
		this.spriteWidget.setText(spriteBlockEntity.getSprite());
		this.spriteWidget.setChangedListener(string -> {
			this.spriteBlockEntity.setSprite(this.spriteWidget.getText());
		});

		this.spriteHelpTooltipPositioner = new ConstantTooltipPositioner(new Vector2i(
			SpriteBlockEditScreen.this.spriteWidget.getX() + SpriteBlockEditScreen.this.spriteWidget.getWidth() + 8,
			SpriteBlockEditScreen.this.spriteWidget.getY() - 16
		));
		this.spriteHelpTooltipText = this.client.textRenderer.wrapLines(Text.translatable("gui.glowcase.screen.sprite_edit.sprite"), 125);

		this.rotationWidget = ButtonWidget.builder(Text.translatable("gui.glowcase.rotate"), (action) -> {
			this.spriteBlockEntity.rotation = (this.spriteBlockEntity.rotation + 45) % 360;
		}).dimensions(width / 2 - 75, height / 2 - 25, 150, 20).build();

		this.zOffsetToggle = ButtonWidget.builder(Text.literal(this.spriteBlockEntity.zOffset.name()), action -> {
			switch (spriteBlockEntity.zOffset) {
				case FRONT -> spriteBlockEntity.zOffset = TextBlockEntity.ZOffset.CENTER;
				case CENTER -> spriteBlockEntity.zOffset = TextBlockEntity.ZOffset.BACK;
				case BACK -> spriteBlockEntity.zOffset = TextBlockEntity.ZOffset.FRONT;
			}

			this.zOffsetToggle.setMessage(Text.literal(this.spriteBlockEntity.zOffset.name()));
		}).dimensions(width / 2 - 75, height / 2 + 5, 150, 20).build();

		this.colorEntryWidget = new TextFieldWidget(this.client.textRenderer, width / 2 - 75, height / 2 + 35, 150, 20, Text.empty());
		this.colorEntryWidget.setText("#" + String.format("%1$06X", this.spriteBlockEntity.color & 0x00FFFFFF));
		this.colorEntryWidget.setChangedListener(string -> {
			TextColor.parse(this.colorEntryWidget.getText()).ifSuccess(color -> {
				this.spriteBlockEntity.color = color == null ? 0xFFFFFFFF : color.getRgb() | 0xFF000000;
			});
		});

		this.scaleEntryWidget = new TextFieldWidget(this.client.textRenderer, width / 2 - 75, height / 2 + 65, 150, 20, Text.empty());
		this.scaleEntryWidget.setText(String.valueOf(this.spriteBlockEntity.scale));
		this.scaleEntryWidget.setChangedListener(string -> {
			 try {
				 this.spriteBlockEntity.scale = Float.parseFloat(string);
			 } catch (NumberFormatException ignored) {}
		});

		this.addDrawableChild(this.spriteWidget);
		this.addDrawableChild(this.rotationWidget);
		this.addDrawableChild(this.zOffsetToggle);
		this.addDrawableChild(this.colorEntryWidget);
		this.addDrawableChild(this.scaleEntryWidget);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawTooltip(this.textRenderer, this.spriteHelpTooltipText, this.spriteHelpTooltipPositioner, 0, 0);
	}

	@Override
	public void close() {
		spriteBlockEntity.setSprite(spriteWidget.getText());
		spriteBlockEntity.markDirty();
		C2SEditSpriteBlock.of(spriteBlockEntity).send();
		super.close();
	}

	record ConstantTooltipPositioner(Vector2ic position) implements TooltipPositioner {
		@Override
		public Vector2ic getPosition(int screenWidth, int screenHeight, int x, int y, int width, int height) {
			return position;
		}
	}
}
