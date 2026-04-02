package dev.hephaestus.glowcase.mixin.client;

import net.minecraft.SharedConstants;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin {
	@Inject(at = @At("HEAD"), method = "main")
	private static void fixVersionSetupInInit(String[] args, CallbackInfo ci) {
		SharedConstants.tryDetectVersion();
	}
}
