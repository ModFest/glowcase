package dev.hephaestus.glowcase.block;

import com.mojang.serialization.MapCodec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ItemAcceptorBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainerHolder;
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
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ItemAcceptorBlock extends GlowcaseBlock {
	public static final MapCodec<ItemAcceptorBlock> CODEC = simpleCodec(ItemAcceptorBlock::new);
	private static final VoxelShape OUTLINE = Shapes.block();
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public ItemAcceptorBlock(BlockBehaviour.Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
	}

	@Override
	protected RenderShape getRenderShape(final BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, POWERED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	protected boolean openEditScreen(BlockPos pos) {
		Glowcase.proxy.openItemAcceptorBlockEditScreen(pos);
		return true;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof ItemAcceptorBlockEntity be)) return InteractionResult.CONSUME;
		if (canEditGlowcase(player, pos)) {
			if (world.isClientSide()) {
				openEditScreen(pos);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof ItemAcceptorBlockEntity be)) {
			return InteractionResult.CONSUME;
		}

		if (!be.isItemAccepted(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (world.getBlockTicks().hasScheduledTick(pos, this) || state.getValue(POWERED)) {
			return InteractionResult.PASS;
		}

		if (!world.isClientSide()) {
			// Remove items
			ItemStack newStack = stack.copyWithCount(be.count);
			stack.consume(be.count, player);

			// Attempt to insert items
			if (getInventoryAt(world, pos.relative(getOutputDirection(world, pos, state))) instanceof Container inventory) {
				addToFirstFreeSlot(inventory, newStack);
			}

			// Schedule redstone pulse
			if(be.getPulse() > 0) world.scheduleTick(pos, this, 2);
		}
		return InteractionResult.SUCCESS;
	}

	private static Container getInventoryAt(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();

		if (block instanceof WorldlyContainerHolder inventoryProvider) {
			return inventoryProvider.getContainer(state, world, pos);
		} else if (state.hasBlockEntity() && world.getBlockEntity(pos) instanceof Container inventory) {
			if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock chestBlock) {
				return ChestBlock.getContainer(chestBlock, state, world, pos, true);
			}

			return inventory;
		}

		return null;
	}

	private ItemStack addToFirstFreeSlot(Container inventory, ItemStack stack) {
		int i = inventory.getMaxStackSize(stack);

		for (int j = 0; j < inventory.getContainerSize(); j++) {
			ItemStack itemStack = inventory.getItem(j);
			if (itemStack.isEmpty() || ItemStack.isSameItemSameComponents(stack, itemStack)) {
				int k = Math.min(stack.getCount(), i - itemStack.getCount());
				if (k > 0) {
					if (itemStack.isEmpty()) {
						inventory.setItem(j, stack.split(k));
					} else {
						stack.shrink(k);
						itemStack.grow(k);
					}
				}

				if (stack.isEmpty()) {
					break;
				}
			}
		}

		return stack;
	}

	@Override
	protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (state.getValue(POWERED)) {
			world.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_CLIENTS);
		} else {
			world.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_CLIENTS);
			world.scheduleTick(pos, this, world.getBlockEntity(pos) instanceof ItemAcceptorBlockEntity be ? be.getPulse() : 4);
		}

		this.updateNeighbors(world, pos, state);
	}

	protected void updateNeighbors(Level world, BlockPos pos, BlockState state) {
		Direction direction = getOutputDirection(world, pos, state);
		BlockPos blockPos = pos.relative(direction);
		Orientation emissionOrientation = ExperimentalRedstoneUtils.initialOrientation(world, direction, null);
		world.neighborChanged(blockPos, this, emissionOrientation);
		world.updateNeighborsAtExceptFromFacing(blockPos, this, direction.getOpposite(), emissionOrientation);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
		return state.getValue(POWERED) && getOutputDirection(world, pos, state) == direction.getOpposite() ? 15 : 0;
	}

	public Direction getOutputDirection(BlockGetter world, BlockPos pos, BlockState state) {
		if (world.getBlockEntity(pos) instanceof ItemAcceptorBlockEntity blockEntity) {
			return switch (blockEntity.outputDirection) {
				case BOTTOM -> Direction.DOWN;
				case BACK -> state.getValue(FACING).getOpposite();
				case TOP -> Direction.UP;
			};
		}

		return state.getValue(FACING).getOpposite();
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ItemAcceptorBlockEntity(pos, state);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return OUTLINE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return OUTLINE;
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("block.glowcase.item_acceptor_block.tooltip.0").withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.translatable("block.glowcase.item_acceptor_block.tooltip.1").withStyle(ChatFormatting.BLUE));
		textConsumer.accept(Component.translatable("block.glowcase.item_acceptor_block.tooltip.2").withStyle(ChatFormatting.BLUE));
		textConsumer.accept(Component.translatable("block.glowcase.item_acceptor_block.tooltip.3").withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
