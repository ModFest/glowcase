package dev.hephaestus.glowcase.block;

import com.mojang.serialization.MapCodec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class PopupBlock extends WaterloggableGlowcaseBlock {
	public static final MapCodec<PopupBlock> CODEC = createCodec(PopupBlock::new);

	public PopupBlock(AbstractBlock.Settings settings) {
		super(settings);
	}
	@Override
	public VoxelShape targetedOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return HALF_CUBED;
	}

	@Override
	protected boolean openEditScreen(BlockPos pos) {
		Glowcase.proxy.openPopupBlockEditScreen(pos);
		return true;
	}

	@Override
	boolean canTarget(PlayerEntity player, BlockPos pos) {
		return true;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new PopupBlockEntity(pos, state);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof PopupBlockEntity be)) return ActionResult.CONSUME;
		if (world.isClient && !(be.lines.size() == 1 && be.lines.getFirst().getContent().equals(PlainTextContent.EMPTY))) {
			Glowcase.proxy.openPopupBlockViewScreen(pos);
		}
		return ActionResult.SUCCESS;
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
		textConsumer.accept(Text.translatable("block.glowcase.popup_block.tooltip.0").formatted(Formatting.GRAY));
		textConsumer.accept(Text.translatable("block.glowcase.generic.tooltip").formatted(Formatting.DARK_GRAY));
		textConsumer.accept(Text.translatable("block.glowcase.popup_block.tooltip.1").formatted(Formatting.DARK_GRAY));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}
}
