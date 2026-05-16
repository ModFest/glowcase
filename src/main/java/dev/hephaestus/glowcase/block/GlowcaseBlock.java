package dev.hephaestus.glowcase.block;

import com.mojang.logging.LogUtils;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.GlowcaseBlockEntity;
import dev.hephaestus.glowcase.block.entity.LockableEditor;
import dev.hephaestus.glowcase.packet.C2SUnlockEditor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Consumer;

public abstract class GlowcaseBlock extends BaseEntityBlock {
	private static final Logger logger = LogUtils.getLogger();

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

	public abstract boolean openEditScreen(BlockPos pos);

	// TODO: consider refactoring this to have a lock guard
	protected final InteractionResult openEditScreen(Level level, BlockPos pos, Player player) {
		if (!(level.getBlockEntity(pos) instanceof GlowcaseBlockEntity blockEntity)) {
			// Shouldn't this be considered infallible?
			// Not having an entity is an error.
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (!canEditGlowcase(player, pos)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (!(player instanceof ServerPlayer serverPlayer)) {
			// DEPRECATED: remove on up-port or C<->S API break
			if (level.isClientSide() && !ClientPlayNetworking.canSend(C2SUnlockEditor.ID)) {
				// Older Glowcase server. Open the GUI anyways.
				this.openEditScreen(pos);
				return InteractionResult.SUCCESS;
			}

			// We cannot give any definitive result from the client.
			return InteractionResult.SUCCESS_SERVER;
		}

		if (blockEntity.lockEditor(player)) {
			// Don't crash older Glowcase clients.
			LockableEditor.trySendOpen(serverPlayer, blockEntity.getLevel(), blockEntity.getBlockPos());
			return InteractionResult.SUCCESS_SERVER;
		}

		// Forcefully closes the screen on older Glowcase clients.
		// DEPRECATED: Remove on up-port OR C<->S API break.
		serverPlayer.closeContainer();

		blockEntity.notifyPlayerOfLockHolder(player, this.getName());

		// This *should* be FAIL, but for some reason,
		// the upstream handler just considers it a PASS.
		// Consume the interaction instead.
		// TODO: fix fail not acting as consume
		return InteractionResult.CONSUME;
	}

	private void openEditScreenOnPlace(GlowcaseBlockEntity blockEntity, Player player) {
		if (player instanceof ServerPlayer) {
			if (blockEntity.lockEditor(player)) {
				return;
			}

			logger.error(
				"{} ({}) @ {} is locked by {}?! Did someone call openEditorWithClientData outside of block placement?",
				this,
				blockEntity,
				blockEntity.getBlockPos(),
				blockEntity.getLockHolder()
			);

			LockableEditor.trySendClose(player, blockEntity.getLevel(), blockEntity.getBlockPos());
		} else {
			this.openEditScreen(blockEntity.getBlockPos());
		}
	}

	protected void openEditorWithClientData(Level world, BlockPos pos, LivingEntity placer, ItemStack stack) {
		if (!(placer instanceof Player player) || !canEditGlowcase(player, pos)) {
			return;
		}

		if (!(world.getBlockEntity(pos) instanceof GlowcaseBlockEntity be)) {
			return;
		}

		if (world.isClientSide()) {
			TypedEntityData<BlockEntityType<?>> blockEntityTag = stack.get(DataComponents.BLOCK_ENTITY_DATA);
			if (blockEntityTag != null) {
				blockEntityTag.loadInto(be, world.registryAccess());
			}
		}

		// TODO: reconsider whether we actually want this as UX.
		//  If we do, *everything* possible to do to the block should be done,
		//  and any block entity data polyfill should ideally cause this to be ignored.
		this.openEditScreenOnPlace(be, player);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		openEditorWithClientData(world, pos, placer, stack);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (player.getItemInHand(hand).is(Glowcase.ITEM_TAG)) {
			return this.openEditScreen(world, pos, player);
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
