package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditHyperlinkBlock;
import dev.hephaestus.glowcase.util.TextUtils;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HyperlinkBlockEditScreen extends GlowcaseScreen {
	private final HyperlinkBlockEntity hyperlinkBlockEntity;

	private TextFieldWidget titleEntryWidget;
	private TextFieldWidget urlEntryWidget;

	public HyperlinkBlockEditScreen(HyperlinkBlockEntity hyperlinkBlockEntity) {
		this.hyperlinkBlockEntity = hyperlinkBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		if (this.client == null) return;

		this.titleEntryWidget = new TextFieldWidget(this.client.textRenderer, width / 10, height / 2 - 30, 8 * width / 10, 20, Text.empty());
		this.titleEntryWidget.setMaxLength(HyperlinkBlockEntity.TITLE_MAX_LENGTH);
		this.titleEntryWidget.setText(this.hyperlinkBlockEntity.getTitle());
		this.titleEntryWidget.setPlaceholder(TextUtils.placeholder("gui.glowcase.title"));

		this.urlEntryWidget = new TextFieldWidget(this.client.textRenderer, width / 10, height / 2 + 10, 8 * width / 10, 20, Text.empty());
		this.urlEntryWidget.setMaxLength(HyperlinkBlockEntity.URL_MAX_LENGTH);
		this.urlEntryWidget.setText(this.hyperlinkBlockEntity.getUrl());
		this.urlEntryWidget.setPlaceholder(TextUtils.placeholder("gui.glowcase.url"));

		this.addDrawableChild(this.titleEntryWidget);
		this.addDrawableChild(this.urlEntryWidget);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
			this.close();
			return true;
		} else if (this.titleEntryWidget.isActive()) {
			return this.titleEntryWidget.keyPressed(keyCode, scanCode, modifiers);
		} else if (this.urlEntryWidget.isActive()) {
			return this.urlEntryWidget.keyPressed(keyCode, scanCode, modifiers);
		} else {
			return false;
		}
	}

	@Override
	public void close() {
		hyperlinkBlockEntity.setUrl(urlEntryWidget.getText());
		hyperlinkBlockEntity.setTitle(titleEntryWidget.getText());
		C2SEditHyperlinkBlock.of(hyperlinkBlockEntity).send();
		super.close();
	}
}
