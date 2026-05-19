package dev.hephaestus.glowcase.client.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.joml.Quaternionf;

/**
 * @author Ampflower
 **/
public final class Quaternionsf {
	private Quaternionsf() {
	}

	public static Quaternionf rotateYX(Vec2 vec) {
		return rotateYXZ(vec.y, vec.x, 0);
	}

	public static Quaternionf rotateYXZ(float y, float x, float z) {
		return new Quaternionf().rotateYXZ(y, x, z);
	}

	public static Quaternionf rotateDegreesYXZ(float y, float x, float z) {
		return rotateYXZ(y * Mth.DEG_TO_RAD, x * Mth.DEG_TO_RAD, z * Mth.DEG_TO_RAD);
	}

	public static Quaternionf rotateXYZ(float x, float y, float z) {
		return new Quaternionf().rotateXYZ(x, y, z);
	}

	public static Quaternionf rotateDegreesXYZ(float x, float y, float z) {
		return rotateXYZ(x * Mth.DEG_TO_RAD, y * Mth.DEG_TO_RAD, z * Mth.DEG_TO_RAD);
	}
}
