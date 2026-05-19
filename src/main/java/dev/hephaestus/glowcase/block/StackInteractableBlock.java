package dev.hephaestus.glowcase.block;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.GlowcaseBlockEntity;
import dev.hephaestus.glowcase.block.entity.StackInteractable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class StackInteractableBlock extends WaterloggableGlowcaseBlock {
	public StackInteractableBlock(BlockBehaviour.Properties settings) {
		super(settings);
	}

	@Override
	boolean canTarget(Player player, BlockPos pos) {
		if (!(player.level().getBlockEntity(pos) instanceof StackInteractable be)) return false;
		return canEditGlowcase(player, pos) && (be.matchesStack(ItemStack.EMPTY) || be.matchesStack(player.getMainHandItem()) || player.getMainHandItem().is(Glowcase.ITEM_TAG));
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof GlowcaseBlockEntity glowcase)) {
			// Consider this infallible?
			return InteractionResult.CONSUME;
		}
		if (!(glowcase instanceof StackInteractable interactable)) {
			return InteractionResult.CONSUME;
		}

		// If it matches the stack, delegate to openEditScreen.
		if (interactable.matchesStack(stack)) {
			// TODO: increment if non-creative instead?
			//  tho this might already do that-
			return this.openEditScreen(world, pos, player);
		}

		if (!canEditGlowcase(player, pos)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		// Check if the player may obtain a lock first before calling the other mutators.
		// TODO: broadcast lock status to clients?
		//  The hardest question in computer science: cache invalidation
		if (world.isClientSide()) {
			return InteractionResult.CONSUME;
		}
		if (!glowcase.mayObtainLock(player)) {
			glowcase.notifyPlayerOfLockHolder(player, this.getName());
			if (player instanceof ServerPlayer serverPlayer) {
				final var packet = glowcase.getUpdatePacket();
				if (packet != null) {
					serverPlayer.connection.send(packet);
				}
			}
			return InteractionResult.CONSUME;
		}

		// Clear if glowcase, opening GUI instead if empty.
		if (stack.is(Glowcase.ITEM_TAG)) {
			if (interactable.matchesStack(ItemStack.EMPTY)) {
				this.openEditScreen(world, pos, player);
			} else {
				interactable.unsetFromStack();
			}
			return InteractionResult.SUCCESS_SERVER;
		}

		// Set if empty.
		if (interactable.matchesStack(ItemStack.EMPTY)) {
			interactable.setFromStack(stack);
			return InteractionResult.SUCCESS_SERVER;
		}

		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}
}
