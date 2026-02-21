package dev.hephaestus.glowcase.block.entity;

import com.mojang.serialization.Codec;
import dev.hephaestus.glowcase.Glowcase;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ItemAcceptorBlockEntity extends GlowcaseBlockEntity {
	private ResourceLocation item = ResourceLocation.withDefaultNamespace("air");
	public int count = 1;
	public int pulse = 4;
	public OutputDirection outputDirection = OutputDirection.BACK;
	public boolean isItemTag = false;
	private List<Item> itemTagList = List.of();

	public ItemAcceptorBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.ITEM_ACCEPTOR_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);

		view.store("item", ResourceLocation.CODEC, this.item);
		view.putInt("count", this.count);
		view.putInt("pulse", this.pulse);
		view.putBoolean("is_item_tag", this.isItemTag);
		view.store("output_direction", OutputDirection.CODEC, this.outputDirection);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		this.setItem(view.read("item", ResourceLocation.CODEC).orElse(ResourceLocation.withDefaultNamespace("air")));
		this.count = view.getIntOr("count", 1);
		this.pulse = view.getIntOr("pulse", 4);
		this.isItemTag = view.getBooleanOr("is_item_tag", false);
		this.outputDirection = view.read("output_direction", OutputDirection.CODEC).orElse(OutputDirection.BACK);
	}

	public ResourceLocation getItem() {
		return item;
	}

	public void setItem(ResourceLocation item) {
		if (item == null) {
			return;
		}

		this.item = item;

		TagKey<Item> itemTag = TagKey.create(Registries.ITEM, item);
		itemTagList = BuiltInRegistries.ITEM.stream().filter(it -> it.getDefaultInstance().is(itemTag)).toList();
	}

	public ItemStack getDisplayItemStack() {
		if (isItemTag) {
			if (itemTagList.isEmpty()) {
				return ItemStack.EMPTY;
			}

			return itemTagList.get((int) (Util.getMillis() / 1000f) % itemTagList.size()).getDefaultInstance();
		} else {
			return BuiltInRegistries.ITEM.getValue(item).getDefaultInstance();
		}
	}

	public boolean isItemAccepted(ItemStack stack) {
		boolean isEqual = isItemTag
			? stack.is(TagKey.create(Registries.ITEM, item))
			: stack.is(BuiltInRegistries.ITEM.getValue(item));

		return isEqual && stack.getCount() >= count;
	}

	public int getPulse() {
		return pulse;
	}

	public enum OutputDirection implements StringRepresentable {
		TOP, BACK, BOTTOM;

		public static final Codec<OutputDirection> CODEC = StringRepresentable.fromEnum(OutputDirection::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
