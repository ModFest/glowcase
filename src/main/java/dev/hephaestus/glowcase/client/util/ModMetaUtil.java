package dev.hephaestus.glowcase.client.util;

import dev.hephaestus.glowcase.Glowcase;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.apache.commons.lang3.Validate;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Liberally stolen from ballotbox, which literally stole it from ModMenu. Thanks ModMenu!
 */
public class ModMetaUtil {
	private static final Map<Path, NativeImageBackedTexture> modIconCache = new HashMap<>();

	public static NativeImageBackedTexture createIcon(ModContainer iconSource, String iconPath) {
		try {
			Path path = iconSource.getPath(iconPath);
			NativeImageBackedTexture cachedIcon = modIconCache.get(path);
			if (cachedIcon != null) {
				return cachedIcon;
			}
			cachedIcon = modIconCache.get(path);
			if (cachedIcon != null) {
				return cachedIcon;
			}
			try (InputStream inputStream = Files.newInputStream(path)) {
				NativeImage image = NativeImage.read(Objects.requireNonNull(inputStream));
				Validate.validState(image.getHeight() == image.getWidth(), "Must be square icon");
				NativeImageBackedTexture tex = new NativeImageBackedTexture(image);
				modIconCache.put(path, tex);
				return tex;
			}

		} catch (IllegalStateException e) {
			if (e.getMessage().equals("Must be square icon")) {
				Glowcase.LOGGER.error("Mod icon must be a square for icon source {}: {}",
					iconSource.getMetadata().getId(),
					iconPath,
					e
				);
			}

			return null;
		} catch (Throwable t) {
			if (!iconPath.equals("assets/" + iconSource.getMetadata().getId() + "/icon.png")) {
				Glowcase.LOGGER.error("Invalid mod icon for icon source {}: {}", iconSource.getMetadata().getId(), iconPath, t);
			}
			return null;
		}
	}

	public static NativeImageBackedTexture getIcon(ModContainer mod, int preferredSize) {
		ModMetadata meta = mod.getMetadata();
		String modId = meta.getId();
		String iconPath = meta.getIconPath(preferredSize).orElse("assets/" + modId + "/icon.png");
		final String finalIconSourceId = modId;
		ModContainer iconSource = FabricLoader.getInstance()
			.getModContainer(modId)
			.orElseThrow(() -> new RuntimeException("Cannot get ModContainer for Fabric mod with id " + finalIconSourceId));
		NativeImageBackedTexture icon = createIcon(iconSource, iconPath);
		return icon;
	}
}
