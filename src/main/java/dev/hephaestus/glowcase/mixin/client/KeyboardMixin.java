package dev.hephaestus.glowcase.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.hephaestus.glowcase.client.gui.screen.ingame.TextBlockEditScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

	@ModifyExpressionValue(
		method = "keyPress",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/GameNarrator;isActive()Z")
	)
	private boolean preventNarratorToggleOnTextBlockScreen(boolean original) {
		//prevents the narrator from being toggled when pressing "ctrl+b" to hotkey bold formatting in the text block
		return original && !(Minecraft.getInstance().screen instanceof TextBlockEditScreen);
	}

}
