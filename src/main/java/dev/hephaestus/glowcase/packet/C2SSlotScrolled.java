package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record C2SSlotScrolled(int syncId, int revision, int slotIndex, int amount) implements CustomPacketPayload {
	public static final Type<C2SSlotScrolled> ID = new Type<>(Glowcase.id("slot_scrolled"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SSlotScrolled> PACKET_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, C2SSlotScrolled::syncId,
		ByteBufCodecs.VAR_INT, C2SSlotScrolled::revision,
		ByteBufCodecs.VAR_INT, C2SSlotScrolled::slotIndex,
		ByteBufCodecs.VAR_INT, C2SSlotScrolled::amount,
		C2SSlotScrolled::new
	);

	@Override
	public Type<C2SSlotScrolled> type() {
		return ID;
	}
}
