package dev.hephaestus.glowcase.item;

import com.mojang.datafixers.util.Pair;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ScreenBlockEntity;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static dev.hephaestus.glowcase.block.GlowcaseBlock.canEditGlowcase;

public class TabletItem extends Item {
	public TabletItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		if (world.isClient() || !stack.contains(Glowcase.SLIDESHOW_COMPONENT.get()) || !stack.contains(Glowcase.LINKED_SCREEN_COMPONENT.get()))
			return ActionResult.PASS;

		// Get components

		Pair<UUID, BlockPos> screenPos = stack.get(Glowcase.LINKED_SCREEN_COMPONENT.get());

		List<Pair<String, String>> slideshow = stack.get(Glowcase.SLIDESHOW_COMPONENT.get());
		assert slideshow != null;
		assert screenPos != null;

		int index = stack.getOrDefault(Glowcase.CURRENT_SLIDE_COMPONENT.get(), 0);
		int step = user.isSneaking() ? -1 : 1;
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
			return ActionResult.PASS;
		}

		Pair<String, String> slide = slideshow.get(index);

		if (index+step >= 0 && index+step < slideshow.size()) {
			// Add potential next image for pre-caching
			Pair<String, String> next_slide = slideshow.get(index+step);
			screen.setImage(slide.getFirst(), slide.getSecond(), next_slide.getFirst());
		} else
			screen.setImage(slide.getFirst(), slide.getSecond(), null);

		return ActionResult.SUCCESS;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		BlockPos pos = context.getBlockPos();
		ItemStack stack = context.getStack();
		World world = context.getWorld();

		if (world.isClient() || player == null)
			return ActionResult.PASS;

		if (!(player.isSneaking() && world.getBlockEntity(pos) instanceof ScreenBlockEntity screen))
			return ActionResult.PASS;

		if (!canEditGlowcase(player, pos)) {
			player.sendMessage(Text.translatable("gui.glowcase.linking_denied"), true);
			return ActionResult.SUCCESS;
		}

		// Update linked block

		Pair<UUID, BlockPos> linkedScreen = stack.getOrDefault(Glowcase.LINKED_SCREEN_COMPONENT.get(), null);
		if (linkedScreen != null && screen.macaddress.equals(linkedScreen.getFirst()) && linkedScreen.getSecond().equals(pos)) {
			stack.remove(Glowcase.LINKED_SCREEN_COMPONENT.get());
			player.sendMessage(Text.translatable("gui.glowcase.unlinked_screen"), true);
		} else {
			stack.set(Glowcase.LINKED_SCREEN_COMPONENT.get(), new Pair<>(screen.macaddress, pos));
			player.sendMessage(Text.translatable("gui.glowcase.updated_linked_screen", pos.toShortString()), true);
		}

		return ActionResult.SUCCESS;
	}

	@Override
	public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
		if (clickType == ClickType.RIGHT && otherStack.isEmpty()) {
			// Open Editor on Client

			if (stack.contains(Glowcase.LINKED_SCREEN_COMPONENT.get())) {
				// Ensure linked screen is correct before we send the client a wrong connection
				Pair<UUID, BlockPos> linkedScreen = stack.get(Glowcase.LINKED_SCREEN_COMPONENT.get());
				assert linkedScreen != null;
				if (!(player.getEntityWorld().getBlockEntity(linkedScreen.getSecond()) instanceof ScreenBlockEntity screen && screen.macaddress.equals(linkedScreen.getFirst()))) {
					stack.remove(Glowcase.LINKED_SCREEN_COMPONENT.get());
				}
			}

			if (player.getEntityWorld().isClient())
				Glowcase.proxy.openTabletEditScreen(stack);

			return true;
		}
		return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference);
	}

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		if (stack.contains(Glowcase.LINKED_SCREEN_COMPONENT.get()) && stack.contains(Glowcase.SLIDESHOW_COMPONENT.get()) && stack.contains(Glowcase.CURRENT_SLIDE_COMPONENT.get())) {
			List<Pair<String, String>> slideshow = stack.get(Glowcase.SLIDESHOW_COMPONENT.get());
			Integer index = stack.getOrDefault(Glowcase.CURRENT_SLIDE_COMPONENT.get(), 0);

			if (slideshow != null && !slideshow.isEmpty() && (index >= 0 && index < slideshow.size()))
				return true;
		}

		return super.isItemBarVisible(stack);
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		List<Pair<String, String>> slideshow = stack.get(Glowcase.SLIDESHOW_COMPONENT.get());

		int max = (slideshow != null && !slideshow.isEmpty()) ? slideshow.size()-1 : 0;
		Integer index = stack.getOrDefault(Glowcase.CURRENT_SLIDE_COMPONENT.get(), 0);

		return MathHelper.clamp(Math.round((float)index * 13.0F / (float)max), 0, 13);
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		return 0xFFFFFF;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
		textConsumer.accept(Text.translatable("item.glowcase.tablet.tooltip.0").formatted(Formatting.GRAY));
		textConsumer.accept(Text.translatable("item.glowcase.tablet.tooltip.1").formatted(Formatting.DARK_GRAY));
		textConsumer.accept(Text.translatable("item.glowcase.tablet.tooltip.2").formatted(Formatting.DARK_GRAY));
	}
}
