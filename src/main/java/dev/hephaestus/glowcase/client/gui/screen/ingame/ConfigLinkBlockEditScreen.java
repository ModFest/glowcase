package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.ConfigLinkBlockEntity;
import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditConfigLinkBlock;
import dev.hephaestus.glowcase.util.TextUtils;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ConfigLinkBlockEditScreen extends GlowcaseScreen {
    private final ConfigLinkBlockEntity configLinkBlockEntity;

    private EditBox titleEntryWidget;
    private EditBox urlEntryWidget;

    public ConfigLinkBlockEditScreen(ConfigLinkBlockEntity configLinkBlockEntity) {
        this.configLinkBlockEntity = configLinkBlockEntity;
    }

    @Override
    public void init() {
        super.init();

        if (this.minecraft == null) return;

        this.titleEntryWidget = new EditBox(this.minecraft.font, width / 10, height / 2 - 30, 8 * width / 10, 20, Component.empty());
        this.titleEntryWidget.setMaxLength(HyperlinkBlockEntity.TITLE_MAX_LENGTH);
        this.titleEntryWidget.setValue(this.configLinkBlockEntity.getTitle());
        this.titleEntryWidget.setHint(TextUtils.placeholder("gui.glowcase.title"));

        this.urlEntryWidget = new EditBox(this.minecraft.font, width / 10, height / 2 + 10, 8 * width / 10, 20, Component.empty());
        this.urlEntryWidget.setMaxLength(HyperlinkBlockEntity.URL_MAX_LENGTH);
        this.urlEntryWidget.setValue(this.configLinkBlockEntity.getUrl());
        this.urlEntryWidget.setHint(TextUtils.placeholder("gui.glowcase.url"));

        this.addRenderableWidget(this.titleEntryWidget);
        this.addRenderableWidget(this.urlEntryWidget);
    }

	@Override
	public boolean keyPressed(KeyEvent event) {
		int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        } else if (this.titleEntryWidget.canConsumeInput()) {
            return this.titleEntryWidget.keyPressed(event);
        } else if (this.urlEntryWidget.canConsumeInput()) {
            return this.urlEntryWidget.keyPressed(event);
        } else {
            return false;
        }
    }

    @Override
    public void onClose() {
        configLinkBlockEntity.setUrl(urlEntryWidget.getValue());
        configLinkBlockEntity.setTitle(titleEntryWidget.getValue());
        C2SEditConfigLinkBlock.of(configLinkBlockEntity).send();
        super.onClose();
    }
}
