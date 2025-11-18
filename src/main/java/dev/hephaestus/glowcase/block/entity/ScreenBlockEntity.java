package dev.hephaestus.glowcase.block.entity;

import com.mojang.datafixers.util.Pair;
import dev.hephaestus.glowcase.Glowcase;
//import dev.hephaestus.glowcase.client.GlowcaseClient;
import net.minecraft.block.BlockState;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

public class ScreenBlockEntity extends GlowcaseBlockEntity {
	public static final int URL_MAX_LENGTH = 1024;
	public static final int ALT_MAX_LENGTH = 1024;

	public UUID macaddress = UUID.randomUUID();

	public String url = "";
	public String alt = "";

	public String preview = "";

	public float width = 1f;
	public float height = 1f;

	public Offset xOffset = Offset.NULL;
	public Offset yOffset = Offset.NULL;
	public Offset zOffset = Offset.NULL;

	public float preciseX = 0f;
	public float preciseY = 0f;
	public float preciseZ = 0f;

	public float pitch = 0f;
	public float yaw = 0f;

	public boolean renderBackface = false;
	public boolean stretch = false;
	public boolean eink = true;

	/**
	 * <p>Used in network code to ensure maximum length.</p>
	 *
	 * <p>Can be avoided by directly editing the NBT on purpose (which kinda acts as a sanity check).</p>
	 */
	public static Pair<String, String> trimStr(String url, String alt) {
		String trimmed_url = url.substring(0, Math.min(url.length(), ScreenBlockEntity.URL_MAX_LENGTH));
		String trimmed_alt = alt.substring(0, Math.min(alt.length(), ScreenBlockEntity.ALT_MAX_LENGTH));

		return new Pair<>(trimmed_url, trimmed_alt);
	}

	public enum Offset {
		NEGATIVE(-1), NULL(0), POSITIVE(1);

		public final int offset;
		Offset(int offset) {
			this.offset = offset;
		}

		public static Offset fromOffset(int offset) {
			if (offset < 0)
				return NEGATIVE;
			else if (offset > 0)
				return POSITIVE;

			return NULL;
		}
	}

	public ScreenBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.SCREEN_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);

		view.put("macaddress", Uuids.INT_STREAM_CODEC, macaddress);

		view.putFloat("width", width);
		view.putFloat("height", height);

		view.putBoolean("renderBackface", renderBackface);
		view.putBoolean("stretch", stretch);
		view.putBoolean("eink", eink);

		view.putInt("x_offset", this.xOffset.offset);
		view.putInt("y_offset", this.yOffset.offset);
		view.putInt("z_offset", this.zOffset.offset);

		view.putFloat("px", this.preciseX);
		view.putFloat("py", this.preciseY);
		view.putFloat("pz", this.preciseZ);

		view.putFloat("pitch", this.pitch);
		view.putFloat("yaw", this.yaw);

		view.putString("url", url);
		view.putString("alt", alt);

		view.putString("preview", preview);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);

		macaddress = view.read("macaddress", Uuids.INT_STREAM_CODEC).orElseGet(UUID::randomUUID);

		width = view.getFloat("width", 1);
		height = view.getFloat("height", 1);

		renderBackface = view.getBoolean("renderBackface", false);
		stretch = view.getBoolean("stretch", false);
		eink = view.getBoolean("eink", false);

		xOffset = Offset.fromOffset(view.getInt("x_offset", 0));
		yOffset = Offset.fromOffset(view.getInt("y_offset", 0));
		zOffset = Offset.fromOffset(view.getInt("z_offset", 0));

		preciseX = view.getFloat("px", 0);
		preciseY = view.getFloat("py", 0);
		preciseZ = view.getFloat("pz", 0);

		pitch = view.getFloat("pitch", 0);
		yaw = view.getFloat("yaw", 0);

		url = view.getString("url", "");
		alt = view.getString("alt", "");

		// Cache preview before needed for smooth experience
		preview = view.getString("preview", "");
		if (this.getWorld() != null && this.getWorld().isClient()) {
			// Todo
			//GlowcaseClient.screenImageCache.getImage(preview, null);
		}

		markDirty();
	}

	public void setImage(String url, String alt, @Nullable String preview) {
		this.url = url;
		this.alt = alt;

		if (preview != null)
			this.preview = preview;

		markDirty();
	}

	public void setupScreen(float width, float height, Offset xOffset, Offset yOffset, Offset zOffset, float pitch, float yaw, boolean eink, boolean stretch, boolean renderBackface) {
		this.width = Math.clamp(width, 0.05f, Integer.MAX_VALUE);
		this.height = Math.clamp(height, 0.05f, Integer.MAX_VALUE);
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.zOffset = zOffset;
		this.pitch = pitch;
		this.yaw = yaw;
		this.eink = eink;
		this.stretch = stretch;
		this.renderBackface = renderBackface;
	}

	// returns a combined offset 
	public Vector3f getOffset() {
		float x = 0f;
		float y = 0f;
		float z = 0f;

		// moved front/back stuff here
		if (xOffset == Offset.POSITIVE) {
			x = width / 2f - 0.5f;
		} else if (xOffset == Offset.NEGATIVE) {
			x = -width / 2f + 0.5f;
		}

		if (yOffset == Offset.POSITIVE) {
			y = height / 2f - 0.5f;
		} else if (yOffset == Offset.NEGATIVE) {
			y = -height / 2f + 0.5f;
		}

		if (zOffset == Offset.POSITIVE) {
			z = -0.45f;
		} else if (zOffset == Offset.NEGATIVE) {
			z = 0.45f;
		}

		x += preciseX;
		y += preciseY;
		z += preciseZ;

		return new Vector3f(x, y, z);
	}

	// to set precise offset
	public void setOffset(Vector3f offset) {
		this.preciseX = offset.x();
		this.preciseY = offset.y();
		this.preciseZ = offset.z();

		markDirty();
	}
}
