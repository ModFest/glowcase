package dev.hephaestus.glowcase.packet;

import com.mojang.datafixers.util.Pair;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ScreenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record C2SEditScreenBlock(BlockPos pos, float width, float height, ScreenBlockEntity.Offset xOffset, ScreenBlockEntity.Offset yOffset, ScreenBlockEntity.Offset zOffset, float pitch, float yaw, boolean renderBackface, boolean eink, boolean stretch, String url, String alt, float preciseX, float preciseY, float preciseZ) implements C2SEditBlockEntity {
	public static final Type<C2SEditScreenBlock> ID = new Type<>(Glowcase.id("channel.screen_block"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditScreenBlock> PACKET_CODEC = StreamCodec.ofMember(
		(packet, buf) -> {
			Pair<String, String> trimmed = ScreenBlockEntity.trimStr(packet.url, packet.alt);

			BlockPos.STREAM_CODEC.encode(buf, packet.pos);
			ByteBufCodecs.FLOAT.encode(buf, packet.width);
			ByteBufCodecs.FLOAT.encode(buf, packet.height);
			ByteBufCodecs.BYTE.encode(buf, (byte) packet.xOffset.ordinal());
			ByteBufCodecs.BYTE.encode(buf, (byte) packet.yOffset.ordinal());
			ByteBufCodecs.BYTE.encode(buf, (byte) packet.zOffset.ordinal());
			ByteBufCodecs.FLOAT.encode(buf, packet.pitch);
            ByteBufCodecs.FLOAT.encode(buf, packet.yaw);
			ByteBufCodecs.BOOL.encode(buf, packet.renderBackface);
			ByteBufCodecs.BOOL.encode(buf, packet.eink);
			ByteBufCodecs.BOOL.encode(buf, packet.stretch);
			ByteBufCodecs.STRING_UTF8.encode(buf, trimmed.getFirst());
			ByteBufCodecs.STRING_UTF8.encode(buf, trimmed.getSecond());
			ByteBufCodecs.FLOAT.encode(buf, packet.preciseX);
            ByteBufCodecs.FLOAT.encode(buf, packet.preciseY);
            ByteBufCodecs.FLOAT.encode(buf, packet.preciseZ);
		},
		(buf) -> new C2SEditScreenBlock(BlockPos.STREAM_CODEC.decode(buf),
			ByteBufCodecs.FLOAT.decode(buf),
			ByteBufCodecs.FLOAT.decode(buf),
			ScreenBlockEntity.Offset.values()[ByteBufCodecs.BYTE.decode(buf)],
			ScreenBlockEntity.Offset.values()[ByteBufCodecs.BYTE.decode(buf)],
			ScreenBlockEntity.Offset.values()[ByteBufCodecs.BYTE.decode(buf)],
			ByteBufCodecs.FLOAT.decode(buf),
            ByteBufCodecs.FLOAT.decode(buf),
			ByteBufCodecs.BOOL.decode(buf),
			ByteBufCodecs.BOOL.decode(buf),
			ByteBufCodecs.BOOL.decode(buf),
			ByteBufCodecs.STRING_UTF8.decode(buf),
			ByteBufCodecs.STRING_UTF8.decode(buf),
			ByteBufCodecs.FLOAT.decode(buf),
            ByteBufCodecs.FLOAT.decode(buf),
            ByteBufCodecs.FLOAT.decode(buf))
	);

	public static C2SEditScreenBlock of(ScreenBlockEntity be) {
		return new C2SEditScreenBlock(be.getBlockPos(), be.width, be.height, be.xOffset, be.yOffset, be.zOffset, be.pitch, be.yaw, be.renderBackface, be.eink, be.stretch, be.url, be.alt, be.preciseX, be.preciseY, be.preciseZ);
	}

	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		if (!(blockEntity instanceof ScreenBlockEntity be)) return;

		Pair<String, String> trimmed = ScreenBlockEntity.trimStr(url, alt);

		be.setupScreen(this.width, this.height, this.xOffset, this.yOffset, this.zOffset, this.pitch, this.yaw, this.eink, this.stretch, this.renderBackface);

		be.preciseX = this.preciseX;
        be.preciseY = this.preciseY;
        be.preciseZ = this.preciseZ;

		be.setImage(trimmed.getFirst(), trimmed.getSecond(), null); // Does markDirty and dispatch for us
	}

	@Override
	public Type<C2SEditScreenBlock> type() {
		return ID;
	}
}
