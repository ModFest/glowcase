package dev.hephaestus.glowcase.util;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Z Offset. Historically for general positioning, soon to remain only for
 * GUI use and migration purposes, and the oddball text utility.
 **/
public enum ZOffset implements StringRepresentable {
	FRONT(0.4F),
	CENTER(0.01F), // but, why?
	BACK(-0.4F);

	public static final Codec<ZOffset> CODEC = StringRepresentable.fromEnum(ZOffset::values);

	private static final List<ZOffset> VALUES = List.of(ZOffset.values());

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

	public static Vec3 adjustPlacement(final Vec3 current, final float xRot) {
		if (MathUtils.equals(current.z, 0.0, 0.1) && Math.abs(xRot) < 30) {
			return BACK.setZ(current);
		}
		if (MathUtils.equals(current.z, -0.4, 0.1) && Math.abs(xRot) > 60) {
			return CENTER.setZ(current);
		}
		return current;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase();
	}
}
