package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.SoundPlayerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public record C2SEditSoundBlock(SoundInfo soundInfo, PositionalInfo positionalInfo, BlockPos blockPos) implements C2SEditBlockEntity {
	public static final Type<C2SEditSoundBlock> ID = new Type<>(Glowcase.id("channel.sound_block.save"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditSoundBlock> PACKET_CODEC = StreamCodec.composite(
		SoundInfo.PACKET_CODEC, C2SEditSoundBlock::soundInfo,
		PositionalInfo.PACKET_CODEC, C2SEditSoundBlock::positionalInfo,
		BlockPos.STREAM_CODEC, C2SEditSoundBlock::blockPos,
		C2SEditSoundBlock::new
	);

	public static C2SEditSoundBlock of(SoundPlayerBlockEntity be) {
		return new C2SEditSoundBlock(
			new SoundInfo(be.soundId, be.category.toString(), be.volume, be.pitch, be.repeatDelay, be.cancelOthers),
			new PositionalInfo(be.distance, be.relative, be.offset),
			be.getBlockPos()
		);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	public BlockPos pos() {
		return blockPos;
	}

	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		if (!(blockEntity instanceof SoundPlayerBlockEntity be)) return;

		be.soundId = soundInfo.id;
		be.category = SoundSource.valueOf(soundInfo.category);
		be.volume = soundInfo.volume;
		be.pitch = soundInfo.pitch;
		be.repeatDelay = soundInfo.repeatDelay;
		be.cancelOthers = soundInfo.cancelOthers;

		be.distance = positionalInfo.distance;
		be.relative = positionalInfo.relative;
		be.offset = positionalInfo.offset;

		be.setChanged();
	}

	public record SoundInfo(ResourceLocation id, String category, float volume, float pitch, int repeatDelay, boolean cancelOthers) {
		public static final StreamCodec<RegistryFriendlyByteBuf, SoundInfo> PACKET_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, SoundInfo::id,
			ByteBufCodecs.STRING_UTF8, SoundInfo::category,
			ByteBufCodecs.FLOAT, SoundInfo::volume,
			ByteBufCodecs.FLOAT, SoundInfo::pitch,
			ByteBufCodecs.INT, SoundInfo::repeatDelay,
			ByteBufCodecs.BOOL, SoundInfo::cancelOthers,
			SoundInfo::new
		);
	}

	public record PositionalInfo(float distance, boolean relative, Vec3 offset) {
		public static final StreamCodec<RegistryFriendlyByteBuf, PositionalInfo> PACKET_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, PositionalInfo::distance,
			ByteBufCodecs.BOOL, PositionalInfo::relative,
			ByteBufCodecs.fromCodec(Vec3.CODEC), PositionalInfo::offset,
			PositionalInfo::new
		);
	}
}
