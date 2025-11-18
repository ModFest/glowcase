package dev.hephaestus.glowcase.block;

import com.mojang.serialization.MapCodec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class TextBlock extends RotatableBlock {
	public static final MapCodec<TextBlock> CODEC = createCodec(TextBlock::new);

	public TextBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected boolean openEditScreen(BlockPos pos) {
		Glowcase.proxy.openTextBlockEditScreen(pos);
		return true;
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {

		if (world.getBlockEntity(pos) instanceof TextBlockEntity be) { // Wish we had ctx.side right now...
			if (be.zOffset == TextBlockEntity.ZOffset.CENTER && Math.abs(placer.getPitch()) < 30) {
				be.zOffset = TextBlockEntity.ZOffset.BACK;
			} else if (be.zOffset == TextBlockEntity.ZOffset.BACK && Math.abs(placer.getPitch()) > 60) {
				be.zOffset = TextBlockEntity.ZOffset.CENTER;
			}
			be.markDirty();
		}
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new TextBlockEntity(pos, state);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
		textConsumer.accept(Text.translatable("block.glowcase.text_block.tooltip.0").formatted(Formatting.GRAY));
		textConsumer.accept(Text.translatable("block.glowcase.generic.tooltip").formatted(Formatting.DARK_GRAY));
		textConsumer.accept(Text.translatable("block.glowcase.text_block.tooltip.1").formatted(Formatting.DARK_GRAY));
		var component = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
		if (component == null) return;
		NbtCompound nbt = component.copyNbtWithoutId(); //TODO: use codecs
		if (nbt == null) return;
		for (NbtElement element : nbt.getList("lines").orElse(new NbtList())) {
			Optional<String> line = element.asString();
			String lineContent;
			if (line.isPresent() && !(lineContent = line.get()).isBlank()) {
				textConsumer.accept(Text.literal((lineContent.length() > 20 ? "%s...\"" : "%s").formatted(lineContent.substring(0, Math.min(lineContent.length(), 20)))).formatted(Formatting.DARK_PURPLE));
			}
		}
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}
}
