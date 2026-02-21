package dev.hephaestus.glowcase.block;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.GlowcaseBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class GlowcaseBlock extends BaseEntityBlock {
	protected static final VoxelShape HALF_CUBED = Shapes.box(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);

	public GlowcaseBlock(BlockBehaviour.Properties settings) {
		super(settings);
	}

	boolean canTarget(Player player, BlockPos pos) {
		return canEditGlowcase(player, pos) && player.getMainHandItem().is(Glowcase.ITEM_TAG);
	}

	protected VoxelShape targetedOutlineShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	abstract protected boolean openEditScreen(BlockPos pos);

	protected void loadClientSideNBT(Level world, BlockPos pos, LivingEntity placer, ItemStack stack) {
		if (world.isClientSide && placer instanceof Player player && canEditGlowcase(player, pos)) {
			CustomData blockEntityTag = stack.get(DataComponents.BLOCK_ENTITY_DATA);
			if (blockEntityTag != null && world.getBlockEntity(pos) instanceof BlockEntity be) {
				blockEntityTag.loadInto(be, world.registryAccess());
			}
			openEditScreen(pos);
		}
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		loadClientSideNBT(world, pos, placer, stack);
		if (world.isClientSide && placer instanceof Player player && canEditGlowcase(player, pos)) {
			openEditScreen(pos);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof GlowcaseBlockEntity)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (player.getItemInHand(hand).is(Glowcase.ITEM_TAG) && canEditGlowcase(player, pos)) {
			if (world.isClientSide) {
				openEditScreen(pos);
			}

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		if (context != CollisionContext.empty() && context instanceof EntityCollisionContext esc && esc.getEntity() instanceof Player player && canTarget(player, pos)
		) {
			return targetedOutlineShape(state, world, pos, context);
		} else {
			return Shapes.empty();
		}
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Nullable
	@SuppressWarnings("unchecked")
	protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> checkType(BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
		return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
	}

	public static boolean canEditGlowcase(@Nullable LivingEntity entity, BlockPos pos) {
		if (entity == null) return false;

		if (entity instanceof Player player) {
			if (player.level() instanceof ServerLevel serverWorld) {
				return player.isCreative() && player.mayInteract(serverWorld, pos);
			}

			return player.isCreative();
		}

		return false;
	}

	@Deprecated
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
	}

	public static BlockBehaviour.Properties defaultSettings() {
		return Properties.of()
			.noOcclusion()
			.noLootTable()
			.noTerrainParticles()
			.strength(-1, Float.MAX_VALUE);
	}
}
