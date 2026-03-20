package dev.hephaestus.glowcase.block;

import com.mojang.serialization.MapCodec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ConfigLinkBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ConfigLinkBlock extends WaterloggableGlowcaseBlock {
	public static final MapCodec<ConfigLinkBlock> CODEC = simpleCodec(ConfigLinkBlock::new);

	public ConfigLinkBlock(BlockBehaviour.Properties settings) {
		super(settings);
	}

	@Override
	protected VoxelShape targetedOutlineShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return HALF_CUBED;
	}

	@Override
	protected boolean openEditScreen(BlockPos pos) {
		Glowcase.proxy.openConfigLinkBlockEditScreen(pos);
		return true;
	}

	@Override
	boolean canTarget(Player player, BlockPos pos) {
		return true;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ConfigLinkBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof ConfigLinkBlockEntity be)) return InteractionResult.CONSUME;
		if (world.isClientSide()) {
			Glowcase.proxy.openConfigScreen(be.getUrl());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("block.glowcase.config_link_block.tooltip.0").withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.translatable("block.glowcase.generic.tooltip").withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
