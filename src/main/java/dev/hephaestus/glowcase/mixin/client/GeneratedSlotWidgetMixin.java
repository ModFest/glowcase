package dev.hephaestus.glowcase.mixin.client;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.GeneratedSlotWidget;
import dev.hephaestus.glowcase.util.EmiClientUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GeneratedSlotWidget.class)
public class GeneratedSlotWidgetMixin {
    @Unique
    private int lastStackHash = -1;

    @Inject(method = "getStack", at = @At("RETURN"), cancellable = true, remap = false)
    private void glowcase$onGetStack(CallbackInfoReturnable<EmiIngredient> cir) {
        EmiIngredient current = cir.getReturnValue();

        int currentHash = current.hashCode();
        if (currentHash != lastStackHash) {
            lastStackHash = currentHash;

            // mark aur cached buffer dirty
            EmiClientUtils.markAllDirty();
        }
    }    
}