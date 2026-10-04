package dev.hephaestus.glowcase.util;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 *
 **/
public enum TextJustify implements StringRepresentable {
	LEFT,
	CENTER,
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
	RIGHT;

	public static final Codec<TextJustify> CODEC = StringRepresentable.fromEnum(TextJustify::values);

	public final Anchor anchor;

	TextJustify() {
		this.anchor = Anchor.MIDDLE;
	}

	TextJustify(final Anchor anchor) {
		this.anchor = anchor;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase();
	}
}
