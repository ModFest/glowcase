package dev.hephaestus.glowcase.client.util;

import com.terraformersmc.modmenu.ModMenu;
import dev.hephaestus.glowcase.util.ModSupportUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @author Ampflower
 **/
@Environment(EnvType.CLIENT)
public final class ConfigLinkClientUtil {
	private static final boolean modmenuAvailable = FabricLoader.getInstance().isModLoaded("modmenu");

	private static final Component glowcase = Component.translatable("block.glowcase.config_link_block");
	private static final Component missingModmenu = Component.translatable("gui.glowcase.config_link.missing.modmenu");

	public static Screen getConfigScreen(Minecraft client, String link) {
		Screen screen = getModScreen(client, link);

		if (screen != null) {
			return screen;
		}

		return notImplemented(client, link);
	}

	@Nullable
	public static Screen getModScreen(Minecraft client, String link) {
		String id = ModSupportUtil.getModId(link);

		if (id == null) {
			return null;
		}

		if (!modmenuAvailable) {
			return modmenuUnavailable(client);
		}

		Screen screen = ModMenu.getConfigScreen(id, null);

		if (screen != null) {
			return screen;
		}

		Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer(id);

		return mod
			.map(modContainer -> modScreenUnavailable(client, modContainer.getMetadata().getName()))
			.orElseGet(() -> modUnavailable(client, id));
	}

	private static Screen modScreenUnavailable(Minecraft client, String modName) {
		return notice(client, glowcase, Component.translatable("gui.glowcase.config_link.missing.mod_screen", modName));
	}

	private static Screen modUnavailable(Minecraft client, String modId) {
		return notice(client, glowcase, Component.translatable("gui.glowcase.config_link.missing.mod", modId));
	}

	private static Screen modmenuUnavailable(Minecraft client) {
		return notice(client, glowcase, missingModmenu);
	}

	private static Screen notImplemented(Minecraft client, String link) {
		return notice(client, glowcase, Component.translatable("gui.glowcase.config_link.missing.link", link));
	}

	private static Screen notice(Minecraft client, Component title, Component notice) {
		return new AlertScreen(() -> client.setScreen(null), title, notice, CommonComponents.GUI_OK, true);
	}
}
