package dev.hephaestus.glowcase.block.entity;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import dev.hephaestus.glowcase.Glowcase;

import java.util.List;
import java.util.Locale;

import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ItemAcceptorBlockEntity extends GlowcaseBlockEntity {
	private String item = "minecraft:air";
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

		view.store("item", Codec.STRING, this.item);
		view.putInt("count", this.count);
		view.putInt("pulse", this.pulse);
		view.putBoolean("is_item_tag", this.isItemTag);
		view.store("output_direction", OutputDirection.CODEC, this.outputDirection);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		this.setItem(view.read("item", Codec.STRING).orElse("minecraft:air"));
		this.count = view.getIntOr("count", 1);
		this.pulse = view.getIntOr("pulse", 4);
		this.isItemTag = view.getBooleanOr("is_item_tag", false);
		this.outputDirection = view.read("output_direction", OutputDirection.CODEC).orElse(OutputDirection.BACK);
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		if (item == null) {
			return;
		}

		this.item = item;

		if (Identifier.tryParse(item) != null) {
			TagKey<Item> itemTag = TagKey.create(Registries.ITEM, Identifier.parse(item));
			itemTagList = BuiltInRegistries.ITEM.stream().filter(it -> it.getDefaultInstance().is(itemTag)).toList();
		}
	}

	public static boolean isItem(String text, RegistryAccess registryAccess) {
		return Identifier.tryParse(text) != null ||  getItemStack(text, registryAccess) != null;
	}

	public ItemStack getDisplayItemStack() {
		if (isItemTag) {
			if (itemTagList.isEmpty()) {
				return ItemStack.EMPTY;
			}

			return itemTagList.get((int) (Util.getMillis() / 1000f) % itemTagList.size()).getDefaultInstance();
		} else {
			return getItemStack(item, level.registryAccess());
		}
	}

	private static ItemStack getItemStack(String text, RegistryAccess registryAccess) {
		if (!Identifier.isValidPath(text)) {
			try {
				return new ItemParser(registryAccess).parse(new StringReader(text.toString())).createItemStack(1);
			} catch (CommandSyntaxException e) {
				return ItemStack.EMPTY;
			}
		} else if (Identifier.tryParse(text) != null) {
			return BuiltInRegistries.ITEM.getValue(Identifier.parse(text)).getDefaultInstance();
		}
		return ItemStack.EMPTY;
	}

	public boolean isItemAccepted(ItemStack stack) {
		if (!Identifier.isValidPath(item)) {
			return ItemStack.isSameItemSameComponents(stack, getDisplayItemStack()) && stack.getCount() >= count;
		} else {
			Identifier parse = Identifier.parse(item);
			boolean isEqual = isItemTag
				? stack.is(TagKey.create(Registries.ITEM, parse))
				: stack.is(BuiltInRegistries.ITEM.getValue(parse));
			return isEqual && stack.getCount() >= count;
		}
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
