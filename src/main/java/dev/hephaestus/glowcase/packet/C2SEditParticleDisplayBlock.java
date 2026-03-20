package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ParticleDisplayBlockEntity;
import dev.hephaestus.glowcase.util.DeviatedInteger;
import dev.hephaestus.glowcase.util.DeviatedVec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record C2SEditParticleDisplayBlock(
	ParticleOptions particle,
	DeviatedInteger count,
	DeviatedVec3d velocity,
	DeviatedVec3d position,
	DeviatedInteger tickRate,
	BlockPos blockPos
) implements C2SEditBlockEntity {
	public static final Type<C2SEditParticleDisplayBlock> ID = new Type<>(Glowcase.id("channel.particle_display.save"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditParticleDisplayBlock> PACKET_CODEC = StreamCodec.composite(
		ParticleTypes.STREAM_CODEC, C2SEditParticleDisplayBlock::particle,
		DeviatedInteger.PACKET_CODEC, C2SEditParticleDisplayBlock::count,
		DeviatedVec3d.PACKET_CODEC, C2SEditParticleDisplayBlock::velocity,
		DeviatedVec3d.PACKET_CODEC, C2SEditParticleDisplayBlock::position,
		DeviatedInteger.PACKET_CODEC, C2SEditParticleDisplayBlock::tickRate,
		BlockPos.STREAM_CODEC, C2SEditParticleDisplayBlock::blockPos,
		C2SEditParticleDisplayBlock::new
	);

	public static C2SEditParticleDisplayBlock of(ParticleDisplayBlockEntity be) {
		return new C2SEditParticleDisplayBlock(be.particle, be.count, be.velocity, be.position, be.tickRate, be.getBlockPos());
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
		if (!(blockEntity instanceof ParticleDisplayBlockEntity be)) return;

		be.particle = this.particle();
		be.count = this.count();
		be.velocity = this.velocity();
		be.position = this.position();
		be.tickRate = this.tickRate();

		be.setChanged();
	}
}
