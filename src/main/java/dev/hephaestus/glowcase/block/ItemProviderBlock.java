package dev.hephaestus.glowcase.block;

import com.mojang.serialization.MapCodec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ItemProviderBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ItemProviderBlock extends StackInteractableBlock {
	public static final MapCodec<ItemProviderBlock> CODEC = simpleCodec(ItemProviderBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

	public ItemProviderBlock(BlockBehaviour.Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.UP));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	public boolean canPickup(Player player, BlockPos pos) {
		return ((player.level().getBlockEntity(pos) instanceof ItemProviderBlockEntity be && be.canGiveTo(player) && !player.isCreative() && be.canGiveTo(player) && (player.getMainHandItem().isEmpty() || (be.matchesStack(player.getMainHandItem()) && player.getMainHandItem().getCount() < player.getMainHandItem().getMaxStackSize()))));
	}

	@Override
	public boolean canTarget(Player player, BlockPos pos) {
		return super.canTarget(player, pos) || canPickup(player, pos) || !player.isCreative();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof ItemProviderBlockEntity be)) return InteractionResult.CONSUME;

		if (be.canGiveTo(player)) {
			if (!world.isClientSide()) be.giveTo(player);
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.CONSUME;
	}

	@Override
	public boolean openEditScreen(BlockPos pos) {
		Glowcase.proxy.openItemProviderBlockEditScreen(pos);
		return true;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ItemProviderBlockEntity(pos, state);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("block.glowcase.item_provider_block.tooltip.0").withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.translatable("block.glowcase.item_provider_block.tooltip.1").withStyle(ChatFormatting.DARK_GRAY));
		textConsumer.accept(Component.translatable("block.glowcase.item_provider_block.tooltip.2").withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getClickedFace());
	}

	@Override
	public VoxelShape targetedOutlineShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		Vec3i facingOffset = state.getValue(FACING).getUnitVec3i();
		return HALF_CUBED.move(-facingOffset.getX() / 2.0F, 0, -facingOffset.getZ() / 2.0F);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
