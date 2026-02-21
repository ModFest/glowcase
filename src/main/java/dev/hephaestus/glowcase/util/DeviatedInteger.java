package dev.hephaestus.glowcase.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record DeviatedInteger(Integer mean, Integer stdDev) implements DeviatedValue<Integer> {
	public static final DeviatedInteger ZERO = new DeviatedInteger(0, 0);

	public static final Codec<DeviatedInteger> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("mean").forGetter(DeviatedInteger::mean),
		Codec.INT.fieldOf("std_dev").forGetter(DeviatedInteger::stdDev)
	).apply(instance, DeviatedInteger::new));

	public static final StreamCodec<ByteBuf, DeviatedInteger> PACKET_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		DeviatedInteger::mean,
		ByteBufCodecs.VAR_INT,
		DeviatedInteger::stdDev,
		DeviatedInteger::new
	);

	@Override
	public Integer get(Supplier<Double> random) {
		return mean + Mth.floor(random.get() * stdDev);
	}
}
