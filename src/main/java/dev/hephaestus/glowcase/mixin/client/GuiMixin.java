package dev.hephaestus.glowcase.mixin.client;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.GlowcaseClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @ModifyArg(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 0), index = 1)
    public Identifier usePickupCrosshair(Identifier original) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null && Minecraft.getInstance().hitResult instanceof BlockHitResult bhr && Minecraft.getInstance().level.getBlockState(bhr.getBlockPos()).is(Glowcase.ITEM_PROVIDER_BLOCK.get()) && Glowcase.ITEM_PROVIDER_BLOCK.get().canPickup(Minecraft.getInstance().player, bhr.getBlockPos())) {
            return GlowcaseClient.PROVIDER_CROSSHAIR_TEXTURE;
        }

        return original;
    }
}
