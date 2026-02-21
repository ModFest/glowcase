package dev.hephaestus.glowcase.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ampflower
 **/
public abstract class WaterloggableGlowcaseBlock extends GlowcaseBlock implements SimpleWaterloggedBlock {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	public WaterloggableGlowcaseBlock(BlockBehaviour.Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return state.getFluidState().isEmpty();
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		}

		return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected FluidState getFluidState(final BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(final BlockPlaceContext ctx) {
		return defaultBlockState().setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER);
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(WATERLOGGED);
	}

	@Override
	public ItemStack pickupBlock(@Nullable LivingEntity drainer, LevelAccessor world, BlockPos pos, BlockState state) {
		if (!GlowcaseBlock.canEditGlowcase(drainer, pos)) {
			return ItemStack.EMPTY;
		}
		return SimpleWaterloggedBlock.super.pickupBlock(drainer, world, pos, state);
	}

	@Override
	public boolean canPlaceLiquid(@Nullable LivingEntity filler, BlockGetter world, BlockPos pos, BlockState state, Fluid fluid) {
		return GlowcaseBlock.canEditGlowcase(filler, pos) && SimpleWaterloggedBlock.super.canPlaceLiquid(filler, world, pos, state, fluid);
	}
}
