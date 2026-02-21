package dev.hephaestus.glowcase.item;

import com.mojang.datafixers.util.Pair;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ScreenBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static dev.hephaestus.glowcase.block.GlowcaseBlock.canEditGlowcase;

public class TabletItem extends Item {
	public TabletItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult use(Level world, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);

		if (world.isClientSide() || !stack.has(Glowcase.SLIDESHOW_COMPONENT.get()) || !stack.has(Glowcase.LINKED_SCREEN_COMPONENT.get()))
			return InteractionResult.PASS;

		// Get components

		Pair<UUID, BlockPos> screenPos = stack.get(Glowcase.LINKED_SCREEN_COMPONENT.get());

		List<Pair<String, String>> slideshow = stack.get(Glowcase.SLIDESHOW_COMPONENT.get());
		assert slideshow != null;
		assert screenPos != null;

		int index = stack.getOrDefault(Glowcase.CURRENT_SLIDE_COMPONENT.get(), 0);
		int step = user.isShiftKeyDown() ? -1 : 1;
		index += step;

		// Ensure boundaries
		if (index >= slideshow.size())
			index = slideshow.size()-1;
		if (index < 0)
			index = 0;

		stack.set(Glowcase.CURRENT_SLIDE_COMPONENT.get(), index);

		if (!(world.getBlockEntity(screenPos.getSecond()) instanceof ScreenBlockEntity screen && screen.macaddress.equals(screenPos.getFirst()))) {
			// Link is invalid
			stack.remove(Glowcase.LINKED_SCREEN_COMPONENT.get());
			return InteractionResult.PASS;
		}

		Pair<String, String> slide = slideshow.get(index);

		if (index+step >= 0 && index+step < slideshow.size()) {
			// Add potential next image for pre-caching
			Pair<String, String> next_slide = slideshow.get(index+step);
			screen.setImage(slide.getFirst(), slide.getSecond(), next_slide.getFirst());
		} else
			screen.setImage(slide.getFirst(), slide.getSecond(), null);

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		BlockPos pos = context.getClickedPos();
		ItemStack stack = context.getItemInHand();
		Level world = context.getLevel();

		if (world.isClientSide() || player == null)
			return InteractionResult.PASS;

		if (!(player.isShiftKeyDown() && world.getBlockEntity(pos) instanceof ScreenBlockEntity screen))
			return InteractionResult.PASS;

		if (!canEditGlowcase(player, pos)) {
			player.displayClientMessage(Component.translatable("gui.glowcase.linking_denied"), true);
			return InteractionResult.SUCCESS;
		}

		// Update linked block

		Pair<UUID, BlockPos> linkedScreen = stack.getOrDefault(Glowcase.LINKED_SCREEN_COMPONENT.get(), null);
		if (linkedScreen != null && screen.macaddress.equals(linkedScreen.getFirst()) && linkedScreen.getSecond().equals(pos)) {
			stack.remove(Glowcase.LINKED_SCREEN_COMPONENT.get());
			player.displayClientMessage(Component.translatable("gui.glowcase.unlinked_screen"), true);
		} else {
			stack.set(Glowcase.LINKED_SCREEN_COMPONENT.get(), new Pair<>(screen.macaddress, pos));
			player.displayClientMessage(Component.translatable("gui.glowcase.updated_linked_screen", pos.toShortString()), true);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player, SlotAccess cursorStackReference) {
		if (clickType == ClickAction.SECONDARY && otherStack.isEmpty()) {
			// Open Editor on Client

			if (stack.has(Glowcase.LINKED_SCREEN_COMPONENT.get())) {
				// Ensure linked screen is correct before we send the client a wrong connection
				Pair<UUID, BlockPos> linkedScreen = stack.get(Glowcase.LINKED_SCREEN_COMPONENT.get());
				assert linkedScreen != null;
				if (!(player.level().getBlockEntity(linkedScreen.getSecond()) instanceof ScreenBlockEntity screen && screen.macaddress.equals(linkedScreen.getFirst()))) {
					stack.remove(Glowcase.LINKED_SCREEN_COMPONENT.get());
				}
			}

			if (player.level().isClientSide())
				Glowcase.proxy.openTabletEditScreen(stack);

			return true;
		}
		return super.overrideOtherStackedOnMe(stack, otherStack, slot, clickType, player, cursorStackReference);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		if (stack.has(Glowcase.LINKED_SCREEN_COMPONENT.get()) && stack.has(Glowcase.SLIDESHOW_COMPONENT.get()) && stack.has(Glowcase.CURRENT_SLIDE_COMPONENT.get())) {
			List<Pair<String, String>> slideshow = stack.get(Glowcase.SLIDESHOW_COMPONENT.get());
			Integer index = stack.getOrDefault(Glowcase.CURRENT_SLIDE_COMPONENT.get(), 0);

			if (slideshow != null && !slideshow.isEmpty() && (index >= 0 && index < slideshow.size()))
				return true;
		}

		return super.isBarVisible(stack);
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		List<Pair<String, String>> slideshow = stack.get(Glowcase.SLIDESHOW_COMPONENT.get());

		int max = (slideshow != null && !slideshow.isEmpty()) ? slideshow.size()-1 : 0;
		Integer index = stack.getOrDefault(Glowcase.CURRENT_SLIDE_COMPONENT.get(), 0);

		return Mth.clamp(Math.round((float)index * 13.0F / (float)max), 0, 13);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0xFFFFFF;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("item.glowcase.tablet.tooltip.0").withStyle(ChatFormatting.GRAY));
		textConsumer.accept(Component.translatable("item.glowcase.tablet.tooltip.1").withStyle(ChatFormatting.DARK_GRAY));
		textConsumer.accept(Component.translatable("item.glowcase.tablet.tooltip.2").withStyle(ChatFormatting.DARK_GRAY));
	}
}
