package dev.hephaestus.glowcase.item;

import dev.hephaestus.glowcase.mixin.LockableContainerBlockEntityAccessor;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Consumer;

public class LockItem extends Item {
	/**
	 * Use an impossible condition for the lock
	 */
	public static final ContainerLock CONTAINER_LOCK = new ContainerLock(ItemPredicate.Builder.create().count(NumberRange.IntRange.exactly(Integer.MIN_VALUE)).build());

	public LockItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		PlayerEntity player = context.getPlayer();
		if (world.isClient() ||
			player == null ||
			!player.isCreative() ||
			!(world.getBlockEntity(context.getBlockPos()) instanceof LockableContainerBlockEntity be)) {
			return ActionResult.PASS;
		}

		var bea = (LockableContainerBlockEntityAccessor) be;
		Text message;
		SoundEvent soundEvent;

		if (bea.glowcase$getLock().equals(ContainerLock.EMPTY)) {
			bea.glowcase$setLock(CONTAINER_LOCK);
			message = Text.translatable("gui.glowcase.locked_block", be.getDisplayName());
			soundEvent = SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE;
		} else {
			bea.glowcase$setLock(ContainerLock.EMPTY);
			message = Text.translatable("gui.glowcase.unlocked_block", be.getDisplayName());
			soundEvent = SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN;
		}

		player.sendMessage(message, true);
		player.playSoundToPlayer(soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
		be.markDirty();

		return ActionResult.SUCCESS;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
		super.appendTooltip(stack, context, displayComponent, textConsumer, type);

		textConsumer.accept(Text.translatable("item.glowcase.lock.tooltip.0").formatted(Formatting.GRAY));
	}
}
