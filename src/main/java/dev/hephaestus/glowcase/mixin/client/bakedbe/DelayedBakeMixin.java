package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.hephaestus.glowcase.client.render.font.GlyphBakeQueue;
import dev.hephaestus.glowcase.client.render.font.QueuedBakedGlyph;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@NullMarked
@SuppressWarnings("AmbiguousMixinReference")
@Mixin(targets = "net.minecraft.client.gui.font.FontSet$DelayedBake")
public class DelayedBakeMixin {
	@Shadow private @Nullable BakedGlyph baked;

	@WrapOperation(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/font/UnbakedGlyph;bake(Lcom/mojang/blaze3d/font/UnbakedGlyph$Stitcher;)Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;"), method = "get")
	private BakedGlyph queueBakeForGettingGlyphMetaOffThread(UnbakedGlyph unbaked, UnbakedGlyph.Stitcher stitcher, Operation<BakedGlyph> original) {
		if (RenderSystem.isOnRenderThread()) {
			return original.call(unbaked, stitcher);
		}

		return GlyphBakeQueue.enqueue(new QueuedBakedGlyph() {
			@Override
			public GlyphInfo info() {
				return unbaked.info();
			}

			@Override
			public BakedGlyph bake() {
				assert DelayedBakeMixin.this.baked != null;

				if (baked.compareAndSet(false, true)) {
					try {
						DelayedBakeMixin.this.baked = original.call(unbaked, stitcher);
					} finally {
						finished.countDown();
					}
				}

				return DelayedBakeMixin.this.baked;
			}
		});
	}
}
