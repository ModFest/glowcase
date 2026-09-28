package dev.hephaestus.glowcase.client.render.bakedbe;

import com.mojang.blaze3d.buffers.Std140Builder;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

@NullMarked
public record RenderOffset(@Nullable Vec3 cameraPos, Vector3i blockPos, Vector3f offset, Vector3f vector) implements DynamicGpuDataStorage.DynamicGpuData {
	public RenderOffset(@Nullable Vec3 cameraPos, Vec3 offset) {
		var vector = offset.toVector3f();

		int x = Mth.floor(offset.x);
		int y = Mth.floor(offset.y);
		int z = Mth.floor(offset.z);

		this(cameraPos, new Vector3i(x, y, z), new Vector3f(vector.x - x, vector.y - y, vector.z - z), vector);
	}
	@Override
	public void write(final ByteBuffer buffer) {
		Std140Builder.intoBuffer(buffer).putInt(1).putIVec3(blockPos).putVec3(offset);
	}
}
