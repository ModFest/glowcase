package dev.hephaestus.glowcase.client;

import dev.hephaestus.glowcase.packet.S2CCloseEditor;
import dev.hephaestus.glowcase.packet.S2COpenEditor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * @author Ampflower
 */
public final class GlowcaseClientNetworking {
	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(S2COpenEditor.ID, S2COpenEditor::receive);
		ClientPlayNetworking.registerGlobalReceiver(S2CCloseEditor.ID, S2CCloseEditor::receive);
	}
}
