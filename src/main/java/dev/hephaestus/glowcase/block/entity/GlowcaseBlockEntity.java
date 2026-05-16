package dev.hephaestus.glowcase.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GlowcaseBlockEntity extends BlockEntity implements LockableEditor {
	@Nullable
	private transient UUID editorLock, pendingEditor;
	private transient long pendingSince;

	public GlowcaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public @Nullable UUID getEditorLock() {
		return this.editorLock;
	}

	@Override
	public void setEditorLock(final @Nullable UUID editorLock) {
		this.editorLock = editorLock;
	}

	@Override
	public @Nullable UUID getPendingEditor() {
		final Level level = this.getLevel();
		if (level == null) {
			// We cannot do anything when there's no level.
			return null;
		}
		if (this.pendingSince + 100 < level.getGameTime()) {
			// 5-second timeout, don't surprise-swap on the original requesting.
			return null;
		}
		return this.pendingEditor;
	}

	@Override
	public void setPendingEditor(final @Nullable UUID pendingEditor) {
		final Level level = this.getLevel();
		if (level == null) {
			// We cannot do anything when there's no level.
			this.pendingEditor = null;
			return;
		}
		this.pendingEditor = pendingEditor;
		this.pendingSince = level.getGameTime();
	}

	@Override
	public void setRemoved() {
		this.intrudeUnlock();
		super.setRemoved();
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (level instanceof ServerLevel sw) sw.getChunkSource().blockChanged(worldPosition);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
		return saveWithoutMetadata(registryLookup);
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
