package dev.hephaestus.glowcase.item;

import dev.hephaestus.glowcase.mixin.BaseContainerBlockEntityAccessor;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

public class LockItem extends Item {
	/**
	 * Use an impossible condition for the lock
	 */
	public static final LockCode CONTAINER_LOCK = new LockCode(ItemPredicate.Builder.item().withCount(MinMaxBounds.Ints.exactly(Integer.MIN_VALUE)).build());

	public LockItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level world = context.getLevel();
		Player player = context.getPlayer();
		if (world.isClientSide() ||
			player == null ||
			!player.isCreative() ||
			!(world.getBlockEntity(context.getClickedPos()) instanceof BaseContainerBlockEntity be)) {
			return InteractionResult.PASS;
		}

		var bea = (BaseContainerBlockEntityAccessor) be;
		Component message;
		SoundEvent soundEvent;

		if (bea.glowcase$getLock().equals(LockCode.NO_LOCK)) {
			bea.glowcase$setLock(CONTAINER_LOCK);
			message = Component.translatable("gui.glowcase.locked_block", be.getDisplayName());
			soundEvent = SoundEvents.WOODEN_TRAPDOOR_CLOSE;
		} else {
			bea.glowcase$setLock(LockCode.NO_LOCK);
			message = Component.translatable("gui.glowcase.unlocked_block", be.getDisplayName());
			soundEvent = SoundEvents.WOODEN_TRAPDOOR_OPEN;
		}

		player.sendOverlayMessage(message);
		player.playSound(soundEvent,1.0F, 1.0F);
		be.setChanged();

		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		super.appendHoverText(stack, context, displayComponent, textConsumer, type);

		textConsumer.accept(Component.translatable("item.glowcase.lock.tooltip.0").withStyle(ChatFormatting.GRAY));
	}
}
