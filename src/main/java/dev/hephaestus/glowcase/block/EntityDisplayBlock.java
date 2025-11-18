package dev.hephaestus.glowcase.block;

import com.mojang.serialization.MapCodec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.DisplayBlockEntity;
import dev.hephaestus.glowcase.block.entity.EntityDisplayBlockEntity;
import dev.hephaestus.glowcase.block.entity.StackInteractable;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class EntityDisplayBlock extends StackInteractableBlock {
	public static final MapCodec<EntityDisplayBlock> CODEC = createCodec(EntityDisplayBlock::new);

	public EntityDisplayBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new EntityDisplayBlockEntity(pos, state);
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);
		if (placer != null && world.getBlockEntity(pos) instanceof DisplayBlockEntity be) {
			be.setYaw((Math.round(((540.0F - placer.getHeadYaw())) / 45.0F) * 45) % 360);
		}
	}

	@Override
	protected boolean openEditScreen(BlockPos pos) {
		Glowcase.proxy.openEntityDisplayBlockEditScreen(pos);
		return true;
	}

	@Override
	boolean canTarget(PlayerEntity player, BlockPos pos) {
		if (!(player.getEntityWorld().getBlockEntity(pos) instanceof StackInteractable be)) return false;
		return canEditGlowcase(player, pos) && ((be.matchesStack(ItemStack.EMPTY) && player.getMainHandStack().getItem() instanceof SpawnEggItem) || be.matchesStack(player.getMainHandStack()) || player.getMainHandStack().isIn(Glowcase.ITEM_TAG));
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return checkType(type, Glowcase.ENTITY_DISPLAY_BLOCK_ENTITY.get(), EntityDisplayBlockEntity::tick);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
		textConsumer.accept(Text.translatable("block.glowcase.entity_display_block.tooltip.0").formatted(Formatting.GRAY));
		textConsumer.accept(Text.translatable("block.glowcase.entity_display_block.tooltip.1").formatted(Formatting.DARK_GRAY));
		textConsumer.accept(Text.translatable("block.glowcase.entity_display_block.tooltip.2").formatted(Formatting.DARK_GRAY));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}
}
