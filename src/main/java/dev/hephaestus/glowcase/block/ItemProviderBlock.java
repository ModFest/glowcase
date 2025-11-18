package dev.hephaestus.glowcase.block;

import com.mojang.serialization.MapCodec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ItemProviderBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ItemProviderBlock extends StackInteractableBlock {
	public static final MapCodec<ItemProviderBlock> CODEC = createCodec(ItemProviderBlock::new);
	public static final EnumProperty<Direction> FACING = Properties.FACING;

	public ItemProviderBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.getDefaultState().with(FACING, Direction.UP));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(FACING);
	}

	public boolean canPickup(PlayerEntity player, BlockPos pos) {
		return ((player.getEntityWorld().getBlockEntity(pos) instanceof ItemProviderBlockEntity be && be.canGiveTo(player) && !player.isCreative() && be.canGiveTo(player) && (player.getMainHandStack().isEmpty() || (be.matchesStack(player.getMainHandStack()) && player.getMainHandStack().getCount() < player.getMainHandStack().getMaxCount()))));
	}

	@Override
	public boolean canTarget(PlayerEntity player, BlockPos pos) {
		return super.canTarget(player, pos) || canPickup(player, pos) || !player.isCreative();
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof ItemProviderBlockEntity be)) return ActionResult.CONSUME;

		if (be.canGiveTo(player)) {
			if (!world.isClient()) be.giveTo(player);
			return ActionResult.SUCCESS;
		}

		return ActionResult.CONSUME;
	}

	@Override
	protected boolean openEditScreen(BlockPos pos) {
		Glowcase.proxy.openItemProviderBlockEditScreen(pos);
		return true;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new ItemProviderBlockEntity(pos, state);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
		textConsumer.accept(Text.translatable("block.glowcase.item_provider_block.tooltip.0").formatted(Formatting.GRAY));
		textConsumer.accept(Text.translatable("block.glowcase.item_provider_block.tooltip.1").formatted(Formatting.DARK_GRAY));
		textConsumer.accept(Text.translatable("block.glowcase.item_provider_block.tooltip.2").formatted(Formatting.DARK_GRAY));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getSide());
	}

	@Override
	public VoxelShape targetedOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		Vec3i facingOffset = state.get(FACING).getVector();
		return HALF_CUBED.offset(-facingOffset.getX() / 2.0F, 0, -facingOffset.getZ() / 2.0F);
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}
}
