package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.block.GlowcaseBlock;
import dev.hephaestus.glowcase.packet.S2CCloseEditor;
import dev.hephaestus.glowcase.packet.S2COpenEditor;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Trait-style implementation of editor locking, houses the bulk of the editor logic.
 *
 * @author Ampflower
 **/
public interface LockableEditor {

	/**
	 * Gets the current active lock holder.
	 */
	default @Nullable Player getLockHolder() {
		final Level level = this.getLevel();
		if (level == null) {
			return null;
		}
		final UUID editorLock = this.getEditorLock();
		if (editorLock == null) {
			return null;
		}
		if (!(level.getEntity(editorLock) instanceof Player player)) {
			return null;
		}
		return player;
	}

	/**
	 * Tests if the player is the current active lock holder, regardless of distance.
	 */
	default boolean isLockHolder(Player player) {
		return player.getUUID().equals(this.getEditorLock()) && GlowcaseBlock.canEditGlowcase(
			player,
			this.getBlockPos()
		);
	}

	/**
	 * Tests if the player is
	 */
	default boolean mayObtainLock(Player player) {
		final UUID editorLock = this.getEditorLock();
		return (editorLock == null && this.isCandidateValid(player)) || this.isLockHolder(player);
	}


	/**
	 * Tests the validity of the candidate entity.
	 */
	default boolean isCandidateValid(Entity entity) {
		if (!(entity instanceof Player player)) {
			return false;
		}

		// Original check was 12 blocks away.
		// Add 8 to get more or less about the right amount of buffer.
		if (!player.isWithinBlockInteractionRange(this.getBlockPos(), 8)) {
			return false;
		}

		return GlowcaseBlock.canEditGlowcase(player, this.getBlockPos());
	}

	private boolean intrudeIfInvalid(Player player) {
		final Player previousLockHolder = this.getLockHolder();
		if (previousLockHolder == null) {
			this.setEditorLock(player == null ? null : player.getUUID());
			return true;
		}

		if (this.isCandidateValid(previousLockHolder)) {
			// Candidate is within range and is able to edit glowcase.
			return false;
		}

		if (!trySendClose(previousLockHolder, this.getLevel(), this.getBlockPos())) {
			// Impossible challenge: how do you safely close the GUI in a system
			// that doesn't allow safely closing the GUI over the network?
			// Added on top of that: Display blocks do not update the server upon closure.
			return false;
		}

		this.setPendingEditor(!canSendOpen(player) ? null : player.getUUID());
		// Despite setting pending editor: we didn't actually obtain a lock!
		// Return false to avoid falsely opening an editor that can't edit.
		return false;
	}

	/**
	 * Tries to lock the block to the given player.
	 *
	 * @param player The player attempting to lock the block.
	 * @return Whether the player locked the block.
	 */
	default boolean lockEditor(Player player) {
		if (!isCandidateValid(player)) {
			return false;
		}
		if (this.isLockHolder(player)) {
			return true;
		}
		return this.intrudeIfInvalid(player);
	}

	/**
	 * Forcefully unlocks the editor.
	 * <p>
	 * Called when the lock is forcefully removed, i.e. when the block is freshly placed,
	 * or the block is destroyed.
	 *
	 * @return The previous lock holder, if any.
	 */
	default @Nullable Player intrudeUnlock() {
		final @Nullable Player previousLockHolder = this.getLockHolder();
		trySendClose(previousLockHolder, this.getLevel(), this.getBlockPos());
		this.setPendingEditor(null);
		return previousLockHolder;
	}

	/**
	 * Forcefully sets the new editor.
	 * <p>
	 * Called when the block is placed by a player.
	 *
	 * @param player The player editing this block.
	 * @return The previous lock holder, if any.
	 */
	default @Nullable Player intrudeEditor(Player player) {
		final @Nullable Player previousLockHolder = this.intrudeUnlock();
		this.setEditorLock(player.getUUID());
		return previousLockHolder;
	}

