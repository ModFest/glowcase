package dev.hephaestus.glowcase.client.util;

import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class NoteTextColorResource implements ResourceManagerReloadListener {
	private static final Identifier TEXTURE = Glowcase.id("textures/gui/note.png");

	public static int TXT_COLOR = 0x000000;

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		Optional<Resource> resource = manager.getResource(TEXTURE);

		if (resource.isPresent()) {
			try {
				InputStream inputStream = resource.get().open();
				BufferedImage image = ImageIO.read(inputStream);

				TXT_COLOR = image.getRGB(image.getWidth()-1, image.getHeight()-1);
			} catch (IOException ignored) { }
		}
	}
}
