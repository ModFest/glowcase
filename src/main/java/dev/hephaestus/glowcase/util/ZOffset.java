package dev.hephaestus.glowcase.util;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Z Offset
 *
 * @deprecated Will be replaced with general positioning.
 * 	This will only remain for migration and GUI purposes.
 **/
@Deprecated
public enum ZOffset implements StringRepresentable {
	FRONT(0.4F),
	CENTER(0.01F), // but, why?
	BACK(-0.4F);

	public static final Codec<ZOffset> CODEC = StringRepresentable.fromEnum(ZOffset::values);

	private static final List<ZOffset> VALUES = List.of(ZOffset.values());

	// actually forPrivatization, but it's to be eventually removed anyways.
	@Deprecated(forRemoval = true)
	public final float offset;

	ZOffset(final float offset) {
		this.offset = offset;
	}

	public Vec3 setX(Vec3 pos) {
		return new Vec3(offset, pos.y, pos.z);
	}

	public Vec3 setY(Vec3 pos) {
		return new Vec3(pos.x, offset, pos.z);
	}

	public Vec3 setZ(Vec3 pos) {
		return new Vec3(pos.x, pos.y, offset);
	}

	public ZOffset next() {
		return VALUES.get((this.ordinal() + 1) % VALUES.size());
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase();
	}
}
