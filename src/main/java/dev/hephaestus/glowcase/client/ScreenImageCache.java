package dev.hephaestus.glowcase.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.datafixers.util.Pair;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.util.HTTPException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.PngInfo;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>This class is a simple screen texture cache.
 * The cache itself is very simple: It attempts to fetch new urls if they are not in the cache yet.</p>
 *
 * <p>As of right now, the cache has no garbage collector. Only restarting the game helps, though,
 * the textures itself are registered as dynamic textures.</p>
 */
public class ScreenImageCache {
	private final HashMap<String, ScreenTexture> cache = new HashMap<>();

	/**
	 * @param blockpos Only needed for logging to make it a bit easier to locate invalid screens.
	 */
	public ScreenTexture getImage(String address, @Nullable BlockPos blockpos) {
		if (!cache.containsKey(address)) {
			createImage(address, blockpos);
		}

		return cache.get(address);
	}

	private void createImage(String address, @Nullable BlockPos blockPos) {
		try {
			URI uri = getURI(address);

			if (uri.getScheme() != null && uri.getScheme().toLowerCase(Locale.ROOT).equals(Glowcase.MODID)) {
				// Local image
				cache.put(address, ScreenTexture.ofIdentifier(Identifier.tryBuild(Glowcase.MODID, uri.getSchemeSpecificPart())));
			} else {
				// Online image
				URL url = toURL(uri);
				cache.put(address, new ScreenTexture(url));
			}
		} catch (HTTPException e) {
			if (blockPos != null && Glowcase.CONFIG.logInvalidScreens.value())
				Glowcase.LOGGER.warn("Screen at [{}] failed: {} ({}). It's url was: '{}'", blockPos.toShortString(), e.getMessage(), e.getCode(), address);

			cache.put(address, new ScreenTexture(e.getCode()));
		} catch (Exception e) {
			// Catch-all branch, if something snuck past the checks and crashed anyways.
			Glowcase.LOGGER.error("Screen at [{}] failed. Its URL was: '{}'", blockPos, address, e);

			cache.put(address, ScreenTexture.MALFORMED_URL);
		}
	}

	/**
	 * Parses a given address.
	 */
	private URI getURI(String address) throws HTTPException {
		URI uri;
		try {
			uri = new URI(address);
		} catch (Exception e) {
			throw new HTTPException("Malformed URL", 920);
		}

		return uri;
	}

	/**
	 * Converts the given uri to an url and ensures its safety.
	 */
	private static URL toURL(URI uri) throws HTTPException {
		validateURI(uri);
		ensureRules(uri);

		URL url;
		try {
			url = uri.toURL();
		} catch (MalformedURLException e) {
			throw new HTTPException("Malformed URL", 920);
		}

		return url;
	}

	/**
	 * Check if url is well formatted. IPs are not allowed (for now) because I can't be bothered.
	 */
	private static void validateURI(URI uri) throws HTTPException {
		// Validate Scheme
		if (uri.getScheme() == null)
			throw new HTTPException("Protocol must be specified (http or https)", 901);

		String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
		if (!(scheme.equals("http") || scheme.equals("https")))
			throw new HTTPException("Invalid protocol; Only http or https are allowed", 901);

		if (uri.getHost() == null) {
			throw new HTTPException("Host is missing.", 920);
		}

		// For now, all IP addresses will be filtered for the sake of the whitelist and blacklist.
		if (uri.getHost().chars().noneMatch(Character::isLetter))
			throw new HTTPException("Host has to be a domain", 403);
	}

	/**
	 * Ensures we are allowed to use this url.
	 */
	private static void ensureRules(URI uri) throws HTTPException {
		String host = uri.getHost();

		for (String rule : Glowcase.CONFIG.whitelist.value()) {
			if (matches(host, rule))
				return;
		}

		for (String rule : Glowcase.CONFIG.blacklist.value()) {
			if (matches(host, rule))
				throw new HTTPException("Given url is not whitelisted", 403);
		}
	}

