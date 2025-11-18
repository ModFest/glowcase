package dev.hephaestus.glowcase.block;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.StackInteractable;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class StackInteractableBlock extends WaterloggableGlowcaseBlock {
	public StackInteractableBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	boolean canTarget(PlayerEntity player, BlockPos pos) {
		if (!(player.getEntityWorld().getBlockEntity(pos) instanceof StackInteractable be)) return false;
		return canEditGlowcase(player, pos) && (be.matchesStack(ItemStack.EMPTY) || be.matchesStack(player.getMainHandStack()) || player.getMainHandStack().isIn(Glowcase.ITEM_TAG));
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		loadClientSideNBT(world, pos, placer, stack);
	}

	@Override
	protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof StackInteractable be)) return ActionResult.CONSUME;

		if (canEditGlowcase(player, pos)) {
			boolean holdingGlowcaseItem = stack.isIn(Glowcase.ITEM_TAG);
			boolean holdingSameAsDisplay = be.matchesStack(stack);

			if (be.matchesStack(ItemStack.EMPTY)) {
				if (!world.isClient()) be.setFromStack(stack);
				return ActionResult.SUCCESS;
			} else if (holdingSameAsDisplay) {
				if (world.isClient()) openEditScreen(pos);
				return ActionResult.SUCCESS;
			} else if (holdingGlowcaseItem) {
				if (!world.isClient()) be.unsetFromStack();
				return ActionResult.SUCCESS;
			}
		}

		return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
	}
}
