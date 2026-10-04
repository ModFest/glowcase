package dev.hephaestus.glowcase.util;

import com.mojang.serialization.Codec;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Offset Anchor
 *
 * @author SuperKat
 **/
public enum Anchor implements StringRepresentable {
	TOP_LEFT(-1, 1),
	TOP(0, 1),
	TOP_RIGHT(1, 1),
	MIDDLE_LEFT(-1, 0),
	MIDDLE(0, 0),
	MIDDLE_RIGHT(1, 0),
	BOTTOM_LEFT(-1, -1),
	BOTTOM(0, -1),
	BOTTOM_RIGHT(1, -1);

	public static final Codec<Anchor> CODEC = StringRepresentable.fromEnum(Anchor::values);
	public static final StreamCodec<ByteBuf, Anchor> STREAM_CODEC = ByteBufCodecs.BYTE.map(
		index -> Anchor.values()[index],
		anchor -> (byte) anchor.ordinal()
	);

	@Deprecated(forRemoval = true)
	public static Anchor fromHorizontalAlignment(TextBlockEntity.HorizontalAlignment horizontalAlignment) {
		return horizontalAlignment.anchor;
	}

	private final int x;
	private final int y;

	Anchor(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	@Override
	public String getSerializedName() {
		return this.name().toLowerCase();
	}
}
