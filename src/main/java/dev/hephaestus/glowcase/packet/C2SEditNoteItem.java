package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.item.component.NoteComponent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record C2SEditNoteItem(NoteComponent noteComponent) implements CustomPacketPayload {
	public static final Type<C2SEditNoteItem> ID = new Type<>(Glowcase.id("channel.note_item"));

	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditNoteItem> PACKET_CODEC = StreamCodec.composite(
		NoteComponent.TYPE.streamCodec(), C2SEditNoteItem::noteComponent,
		C2SEditNoteItem::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public void receive(ServerPlayNetworking.Context context) {
		ItemStack stack = context.player().getMainHandItem();
		if (!(stack.is(Glowcase.NOTE_ITEM.get()))) return;

		if (stack.has(Glowcase.NOTE_COMPONENT.get())) {
			NoteComponent existingNote = stack.get(Glowcase.NOTE_COMPONENT.get());
			assert existingNote != null;
			if (existingNote.title().isPresent())
				return; // Already signed; This copy must not be modified
		}

		stack.set(Glowcase.NOTE_COMPONENT.get(), noteComponent);
	}
}
