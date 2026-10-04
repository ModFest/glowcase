package dev.hephaestus.glowcase.block;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.util.ZOffset;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public class TextBlock extends RotatableBlock {
	public TextBlock(BlockBehaviour.Properties settings) {
		super(settings);
	}

	@Override
	public boolean openEditScreen(BlockPos pos) {
		Glowcase.proxy.openTextBlockEditScreen(pos);
		return true;
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {

		if (world.getBlockEntity(pos) instanceof TextBlockEntity be) { // Wish we had ctx.side right now...
			final var offset = be.offset;
			be.offset = ZOffset.adjustPlacement(be.offset, placer.getXRot());
			if (offset != be.offset) {
				be.setChanged();
			}
		}
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TextBlockEntity(pos, state);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("block.glowcase.text_block.tooltip.0").withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.translatable("block.glowcase.generic.tooltip").withStyle(ChatFormatting.DARK_GRAY));
		textConsumer.accept(Component.translatable("block.glowcase.text_block.tooltip.1").withStyle(ChatFormatting.DARK_GRAY));
		TypedEntityData<BlockEntityType<?>> component = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (component == null) return;
		CompoundTag nbt = component.getUnsafe(); //TODO: use codecs
		if (nbt == null) return;
		for (Tag element : nbt.getList("lines").orElse(new ListTag())) {
			Optional<String> line = element.asString();
			String lineContent;
			if (line.isPresent() && !(lineContent = line.get()).isBlank()) {
				textConsumer.accept(Component.literal((lineContent.length() > 20 ? "%s...\"" : "%s").formatted(lineContent.substring(0, Math.min(lineContent.length(), 20)))).withStyle(ChatFormatting.DARK_PURPLE));
			}
		}
	}
}
