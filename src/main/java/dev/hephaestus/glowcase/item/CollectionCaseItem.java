package dev.hephaestus.glowcase.item;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.item.component.CollectionComponent;
import dev.hephaestus.glowcase.util.CollectableStack;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class CollectionCaseItem extends Item implements ScrollableItem {
	public CollectionCaseItem(Properties settings) {
		super(settings);
	}

	public boolean bundleInteract(ItemStack caseStack, ItemStack otherStack, ClickAction clickType, Player player, Consumer<ItemStack> otherStackSetter, boolean tooltipVisible) {
		CollectionComponent collection = caseStack.get(Glowcase.COLLECTION_COMPONENT.get());
		if (collection != null && clickType.equals(ClickAction.SECONDARY)) {
			if (otherStack.isEmpty()) { // Removal Actions
				ItemStack retrievedStack = collection.getSelectedCollectableStack();
				if (!retrievedStack.isEmpty() && collection.isSelectedCollected()) { // Retrieve Collectable
					otherStackSetter.accept(retrievedStack);
					caseStack.set(Glowcase.COLLECTION_COMPONENT.get(), collection.retrieveSelectedStack(player.isCreative()));
					if (player.isLocalPlayer()) playRetrieveSound(player);
					return true;
				} else if (player.isCreative() && tooltipVisible && collection.hasSelection()) { // Remove Collectable
					caseStack.set(Glowcase.COLLECTION_COMPONENT.get(), collection.withoutSelectedStack());
					if (player.isLocalPlayer()) playRemoveSound(player);
					return true;
				}
			} else { // Insertion Actions
				int collectionIndex = collection.getCollectionIndex(otherStack);
				if (collectionIndex != -1) { // Collect Collectable
					otherStack.shrink(collection.collectables().get(collectionIndex).getStack().getCount());
					caseStack.set(Glowcase.COLLECTION_COMPONENT.get(), collection.collectStack(collectionIndex));
					if (player.isLocalPlayer()) playCollectSound(player);
					return true;
				} else if (player.isCreative()) { // Add Collectable
					caseStack.set(Glowcase.COLLECTION_COMPONENT.get(), collection.withStackAfterSelection(otherStack));
					if (player.isLocalPlayer()) playAddSound(player);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack caseStack, Slot slot, ClickAction clickType, Player player) {
		return bundleInteract(caseStack, slot.getItem(), clickType, player, slot::setByPlayer, false) || super.overrideStackedOnOther(caseStack, slot, clickType, player);
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack caseStack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player, SlotAccess cursorStackReference) {
		return bundleInteract(caseStack, otherStack, clickType, player, cursorStackReference::set, true) || super.overrideOtherStackedOnMe(caseStack, otherStack, slot, clickType, player, cursorStackReference);
	}

	@Override
	public void scroll(ItemStack caseStack, Player player, int amount) {
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
			if (player.isLocalPlayer()) playScrollSound(player);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		super.appendHoverText(stack, context, displayComponent, textConsumer, type);

		CollectionComponent collection = stack.get(Glowcase.COLLECTION_COMPONENT.get());
		textConsumer.accept(Component.translatable("item.glowcase.collection_case.tooltip.0").withStyle(ChatFormatting.GRAY));
		if (type.isCreative()) textConsumer.accept(Component.translatable("item.glowcase.collection_case.tooltip.creative.0").withStyle(ChatFormatting.DARK_GRAY));
		if (collection != null && !collection.collectables().isEmpty()) {
			textConsumer.accept(Component.translatable("item.glowcase.collection_case.tooltip.1", collection.collected(), collection.collectables().size()).withStyle(ChatFormatting.DARK_PURPLE));
			for (int i = 0; i < collection.collectables().size(); i++) {
				CollectableStack collectable = collection.collectables().get(i);
				textConsumer.accept(collectable.getCollectableName(context.registries(), collection.selected() == i));
			}
		}
	}

	private void playScrollSound(Entity entity) {
		entity.playSound(SoundEvents.LEVER_CLICK, 0.2F, 1.2F);
	}

	private void playRetrieveSound(Entity entity) {
		entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
	}

	private void playRemoveSound(Entity entity) {
		entity.playSound(SoundEvents.CHISELED_BOOKSHELF_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
	}

	private void playAddSound(Entity entity) {
		entity.playSound(SoundEvents.CHISELED_BOOKSHELF_PICKUP, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
	}

	private void playCollectSound(Entity entity) {
		entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
	}
}
