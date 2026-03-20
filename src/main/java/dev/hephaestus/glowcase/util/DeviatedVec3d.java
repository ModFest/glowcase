package dev.hephaestus.glowcase.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public record DeviatedVec3d(Vec3 mean, Vec3 stdDev) implements DeviatedValue<Vec3> {
	public static final DeviatedVec3d ZERO = new DeviatedVec3d(Vec3.ZERO, Vec3.ZERO);

	public static final Codec<DeviatedVec3d> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Vec3.CODEC.fieldOf("mean").forGetter(DeviatedVec3d::mean),
		Vec3.CODEC.fieldOf("std_dev").forGetter(DeviatedVec3d::stdDev)
	).apply(instance, DeviatedVec3d::new));

	public static final StreamCodec<ByteBuf, DeviatedVec3d> PACKET_CODEC = StreamCodec.composite(
		Vec3.STREAM_CODEC,
		DeviatedVec3d::mean,
		Vec3.STREAM_CODEC,
		DeviatedVec3d::stdDev,
		DeviatedVec3d::new
	);

	@Override
	public Vec3 get(Supplier<Double> random) {
		return new Vec3(
			mean.x + random.get() * stdDev.x,
			mean.y + random.get() * stdDev.y,
			mean.z + random.get() * stdDev.z
		);
	}
}
