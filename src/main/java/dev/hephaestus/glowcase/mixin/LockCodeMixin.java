package dev.hephaestus.glowcase.mixin;

import dev.hephaestus.glowcase.item.LockItem;
import net.minecraft.world.LockCode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LockCode.class)
public class LockCodeMixin {
	@SuppressWarnings("EqualsBetweenInconvertibleTypes")
	@Inject(at = @At("HEAD"), method = "unlocksWith", cancellable = true)
	private void canOpen(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (LockItem.CONTAINER_LOCK.equals(this)) {
			cir.setReturnValue(false);
		}
	}
}