	/**
	 * <p>Compares the host with the given rule.</p>
	 *
	 * <p>This method attempts to add support for wildcards.</p>
	 */
	private static boolean matches(String host, String rule) {
		if (rule.contains("*")) {
			if (rule.equals("*"))
				return true;

			if (rule.startsWith("*.")) {
				String ruleDomain = rule.substring(2);
				if (host.endsWith(ruleDomain)) {
					// Ensure the wildcard matches only one level
					String hostWithoutRule = host.substring(0, host.length() - ruleDomain.length() - 1);
					return !hostWithoutRule.contains(".");
				}
				return false;
			}
		}

		return host.equals(rule);
	}

	/**
	 * Container for a texture. Will attempt to fetch the actual texture on creation.
	 * If an error happened, the texture will be null.
	 */
	public static class ScreenTexture {
		private final CompletableFuture<Integer> loader;
		@Nullable
		private Identifier texture;

		private int width = 0;
		private int height = 0;

		private static final CompletableFuture<Integer> COMPLETED = CompletableFuture.completedFuture(200);

		public static final ScreenTexture MALFORMED_URL = new ScreenTexture(920);
		public static final ScreenTexture MALFORMED_IDENTIFIER = new ScreenTexture(921);
		public static final ScreenTexture MISSING = new ScreenTexture(404);
		public static final ScreenTexture UNSUPPORTED_IMAGE_FORMAT = new ScreenTexture(903);

		public Pair<Integer, Identifier> getTexture() {
			if (loader.isDone()) {
				return new Pair<>(loader.join(), texture);
			}

			return new Pair<>(102, texture);
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		/**
		 * Creates a new empty texture with the given status code.
		 */
		public ScreenTexture(int code) {
			loader = CompletableFuture.completedFuture(code);
		}

		private ScreenTexture(@NotNull Identifier texture, int width, int height) {
			this.loader = COMPLETED;
			this.texture = texture;
			this.width = width;
			this.height = height;
		}

		/**
		 * Attempts to create a new texture by fetching a image from the given url.
		 */
		public ScreenTexture(URL url) {
			loader = CompletableFuture.supplyAsync(() -> {
				// Fetch URL
				HttpURLConnection connection;
				InputStream stream;
				try {
					connection = (HttpURLConnection) url.openConnection(Minecraft.getInstance().getProxy());
					connection.setDoInput(true);
					connection.setDoOutput(false);
					connection.connect();

					int status = connection.getResponseCode();
					if (status / 100 != 2) return status; // An actual status code for once here

					stream = connection.getInputStream();
				} catch (SocketTimeoutException e) {
					return 408; // Request Timeout
				} catch (Exception e) {
					return 902; // Unable to create a connection
				}

				// Parse image
				NativeImage nativeImage;
				try {
					nativeImage = NativeImage.read(stream);
				} catch (IOException e) {
					return 903; // Unable to parse image.
				}

				// TODO: GIF support? STBImage _should_ support gif, but I didn't manage to make it work yet

				// TODO: Perhaps adding a local file cache might be wise

				int result = Minecraft.getInstance().submit(() -> {
					width = nativeImage.getWidth();
					height = nativeImage.getHeight();

					String imageHash = Integer.toHexString(nativeImage.hashCode());
					DynamicTexture nativeTexture = new DynamicTexture(() -> imageHash, nativeImage);

					// Register image as texture
					TextureManager textureManager = Minecraft.getInstance().getTextureManager();
					this.texture = Glowcase.id("glowcase/img", imageHash);
					textureManager.register(this.texture, nativeTexture);

					return 200;
				}).join();

				connection.disconnect();
				return result;
			}, Util.backgroundExecutor());
		}

		public static ScreenTexture ofIdentifier(@Nullable Identifier texture) {
			if (texture == null) {
				return MALFORMED_IDENTIFIER;
			}

			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);

			if (resource.isEmpty()) {
				return MISSING;
			}

			try (final InputStream input = resource.get().open()) {
				byte[] bytes = input.readAllBytes();
				PngInfo.validateHeader(ByteBuffer.wrap(bytes));
			} catch (IOException e) {
				return UNSUPPORTED_IMAGE_FORMAT;
			}

			GpuTexture raw = Minecraft.getInstance().getTextureManager().getTexture(texture).getTexture();

			return new ScreenTexture(texture,
				raw.getWidth(0),
				raw.getHeight(0)
			);
		}
	}
}
