package dev.hephaestus.glowcase.compat;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ConfigLinkBlockEntity;
import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import dev.hephaestus.glowcase.block.entity.ItemDisplayBlockEntity;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Makes Polydex hover display more correct information
 *
 * @author Patbox
 */
public class PolydexCompatibility {
	public static void onInitialize() {
		HoverDisplayBuilder.register(Glowcase.ITEM_DISPLAY_BLOCK.get(), PolydexCompatibility::setupItemDisplayBlock);
		HoverDisplayBuilder.register(Glowcase.HYPERLINK_BLOCK.get(), PolydexCompatibility::setupHyperlinkBlock);
		HoverDisplayBuilder.register(Glowcase.CONFIG_LINK_BLOCK.get(), PolydexCompatibility::setupConfigLinkBlock);
	}

	private static void setupHyperlinkBlock(HoverDisplayBuilder hoverDisplayBuilder) {
		var target = hoverDisplayBuilder.getTarget();
		if (target.player().isCreative()) {
			return;
		}

		if (target.blockEntity() instanceof HyperlinkBlockEntity blockEntity && !blockEntity.getUrl().isEmpty()) {
			hoverDisplayBuilder.setComponent(HoverDisplayBuilder.NAME, Component.literal(blockEntity.getUrl()));
			hoverDisplayBuilder.setComponent(HoverDisplayBuilder.MOD_SOURCE, Component.literal("Internet"));
		}
	}

	private static void setupConfigLinkBlock(HoverDisplayBuilder hoverDisplayBuilder) {
		var target = hoverDisplayBuilder.getTarget();
		if (target.player().isCreative()) {
			return;
		}

		if (target.blockEntity() instanceof ConfigLinkBlockEntity blockEntity && !blockEntity.getUrl().isEmpty()) {
			hoverDisplayBuilder.setComponent(HoverDisplayBuilder.NAME, Component.literal(blockEntity.getUrl()));
			hoverDisplayBuilder.setComponent(HoverDisplayBuilder.MOD_SOURCE, Component.literal("Mod Config"));
		}
	}

	private static void setupItemDisplayBlock(HoverDisplayBuilder hoverDisplayBuilder) {
		var target = hoverDisplayBuilder.getTarget();
		if (target.player().isCreative()) {
			return;
		}

		if (target.blockEntity() instanceof ItemDisplayBlockEntity blockEntity && !blockEntity.matchesStack(ItemStack.EMPTY)) {
			var item = blockEntity.getStack();
			hoverDisplayBuilder.setComponent(HoverDisplayBuilder.NAME, item.getHoverName());
			// I won't break this I promise
			hoverDisplayBuilder.setComponent(HoverDisplayBuilder.MOD_SOURCE, PolydexImpl.getMod(BuiltInRegistries.ITEM.getKey(item.getItem())));
		}
	}
}
