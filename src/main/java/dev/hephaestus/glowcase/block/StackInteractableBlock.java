package dev.hephaestus.glowcase.block;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.StackInteractable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		loadClientSideNBT(world, pos, placer, stack);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof StackInteractable be)) return InteractionResult.CONSUME;

		if (canEditGlowcase(player, pos)) {
			boolean holdingGlowcaseItem = stack.is(Glowcase.ITEM_TAG);
			boolean holdingSameAsDisplay = be.matchesStack(stack);

			if (be.matchesStack(ItemStack.EMPTY)) {
				if (!world.isClientSide) be.setFromStack(stack);
				return InteractionResult.SUCCESS;
			} else if (holdingSameAsDisplay) {
				if (world.isClientSide) openEditScreen(pos);
				return InteractionResult.SUCCESS;
			} else if (holdingGlowcaseItem) {
				if (!world.isClientSide) be.unsetFromStack();
				return InteractionResult.SUCCESS;
			}
		}

		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}
}