	/**
	 * Tries to unlock the block from the given player.
	 *
	 * @param player The player attempting to unlock the block.
	 * @return Whether the player unlocked the block.
	 */
	default boolean unlockEditor(Player player) {
		if (this.isLockHolder(player)) {
			this.handoffToPending();
			return true;
		}
		return this.intrudeIfInvalid(null);
	}

	private void handoffToPending() {
		final Level level = this.getLevel();
		if (level == null) {
			this.setEditorLock(null);
			return;
		}
		final UUID uuid = this.getPendingEditor();
		if (uuid == null
			// Valid server-only
			|| !(level.getEntity(uuid) instanceof ServerPlayer player)
			// DEPRECATED: inline on up-port or C<->S ABI break
			|| !ServerPlayNetworking.canSend(player, S2COpenEditor.ID)
		) {
			this.setEditorLock(null);
			return;
		}
		this.setEditorLock(uuid);
		this.setPendingEditor(null);

		// Ensure the player gets the most up-to-date information about the block
		// prior to opening the UI.
		final PacketSender sender = ServerPlayNetworking.getSender(player);
		sender.sendPacket(new ClientboundBundlePacket(List.of(
			new ClientboundBlockUpdatePacket(level, this.getBlockPos()),
			this.getUpdatePacket(),
			// Admittedly, I do not know why the sender can't do this.
			ServerPlayNetworking.createClientboundPacket(new S2COpenEditor(
				this.getLevel().dimension(),
				this.getBlockPos()
			))
		)));
	}

	default void notifyPlayerOfLockHolder(final Player player, final Component name) {
		final Player lockHolder = this.getLockHolder();

		if (lockHolder != null) {
			player.sendOverlayMessage(Component.translatableWithFallback(
				"gui.glowcase.editor.locked",
				"%2$s 🔒 %1$s", // For older Glowcase clients
				name,
				lockHolder.getDisplayName()
			));
		} else {
			player.sendOverlayMessage(Component.translatableWithFallback(
				"gui.glowcase.editor.toofar",
				"%s 📏", // For older Glowcase clients
				name
			));
		}
	}

	// Current editor
	@Nullable UUID getEditorLock();

	void setEditorLock(@Nullable UUID editorLock);

	// Pending Editor - allows the old editor to save their work.
	@Nullable UUID getPendingEditor();

	void setPendingEditor(@Nullable UUID pendingEditor);

	// Vanilla BlockEntity
	@Nullable Level getLevel();

	BlockPos getBlockPos();

	Packet<ClientGamePacketListener> getUpdatePacket();

	// Utility

	@CheckReturnValue
	@Contract("null -> false")
	private static boolean canSendOpen(Player player) {
		return player instanceof ServerPlayer serverPlayer && ServerPlayNetworking.canSend(
			serverPlayer,
			S2COpenEditor.ID
		);
	}

	@Contract("null, _, _ -> false")
	static boolean trySendOpen(Player player, Level level, BlockPos pos) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		if (!ServerPlayNetworking.canSend(serverPlayer, S2COpenEditor.ID)) {
			return false;
		}
		ServerPlayNetworking.send(serverPlayer, new S2COpenEditor(level.dimension(), pos));
		return true;
	}

	@CheckReturnValue
	@Contract("null -> false")
	private static boolean canSendClose(Player player) {
		return player instanceof ServerPlayer serverPlayer && ServerPlayNetworking.canSend(
			serverPlayer,
			S2CCloseEditor.ID
		);
	}

	@Contract("null, _, _ -> false")
	static boolean trySendClose(Player player, Level level, BlockPos pos) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		if (!ServerPlayNetworking.canSend(serverPlayer, S2CCloseEditor.ID)) {
			return false;
		}
		ServerPlayNetworking.send(serverPlayer, new S2CCloseEditor(level.dimension(), pos));
		return true;
	}
}
