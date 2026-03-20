package dev.hephaestus.glowcase.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record DisplayBlockSettings(Vector3fc offset, Vector3fc scale, float pitch, float yaw, boolean renderAsBlock) {
	public static final Codec<DisplayBlockSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ExtraCodecs.VECTOR3F.lenientOptionalFieldOf("offset", new Vector3f()).forGetter(DisplayBlockSettings::offset),
		ExtraCodecs.VECTOR3F.lenientOptionalFieldOf("scale", new Vector3f(1.0F)).forGetter(DisplayBlockSettings::scale),
		Codec.FLOAT.lenientOptionalFieldOf("pitch", 0F).forGetter(DisplayBlockSettings::pitch),
		Codec.FLOAT.lenientOptionalFieldOf("yaw", 0F).forGetter(DisplayBlockSettings::yaw),
		Codec.BOOL.lenientOptionalFieldOf("renderAsBlock", false).forGetter(DisplayBlockSettings::renderAsBlock)
	).apply(instance, DisplayBlockSettings::new));

	public static final StreamCodec<ByteBuf, DisplayBlockSettings> PACKET_CODEC = StreamCodec.composite(
		ByteBufCodecs.VECTOR3F, DisplayBlockSettings::offset,
		ByteBufCodecs.VECTOR3F, DisplayBlockSettings::scale,
		ByteBufCodecs.FLOAT, DisplayBlockSettings::pitch,
		ByteBufCodecs.FLOAT, DisplayBlockSettings::yaw,
		ByteBufCodecs.BOOL, DisplayBlockSettings::renderAsBlock,
		DisplayBlockSettings::new
	);

	public DisplayBlockSettings() {
		this(new Vector3f(), new Vector3f(1.0F), 0F, 0F, false);
	}

	public boolean isEmpty() {
		return offset.equals(new Vector3f()) && scale.equals(new Vector3f(1.0F)) && pitch == 0F && yaw == 0F && renderAsBlock;
	}
}
