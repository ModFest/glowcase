package dev.hephaestus.glowcase.mixin.client;

import dev.hephaestus.glowcase.client.util.TooltipCow;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

/**
 * @author Ampflower
 **/
@Mixin(Tooltip.class)
public class TooltipMixin implements TooltipCow {

	@Shadow
	@Final
	private Component message;

	@Shadow
	@Final
	private @Nullable Component narration;

	@Shadow
	@Final
	private Optional<TooltipComponent> component;

	@Shadow
	@Final
	private @Nullable Identifier style;

	// I mean in theory we could also just do (Tooltip)(Object)new Self(), but this is stricter,
	// and would yield more clear errors that aren't just random mid-runtime explosions.
	@Invoker("<init>")
	private static Tooltip invokeConstructor(
		final Component message,
		final Component narration,
		final Optional<TooltipComponent> component,
		final @Nullable Identifier style
	) {
		throw new AssertionError();
	}

	@Unique
	private Tooltip copyWithMessage(final Component message) {
		return invokeConstructor(
			message,
			this.narration,
			this.component,
			this.style
		);
	}

	@Override
	public Tooltip glowcase$prepend(final Component component) {
		return this.copyWithMessage(Component.empty().append(component).append(this.message));
	}

	@Override
	public Tooltip glowcase$prependLine(final Component component) {
		return this.copyWithMessage(Component.empty().append(component).append("\n").append(this.message));
	}

	@Override
	public Tooltip glowcase$append(final Component component) {
		return this.copyWithMessage(Component.empty().append(this.message).append(component));
	}

	@Override
	public Tooltip glowcase$appendLine(final Component component) {
		return this.copyWithMessage(Component.empty().append(this.message).append("\n").append(component));
	}
}
