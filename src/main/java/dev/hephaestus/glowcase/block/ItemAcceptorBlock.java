package dev.hephaestus.glowcase.block;

import com.mojang.serialization.MapCodec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ItemAcceptorBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.block.OrientationHelper;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ItemAcceptorBlock extends GlowcaseBlock {
	public static final MapCodec<ItemAcceptorBlock> CODEC = createCodec(ItemAcceptorBlock::new);
	private static final VoxelShape OUTLINE = VoxelShapes.fullCube();
	public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
	public static final BooleanProperty POWERED = Properties.POWERED;

	public ItemAcceptorBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.getDefaultState().with(FACING, Direction.NORTH).with(POWERED, false));
	}

	@Override
	protected BlockRenderType getRenderType(final BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(FACING, POWERED);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected boolean openEditScreen(BlockPos pos) {
		Glowcase.proxy.openItemAcceptorBlockEditScreen(pos);
		return true;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof ItemAcceptorBlockEntity be)) return ActionResult.CONSUME;
		if (canEditGlowcase(player, pos)) {
			if (world.isClient()) {
				openEditScreen(pos);
			}
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	@Override
	protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof ItemAcceptorBlockEntity be)) {
			return ActionResult.CONSUME;
		}

		if (!be.isItemAccepted(stack)) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		if (world.getBlockTickScheduler().isQueued(pos, this) || state.get(POWERED)) {
			return ActionResult.PASS;
		}

		if (!world.isClient()) {
			// Remove items
			ItemStack newStack = stack.copyWithCount(be.count);
			stack.decrementUnlessCreative(be.count, player);

			// Attempt to insert items
			if (getInventoryAt(world, pos.offset(getOutputDirection(world, pos, state))) instanceof Inventory inventory) {
				addToFirstFreeSlot(inventory, newStack);
			}

			// Schedule redstone pulse
			if(be.getPulse() > 0) world.scheduleBlockTick(pos, this, 2);
		}
		return ActionResult.SUCCESS;
	}

	private static Inventory getInventoryAt(World world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();

		if (block instanceof InventoryProvider inventoryProvider) {
			return inventoryProvider.getInventory(state, world, pos);
		} else if (state.hasBlockEntity() && world.getBlockEntity(pos) instanceof Inventory inventory) {
			if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock chestBlock) {
				return ChestBlock.getInventory(chestBlock, state, world, pos, true);
			}

			return inventory;
		}

		return null;
	}

	private ItemStack addToFirstFreeSlot(Inventory inventory, ItemStack stack) {
		int i = inventory.getMaxCount(stack);

		for (int j = 0; j < inventory.size(); j++) {
			ItemStack itemStack = inventory.getStack(j);
			if (itemStack.isEmpty() || ItemStack.areItemsAndComponentsEqual(stack, itemStack)) {
				int k = Math.min(stack.getCount(), i - itemStack.getCount());
				if (k > 0) {
					if (itemStack.isEmpty()) {
						inventory.setStack(j, stack.split(k));
					} else {
						stack.decrement(k);
						itemStack.increment(k);
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
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.get(POWERED)) {
			world.setBlockState(pos, state.with(POWERED, false), Block.NOTIFY_LISTENERS);
		} else {
			world.setBlockState(pos, state.with(POWERED, true), Block.NOTIFY_LISTENERS);
			world.scheduleBlockTick(pos, this, world.getBlockEntity(pos) instanceof ItemAcceptorBlockEntity be ? be.getPulse() : 4);
		}

		this.updateNeighbors(world, pos, state);
	}

	protected void updateNeighbors(World world, BlockPos pos, BlockState state) {
		Direction direction = getOutputDirection(world, pos, state);
		BlockPos blockPos = pos.offset(direction);
		WireOrientation emissionOrientation = OrientationHelper.getEmissionOrientation(world, direction, null);
		world.updateNeighbor(blockPos, this, emissionOrientation);
		world.updateNeighborsExcept(blockPos, this, direction.getOpposite(), emissionOrientation);
	}

	@Override
	protected boolean emitsRedstonePower(BlockState state) {
		return true;
	}

	@Override
	protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return state.get(POWERED) && getOutputDirection(world, pos, state) == direction.getOpposite() ? 15 : 0;
	}

	public Direction getOutputDirection(BlockView world, BlockPos pos, BlockState state) {
		if (world.getBlockEntity(pos) instanceof ItemAcceptorBlockEntity blockEntity) {
			return switch (blockEntity.outputDirection) {
				case BOTTOM -> Direction.DOWN;
				case BACK -> state.get(FACING).getOpposite();
				case TOP -> Direction.UP;
			};
		}

		return state.get(FACING).getOpposite();
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new ItemAcceptorBlockEntity(pos, state);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return OUTLINE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return OUTLINE;
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
		textConsumer.accept(Text.translatable("block.glowcase.item_acceptor_block.tooltip.0").formatted(Formatting.GRAY));
		textConsumer.accept(Text.translatable("block.glowcase.item_acceptor_block.tooltip.1").formatted(Formatting.BLUE));
		textConsumer.accept(Text.translatable("block.glowcase.item_acceptor_block.tooltip.2").formatted(Formatting.BLUE));
		textConsumer.accept(Text.translatable("block.glowcase.item_acceptor_block.tooltip.3").formatted(Formatting.DARK_GRAY));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}
}
