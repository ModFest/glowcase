package dev.hephaestus.glowcase.block;

import com.mojang.serialization.MapCodec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class SpriteBlock extends WaterloggableGlowcaseBlock {
	public static final MapCodec<SpriteBlock> CODEC = simpleCodec(SpriteBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

	public SpriteBlock(BlockBehaviour.Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.UP));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	protected boolean openEditScreen(BlockPos pos) {
		Glowcase.proxy.openSpriteBlockEditScreen(pos);
		return true;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getClickedFace());
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		loadClientSideNBT(world, pos, placer, stack);
		if (placer != null && world.getBlockEntity(pos) instanceof SpriteBlockEntity be) {
			if (state.getValue(FACING).equals(Direction.UP)) {
				be.setRotation(Math.round(((540.0F + placer.getYHeadRot()) % 360.0F) / 45.0F) * 45);
			}
			if (state.getValue(FACING).equals(Direction.DOWN)) {
				be.setRotation(Math.round(((540.0F - placer.getYHeadRot()) % 360.0F) / 45.0F) * 45);
			}
		}
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SpriteBlockEntity(pos, state);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("block.glowcase.sprite_block.tooltip.0").withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.translatable("block.glowcase.generic.tooltip").withStyle(ChatFormatting.DARK_GRAY));
		textConsumer.accept(Component.translatable("block.glowcase.sprite_block.tooltip.1").withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
