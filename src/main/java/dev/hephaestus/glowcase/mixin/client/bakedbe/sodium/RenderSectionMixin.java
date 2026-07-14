package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.sodium.BakedBESortingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(RenderSection.class)
public class RenderSectionMixin {
	@Shadow @Final private int chunkX;
	@Shadow @Final private int chunkY;
	@Shadow @Final private int chunkZ;

	@Inject(at = @At("RETURN"), method = "clearRenderState")
	private void onSectionReleased(CallbackInfoReturnable<Boolean> cir) {
		GlowcaseLevelRenderer.getInstance().releaseSection(SectionPos.asLong(chunkX, chunkY, chunkZ));
	}

	@Inject(method = "addBuildOutput", at = @At("HEAD"), cancellable = true)
	private void fixInstanceofByBlocking(BuilderTaskOutput output, CallbackInfoReturnable<Boolean> cir) {
		if (output instanceof BakedBESortingTask.Output) {
			cir.setReturnValue(false);
		}
	}
}
