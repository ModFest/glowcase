package dev.hephaestus.glowcase.block;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.GlowcaseBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class GlowcaseBlock extends BlockWithEntity {
	protected static final VoxelShape HALF_CUBED = VoxelShapes.cuboid(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);

	public GlowcaseBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	boolean canTarget(PlayerEntity player, BlockPos pos) {
		return canEditGlowcase(player, pos) && player.getMainHandStack().isIn(Glowcase.ITEM_TAG);
	}

	protected VoxelShape targetedOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.fullCube();
	}

	abstract protected boolean openEditScreen(BlockPos pos);

	protected void loadClientSideNBT(World world, BlockPos pos, LivingEntity placer, ItemStack stack) {
		if (world.isClient() && placer instanceof PlayerEntity player && canEditGlowcase(player, pos)) {
			var blockEntityTag = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
			if (blockEntityTag != null && world.getBlockEntity(pos) instanceof BlockEntity be) {
				blockEntityTag.applyToBlockEntity(be, world.getRegistryManager());
			}
			openEditScreen(pos);
		}
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		loadClientSideNBT(world, pos, placer, stack);
		if (world.isClient() && placer instanceof PlayerEntity player && canEditGlowcase(player, pos)) {
			openEditScreen(pos);
		}
	}

	@Override
	protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof GlowcaseBlockEntity)) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		if (player.getStackInHand(hand).isIn(Glowcase.ITEM_TAG) && canEditGlowcase(player, pos)) {
			if (world.isClient()) {
				openEditScreen(pos);
			}

			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		if (context != ShapeContext.absent() && context instanceof EntityShapeContext esc && esc.getEntity() instanceof PlayerEntity player && canTarget(player, pos)
		) {
			return targetedOutlineShape(state, world, pos, context);
		} else {
			return VoxelShapes.empty();
		}
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}

	@Nullable
	@SuppressWarnings("unchecked")
	protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> checkType(BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
		return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
	}

	public static boolean canEditGlowcase(@Nullable LivingEntity entity, BlockPos pos) {
		if (entity == null) return false;

		if (entity instanceof PlayerEntity player) {
			if (player.getEntityWorld() instanceof ServerWorld serverWorld) {
				return player.isCreative() && player.canModifyAt(serverWorld, pos);
			}

			return player.isCreative();
		}

		return false;
	}

	@Deprecated
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
	}

	public static AbstractBlock.Settings defaultSettings() {
		return Settings.create()
			.nonOpaque()
			.dropsNothing()
			.noBlockBreakParticles()
			.strength(-1, Float.MAX_VALUE);
	}
}
