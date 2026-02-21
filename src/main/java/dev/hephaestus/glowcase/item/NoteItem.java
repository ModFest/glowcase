package dev.hephaestus.glowcase.item;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.item.component.NoteComponent;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class NoteItem extends Item {
	public NoteItem(Properties settings) {
		super(settings);
	}


	@Override
	public InteractionResult use(Level world, Player user, InteractionHand hand) {
		ItemStack stackInHand = user.getItemInHand(hand);

		NoteComponent noteComponent = stackInHand.get(Glowcase.NOTE_COMPONENT.get());

		// Only edit when not signed
		if (noteComponent != null && noteComponent.title().isPresent())
			return InteractionResult.PASS;

		if (world.isClientSide())
			Glowcase.proxy.openNoteEditScreen(stackInHand);

		return InteractionResult.SUCCESS;
	}

	@Override
	public Component getName(ItemStack stack) {
		if (stack.has(Glowcase.NOTE_COMPONENT.get())) {
			NoteComponent noteComponent = stack.get(Glowcase.NOTE_COMPONENT.get());
			assert noteComponent != null;
			if (noteComponent.title().isPresent())
				return Component.literal(noteComponent.title().get()).setStyle(Style.EMPTY.withItalic(true));
		}
		return super.getName(stack);
	}

	@Override
	public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		boolean signed = false;

		if (itemStack.has(Glowcase.NOTE_COMPONENT.get())) {
			NoteComponent noteComponent = itemStack.get(Glowcase.NOTE_COMPONENT.get());
			assert noteComponent != null;

			if (noteComponent.title().isPresent()) {
				signed = true;
				Component author = (noteComponent.author().isPresent()) ? Component.literal(noteComponent.author().get()) : Component.translatable("gui.glowcase.note.anonymous").withStyle(ChatFormatting.WHITE);

				textConsumer.accept(Component.translatable("item.glowcase.note.tooltip.0", author).withStyle(ChatFormatting.YELLOW));
			}
		}

		if (!signed) {
			textConsumer.accept(Component.translatable("item.glowcase.note.tooltip.1").withStyle(ChatFormatting.GRAY));
		}
	}
}
