package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.client.sodium.AsyncEntityRenderer;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.client.render.immediate.model.EntityRenderer;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ModelCuboid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
	@Inject(at = @At("HEAD"), method = "renderCuboid", cancellable = true)
    private static void fixAsync(PoseStack.Pose matrices, VertexBufferWriter writer, ModelCuboid cuboid, int light, int overlay, int color, CallbackInfo ci) {
		// Stop sodium from using a shared matrix stack across all threads
		if (!RenderSystem.isOnRenderThread()) {
			ci.cancel();
			AsyncEntityRenderer.INSTANCE.get().renderCuboid(matrices, writer, cuboid, light, overlay, color);
		}
    }
}
