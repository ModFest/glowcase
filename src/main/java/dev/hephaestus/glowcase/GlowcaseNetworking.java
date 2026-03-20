package dev.hephaestus.glowcase;

import dev.hephaestus.glowcase.item.ScrollableItem;
import dev.hephaestus.glowcase.packet.C2SEditConfigLinkBlock;
import dev.hephaestus.glowcase.packet.C2SEditEntityDisplayBlock;
import dev.hephaestus.glowcase.packet.C2SEditHyperlinkBlock;
import dev.hephaestus.glowcase.packet.C2SEditItemAcceptorBlock;
import dev.hephaestus.glowcase.packet.C2SEditItemDisplayBlock;
import dev.hephaestus.glowcase.packet.C2SEditItemProviderBlock;
import dev.hephaestus.glowcase.packet.C2SEditNoteItem;
import dev.hephaestus.glowcase.packet.C2SEditOutlineBlock;
import dev.hephaestus.glowcase.packet.C2SEditParticleDisplayBlock;
import dev.hephaestus.glowcase.packet.C2SEditPopupBlock;
import dev.hephaestus.glowcase.packet.C2SEditRecipeBlock;
import dev.hephaestus.glowcase.packet.C2SEditScreenBlock;
import dev.hephaestus.glowcase.packet.C2SEditSoundBlock;
import dev.hephaestus.glowcase.packet.C2SEditSpriteBlock;
import dev.hephaestus.glowcase.packet.C2SEditTabletItem;
import dev.hephaestus.glowcase.packet.C2SEditTextBlock;
import dev.hephaestus.glowcase.packet.C2SSlotScrolled;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GlowcaseNetworking {
	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(C2SEditHyperlinkBlock.ID, C2SEditHyperlinkBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditConfigLinkBlock.ID, C2SEditConfigLinkBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditItemDisplayBlock.ID, C2SEditItemDisplayBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditTextBlock.ID, C2SEditTextBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditPopupBlock.ID, C2SEditPopupBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditRecipeBlock.ID, C2SEditRecipeBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditSpriteBlock.ID, C2SEditSpriteBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditOutlineBlock.ID, C2SEditOutlineBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditParticleDisplayBlock.ID, C2SEditParticleDisplayBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditSoundBlock.ID, C2SEditSoundBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditItemAcceptorBlock.ID, C2SEditItemAcceptorBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditScreenBlock.ID, C2SEditScreenBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditItemProviderBlock.ID, C2SEditItemProviderBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditTabletItem.ID, C2SEditTabletItem.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditNoteItem.ID, C2SEditNoteItem.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SEditEntityDisplayBlock.ID, C2SEditEntityDisplayBlock.PACKET_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(C2SSlotScrolled.ID, C2SSlotScrolled.PACKET_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(C2SEditHyperlinkBlock.ID, C2SEditHyperlinkBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditConfigLinkBlock.ID, C2SEditConfigLinkBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditItemDisplayBlock.ID, C2SEditItemDisplayBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditTextBlock.ID, C2SEditTextBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditPopupBlock.ID, C2SEditPopupBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditRecipeBlock.ID, C2SEditRecipeBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditSpriteBlock.ID, C2SEditSpriteBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditOutlineBlock.ID, C2SEditOutlineBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditParticleDisplayBlock.ID, C2SEditParticleDisplayBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditSoundBlock.ID, C2SEditSoundBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditItemAcceptorBlock.ID, C2SEditItemAcceptorBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditScreenBlock.ID, C2SEditScreenBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditItemProviderBlock.ID, C2SEditItemProviderBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditTabletItem.ID, C2SEditTabletItem::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditNoteItem.ID, C2SEditNoteItem::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SEditEntityDisplayBlock.ID, C2SEditEntityDisplayBlock::receive);
		ServerPlayNetworking.registerGlobalReceiver(C2SSlotScrolled.ID, GlowcaseNetworking::slotScrolled);
	}

	/**
	 * @author zacharybarbanell
	 */
	private static void slotScrolled(C2SSlotScrolled packet, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayer player = ctx.player();
			player.resetLastActionTime();
			AbstractContainerMenu screenHandler = player.containerMenu;

			if (screenHandler.containerId != packet.syncId()) {
				return;
			}
			if (player.isSpectator()) {
				screenHandler.sendAllDataToRemote();
				return;
			}
			if (!screenHandler.stillValid(player)) {
				Glowcase.LOGGER.debug("Player {} interacted with invalid menu {}", player, screenHandler);
				return;
			}
			if (!screenHandler.isValidSlotIndex(packet.slotIndex())) {
				Glowcase.LOGGER.debug("Player {} clicked invalid slot index: {}, available slots: {}", player.getName(), packet.slotIndex(), screenHandler.slots.size());
				return;
			}
			boolean flag = packet.revision() == player.containerMenu.getStateId();
			screenHandler.suppressRemoteUpdates();
			Slot slot = screenHandler.getSlot(packet.slotIndex());
			ItemStack stack = slot.getItem();
			if (stack.getItem() instanceof ScrollableItem si) {
				si.scroll(stack, player, packet.amount());
			}
			screenHandler.resumeRemoteUpdates();
			if (flag) {
				screenHandler.broadcastFullState();
			} else {
				screenHandler.broadcastChanges();
			}
		});
	}
}
