package dev.hephaestus.glowcase.block.entity;

import com.mojang.serialization.Codec;
import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ItemProviderBlockEntity extends GlowcaseBlockEntity implements InfiniteInventory, StackInteractable {
	protected ItemStack stack = ItemStack.EMPTY;
	protected GivesItem givesItem = GivesItem.ALWAYS;
	protected boolean invisible = false;
	public long cooldown = 0;
	protected Map<UUID, Long> givenTimes = new HashMap<>();

	public ItemProviderBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.ITEM_PROVIDER_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	public boolean matchesStack(ItemStack stack) {
		return ItemStack.isSameItem(this.stack, stack);
	}

	@Override
	public void setFromStack(ItemStack stack) {
		this.stack = stack.copy();
		this.givenTimes.clear();
		this.setChanged();
	}

	@Override
	public void unsetFromStack() {
		this.stack = ItemStack.EMPTY;
		this.setChanged();
	}

	@Override
	public ItemStack getStack() {
		return stack;
	}

	public GivesItem getGivesItem() {
		return givesItem;
	}

	public boolean isInvisible() {
		return this.invisible;
	}

	public void setGivesItem(GivesItem givesItem) {
		this.givesItem = givesItem;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);

		if (!this.stack.isEmpty()) view.store("item", ItemStack.CODEC, this.stack);
		view.store("gives_item", GivesItem.CODEC, this.givesItem);
		view.putLong("cooldown", this.cooldown);
		view.store("given_times", Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, Codec.LONG), givenTimes);

		view.putBoolean("invisible", this.invisible);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		this.stack = view.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		this.givesItem = view.read("gives_item", GivesItem.CODEC).orElse(GivesItem.ALWAYS);
		this.cooldown = view.getLongOr("cooldown", 0);
		this.givenTimes = new HashMap<>(view.read("given_times", Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, Codec.LONG)).orElseGet(() -> Map.of()));
		this.invisible = view.getBooleanOr("invisible", false);
	}

	public void cycleGiveType() {
		this.givesItem = GivesItem.values()[(this.givesItem.ordinal() + 1) % GivesItem.values().length];
		givenTimes.clear();
		setChanged();
	}

	public long getCooldownTicks(Player player) {
		return givenTimes.containsKey(player.getUUID()) ? givenTimes.get(player.getUUID()) + this.cooldown * 20 - level.getGameTime() : 0;
	}

	public boolean canGiveTo(Player player) {
		if (!hasItem()) return false;
		else return switch (this.givesItem) {
			case ALWAYS -> true;
			case TIMED -> player.isCreative() || getCooldownTicks(player) <= 0;
			case ONE -> player.isCreative() || !player.getInventory().hasAnyOf(Set.of(stack.getItem()));
		};
	}

	public void giveTo(Player player) {
		ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
		boolean holdingSameAsDisplay = ItemStack.isSameItemSameComponents(getStack(), itemStack);

		if (itemStack.isEmpty()) {
			ItemStack stackToGive = getStack().copy();
			if (this.givesItem == GivesItem.ALWAYS && player.isShiftKeyDown()) {
				stackToGive.setCount(stackToGive.getMaxStackSize());
			}

			player.setItemInHand(InteractionHand.MAIN_HAND, stackToGive);
		} else if (holdingSameAsDisplay) {
			itemStack.grow(getStack().getCount());
			itemStack.limitSize(itemStack.getMaxStackSize());
			player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);
		} else {
			return;
		}

		if (!player.isCreative()) {
			givenTimes.put(player.getUUID(), level.getGameTime());
			setChanged();
		}
	}

	public enum GivesItem implements StringRepresentable {
		ALWAYS, TIMED, ONE;

		public static final Codec<GivesItem> CODEC = StringRepresentable.fromEnum(GivesItem::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
