package dev.hephaestus.glowcase.util;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/**
 *
 **/
public enum TextJustify implements StringRepresentable {
	LEFT(0),
	CENTER(1),
	/**
	 * @deprecated Use {@link #CENTER} with {@link Anchor#MIDDLE_LEFT}
	 */
	@Deprecated(forRemoval = true)
	CENTER_LEFT(Anchor.MIDDLE_LEFT),
	/**
	 * @deprecated Use {@link #CENTER} with {@link Anchor#MIDDLE_RIGHT}
	 */
	@Deprecated(forRemoval = true)
	CENTER_RIGHT(Anchor.MIDDLE_RIGHT),
	RIGHT(2);

	private static final List<TextJustify> LEGACY_ALIGNMENT = List.of(LEFT, CENTER, RIGHT);

	private static final Codec<TextJustify> BYTE_CODEC = Codec.BYTE.xmap(LEGACY_ALIGNMENT::get, v -> v.legacyOrdinal);
	public static final Codec<TextJustify> CODEC = StringRepresentable.fromEnum(TextJustify::values);
	public static final Codec<TextJustify> LEGACY_CODEC = CODEC.withAlternative(BYTE_CODEC);

	private final byte legacyOrdinal;
	public final Anchor anchor;

	TextJustify(final int legacyOrdinal) {
		this.legacyOrdinal = (byte) legacyOrdinal;
		this.anchor = Anchor.MIDDLE;
	}

	TextJustify(final Anchor anchor) {
		this.legacyOrdinal = -1;
		this.anchor = anchor;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase();
	}
}
