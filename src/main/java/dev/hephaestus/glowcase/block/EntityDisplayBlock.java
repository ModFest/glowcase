package dev.hephaestus.glowcase.block;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.DisplayBlockEntity;
import dev.hephaestus.glowcase.block.entity.EntityDisplayBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class EntityDisplayBlock extends StackInteractableBlock {
	public EntityDisplayBlock(BlockBehaviour.Properties settings) {
		super(settings);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EntityDisplayBlockEntity(pos, state);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
		super.setPlacedBy(world, pos, state, placer, itemStack);
		if (placer != null && world.getBlockEntity(pos) instanceof DisplayBlockEntity be) {
			be.setYaw((Math.round(((540.0F - placer.getYHeadRot())) / 45.0F) * 45) % 360);
		}
	}

	@Override
	public boolean openEditScreen(BlockPos pos) {
		Glowcase.proxy.openEntityDisplayBlockEditScreen(pos);
		return true;
	}

	@Override
	boolean canTarget(Player player, BlockPos pos) {
		if (!(player.level().getBlockEntity(pos) instanceof EntityDisplayBlockEntity be)) return false;
		if (!canEditGlowcase(player, pos)) return false;

		ItemStack held = player.getMainHandItem();
		if (held.is(Glowcase.ITEM_TAG) || be.matchesStack(held)) return true;

		return be.matchesStack(ItemStack.EMPTY)
			&& (held.getItem() instanceof SpawnEggItem || held.getItem() instanceof PlayerHeadItem);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		return checkType(type, Glowcase.ENTITY_DISPLAY_BLOCK_ENTITY.get(), EntityDisplayBlockEntity::tick);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("block.glowcase.entity_display_block.tooltip.0").withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.translatable("block.glowcase.entity_display_block.tooltip.1").withStyle(ChatFormatting.DARK_GRAY));
		textConsumer.accept(Component.translatable("block.glowcase.entity_display_block.tooltip.2").withStyle(ChatFormatting.DARK_GRAY));
	}
}
