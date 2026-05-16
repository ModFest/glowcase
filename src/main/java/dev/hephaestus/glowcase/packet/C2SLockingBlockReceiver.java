package dev.hephaestus.glowcase.packet;

import com.mojang.logging.LogUtils;
import dev.hephaestus.glowcase.block.GlowcaseBlock;
import dev.hephaestus.glowcase.block.entity.GlowcaseBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * @author Ampflower
 **/
@NotNullByDefault
public interface C2SLockingBlockReceiver extends CustomPacketPayload {
	@ApiStatus.Internal
	Logger logger = LogUtils.getLogger();

	void receive(
		final ServerPlayer player,
		final ServerLevel level,
		final GlowcaseBlockEntity blockEntity
	);

	@ApiStatus.NonExtendable
	default void receive(ServerPlayNetworking.Context context) {
		final ServerPlayer player = context.player();
		final ServerLevel level = player.level();

		// The following requires the data to be loaded.
		// Check if we have the data loaded first, to avoid a DOS.
		if (!level.areEntitiesLoaded(ChunkPos.pack(pos()))) {
			return;
		}

		final BlockEntity blockEntity = level.getBlockEntity(this.pos());

		if (!canEdit(player, blockEntity)) {
			reject(context.responseSender(), level, blockEntity);
			return;
		}

		final GlowcaseBlockEntity glowcase = (GlowcaseBlockEntity) blockEntity;

		if (!glowcase.unlockEditor(player)) {
			logger.error("{} cannot unlock {} at {} despite passing the canEdit test???", level, blockEntity, pos());
			reject(context.responseSender(), level, blockEntity);
			return;
		}

		receive(player, level, glowcase);
	}

	default void reject(
		final PacketSender responseSender,
		final ServerLevel level,
		final @Nullable BlockEntity entity
	) {
	}

	@Contract("_, null -> false")
	default boolean canEdit(
		final ServerPlayer player,
		final @Nullable BlockEntity entity
	) {
		if (!(entity instanceof GlowcaseBlockEntity glowcase)) {
			return false;
		}

		if (!(player.level().getBlockState(pos()).getBlock() instanceof GlowcaseBlock)) {
			return false;
		}

		// Distantless validity check.
		if (glowcase.isLockHolder(player)) {
			return true;
		}

		// Fall-back check.
		return lockEditor(glowcase, player);
	}

	// Allows unlock to not implicitly lock.
	default boolean lockEditor(final GlowcaseBlockEntity glowcase, final ServerPlayer player) {
		return glowcase.lockEditor(player);
	}

	// Required context
	BlockPos pos();
}
