package dev.hephaestus.glowcase.block;

import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public abstract class RotatableBlock extends WaterloggableGlowcaseBlock {

	public RotatableBlock(BlockBehaviour.Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BlockStateProperties.ROTATION_16);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, Mth.floor((double) ((180.0F + ctx.getRotation()) * 16.0F / 360.0F) + 0.5D) & 15);
	}
}
