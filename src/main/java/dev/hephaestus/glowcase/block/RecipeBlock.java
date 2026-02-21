package dev.hephaestus.glowcase.block;

import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.RecipeBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RecipeBlock extends RotatableBlock {
	public static final MapCodec<RecipeBlock> CODEC = simpleCodec(RecipeBlock::new);

	public RecipeBlock(BlockBehaviour.Properties settings) {
		super(settings);
	}

	@Override
	protected boolean openEditScreen(BlockPos pos) {
		Glowcase.proxy.openRecipeBlockEditScreen(pos);
		return true;
	}

	@Override
	boolean canTarget(Player player, BlockPos pos) {
		return true;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RecipeBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof RecipeBlockEntity be)) return InteractionResult.CONSUME;

		if (world.isClientSide()) {
			be.openRecipe();
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.CONSUME;
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("block.glowcase.recipe_block.tooltip.0").withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.translatable("block.glowcase.generic.tooltip").withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public VoxelShape targetedOutlineShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		if (!(world.getBlockEntity(pos) instanceof RecipeBlockEntity be)) return Shapes.empty();
		float rotation = -(state.getValue(BlockStateProperties.ROTATION_16) * 360) / 16.0F;
		Vector3f offset = new Vector3f(0, 0, be.zOffset == TextBlockEntity.ZOffset.CENTER ? 0.01F : be.zOffset == TextBlockEntity.ZOffset.FRONT ? 0.4F : -0.4F).rotate(Axis.YP.rotationDegrees(rotation));
		return HALF_CUBED.move(offset.x, offset.y, offset.z);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
