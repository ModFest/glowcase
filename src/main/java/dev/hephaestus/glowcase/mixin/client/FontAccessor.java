package dev.hephaestus.glowcase.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(Font.class)
public interface FontAccessor {
	@Invoker("getFontSet") FontSet invokeGetFontStorage(ResourceLocation id);
}
