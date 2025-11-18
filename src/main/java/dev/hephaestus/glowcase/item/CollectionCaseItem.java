package dev.hephaestus.glowcase.item;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.item.component.CollectionComponent;
import dev.hephaestus.glowcase.util.CollectableStack;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.Consumer;

public class CollectionCaseItem extends Item implements ScrollableItem {
	public CollectionCaseItem(Settings settings) {
		super(settings);
	}

	public boolean bundleInteract(ItemStack caseStack, ItemStack otherStack, ClickType clickType, PlayerEntity player, Consumer<ItemStack> otherStackSetter, boolean tooltipVisible) {
		CollectionComponent collection = caseStack.get(Glowcase.COLLECTION_COMPONENT.get());
		if (collection != null && clickType.equals(ClickType.RIGHT)) {
			if (otherStack.isEmpty()) { // Removal Actions
				ItemStack retrievedStack = collection.getSelectedCollectableStack();
				if (!retrievedStack.isEmpty() && collection.isSelectedCollected()) { // Retrieve Collectable
					otherStackSetter.accept(retrievedStack);
					caseStack.set(Glowcase.COLLECTION_COMPONENT.get(), collection.retrieveSelectedStack(player.isCreative()));
					playRetrieveSound(player);
					return true;
				} else if (player.isCreative() && tooltipVisible && collection.hasSelection()) { // Remove Collectable
					caseStack.set(Glowcase.COLLECTION_COMPONENT.get(), collection.withoutSelectedStack());
					playRemoveSound(player);
					return true;
				}
			} else { // Insertion Actions
				int collectionIndex = collection.getCollectionIndex(otherStack);
				if (collectionIndex != -1) { // Collect Collectable
					otherStack.decrement(collection.collectables().get(collectionIndex).getStack().getCount());
					caseStack.set(Glowcase.COLLECTION_COMPONENT.get(), collection.collectStack(collectionIndex));
					playCollectSound(player);
					return true;
				} else if (player.isCreative()) { // Add Collectable
					caseStack.set(Glowcase.COLLECTION_COMPONENT.get(), collection.withStackAfterSelection(otherStack));
					playAddSound(player);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean onStackClicked(ItemStack caseStack, Slot slot, ClickType clickType, PlayerEntity player) {
		return bundleInteract(caseStack, slot.getStack(), clickType, player, slot::setStack, false) || super.onStackClicked(caseStack, slot, clickType, player);
	}

	@Override
	public boolean onClicked(ItemStack caseStack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
		return bundleInteract(caseStack, otherStack, clickType, player, cursorStackReference::set, true) || super.onClicked(caseStack, otherStack, slot, clickType, player, cursorStackReference);
	}

	@Override
	public void scroll(ItemStack caseStack, PlayerEntity player, int amount) {
		CollectionComponent collection = caseStack.get(Glowcase.COLLECTION_COMPONENT.get());
		if (collection != null) {
			if (amount > 0) {
				for (int i = 0; i < amount; i++) {
					collection = collection.selectPrevious(!player.isCreative());
				}
				caseStack.set(Glowcase.COLLECTION_COMPONENT.get(), collection);
			} else {
				for (int i = 0; i < Math.abs(amount); i++) {
					collection = collection.selectNext(!player.isCreative());
				}
				caseStack.set(Glowcase.COLLECTION_COMPONENT.get(), collection);
			}
			playScrollSound(player);
		}
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
		super.appendTooltip(stack, context, displayComponent, textConsumer, type);
		
		CollectionComponent collection = stack.get(Glowcase.COLLECTION_COMPONENT.get());
		textConsumer.accept(Text.translatable("item.glowcase.collection_case.tooltip.0").formatted(Formatting.GRAY));
		if (type.isCreative()) textConsumer.accept(Text.translatable("item.glowcase.collection_case.tooltip.creative.0").formatted(Formatting.DARK_GRAY));
		if (collection != null && !collection.collectables().isEmpty()) {
			textConsumer.accept(Text.translatable("item.glowcase.collection_case.tooltip.1", collection.collected(), collection.collectables().size()).formatted(Formatting.DARK_PURPLE));
			for (int i = 0; i < collection.collectables().size(); i++) {
				CollectableStack collectable = collection.collectables().get(i);
				textConsumer.accept(collectable.getCollectableName(context.getRegistryLookup(), collection.selected() == i));
			}
		}
	}

	private void playScrollSound(Entity entity) {
		entity.playSound(SoundEvents.BLOCK_LEVER_CLICK, 0.2F, 1.2F);
	}

	private void playRetrieveSound(Entity entity) {
		entity.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F);
	}

	private void playRemoveSound(Entity entity) {
		entity.playSound(SoundEvents.BLOCK_CHISELED_BOOKSHELF_INSERT, 0.8F, 0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F);
	}

	private void playAddSound(Entity entity) {
		entity.playSound(SoundEvents.BLOCK_CHISELED_BOOKSHELF_PICKUP, 0.8F, 0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F);
	}

	private void playCollectSound(Entity entity) {
		entity.playSound(SoundEvents.ITEM_BUNDLE_INSERT, 0.8F, 0.8F + entity.getEntityWorld().getRandom().nextFloat() * 0.4F);
	}
}
