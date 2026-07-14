package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.render.bakedbe.level.VisibleSections;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.RenderListProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("JavadocDeclaration")
@Environment(EnvType.CLIENT)
@Mixin(VisibleChunkCollector.class)
public abstract class VisibleChunkCollectorMixin implements RenderListProvider {
	// We can't tell how big this will be, so we'll just use the last size
	private final @Unique LongArrayList sortedSections = new LongArrayList(GlowcaseLevelRenderer.getInstance().visibleSections().indexSize());

	@Inject(at = @At("RETURN"), method = "visit")
	private void addSectionPos(int x, int y, int z, CallbackInfo ci) {
		sortedSections.add(RenderRegion.key(x, y, z));
	}

	/**
	 * @author Luna (Awakened Redstone)
	 * @reason Add hook for finalized section resort
	 */
	@Override
	public SortedRenderLists createRenderLists(Viewport viewport) {
		ProfilerFiller profiler = Profiler.get();
		profiler.push("glowcase:baked_be/tree_resort");

		VisibleSections visibleSections = GlowcaseLevelRenderer.getInstance().visibleSections();
		if (visibleSections.shouldResort(sortedSections)) {
			visibleSections.finishSorting(sortedSections);
		}

		profiler.pop();
		return RenderListProvider.super.createRenderLists(viewport);
	}
}
