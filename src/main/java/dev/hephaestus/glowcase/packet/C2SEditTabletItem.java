package dev.hephaestus.glowcase.packet;

import com.mojang.datafixers.util.Pair;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ScreenBlockEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;

public record C2SEditTabletItem(int index, String url, String alt) implements CustomPacketPayload {
	public static final Type<C2SEditTabletItem> ID = new Type<>(Glowcase.id("channel.slide_tablet"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditTabletItem> PACKET_CODEC = StreamCodec.composite(
		ByteBufCodecs.INT, C2SEditTabletItem::index,
		ByteBufCodecs.STRING_UTF8, C2SEditTabletItem::url,
		ByteBufCodecs.STRING_UTF8, C2SEditTabletItem::alt,
		C2SEditTabletItem::new
	);

	public static C2SEditTabletItem of(int index, String url, String alt) {
		Pair<String, String> trimmed = ScreenBlockEntity.trimStr(url, alt);
		return new C2SEditTabletItem(index, trimmed.getFirst(), trimmed.getSecond());
	}

	public void receive(ServerPlayNetworking.Context context) {
		ItemStack stack = context.player().getMainHandItem();
		if (!(stack.is(Glowcase.TABLET_ITEM.get()))) return;

		Pair<String, String> trimmed = ScreenBlockEntity.trimStr(this.url, this.alt);
		Pair<String, String> slide = new Pair<>(trimmed.getFirst(), trimmed.getSecond());

		// We need a modifiable variant
		ArrayList<Pair<String, String>> slideshow = new ArrayList<>(stack.getOrDefault(Glowcase.SLIDESHOW_COMPONENT.get(), new ArrayList<>()));

		if (this.index > slideshow.size())
			return;
		else if (this.index == slideshow.size())
			slideshow.add(this.index, slide);
		else
			slideshow.set(this.index, slide);

		// Trim "gaps" to only allow one at most instead of many next to each other
		for (int i=0; i < slideshow.size()-1; i++) {
			Pair<String, String> current = slideshow.get(i);
			Pair<String, String> next = slideshow.get(i+1);

			// Is current and next a gap?
			if (current.getFirst().isEmpty() && current.getSecond().isEmpty() && next.getFirst().isEmpty() && next.getSecond().isEmpty()) {
				slideshow.remove(i+1);
				i--;
			}
		}

		stack.set(Glowcase.SLIDESHOW_COMPONENT.get(), slideshow);
	}

	public void send() {
		ClientPlayNetworking.send(this);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
