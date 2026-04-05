package dev.hephaestus.glowcase.mixin.client.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(RenderSectionManager.class)
public class RenderSectionManagerMixin {
	@Inject(at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobResult;successfully(Ljava/lang/Object;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobResult;"), method = "submitSectionTask")
	private void clearEmptySection(CallbackInfo ci, @Local(argsOnly = true, name = "section") RenderSection section) {
		GlowcaseLevelRenderer.getInstance().queueCompilation(section.getPosition().asLong(), null);
	}
}
