package dev.hephaestus.glowcase.client.gui.screen.ingame;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ampflower
 */
public interface Editor {
	default @Nullable CustomPacketPayload getDeltaPayload() {
		return null;
	}

	@MustBeInvokedByOverriders
	default void sendDeltaPacket() {
		sendNullablePayload(this.getDeltaPayload());
	}

	@Nullable CustomPacketPayload getUpdatePayload();

	@MustBeInvokedByOverriders
	default void sendUpdatePacket() {
		sendNullablePayload(this.getUpdatePayload());
	}

	private static void sendNullablePayload(
		final @Nullable CustomPacketPayload payload
	) {
		if (payload != null) {
			ClientPlayNetworking.send(payload);
		}
	}
}
