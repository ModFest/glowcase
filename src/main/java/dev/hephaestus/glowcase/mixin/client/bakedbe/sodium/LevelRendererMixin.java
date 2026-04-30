package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.render.bakedbe.level.VisibleSections;
import dev.hephaestus.glowcase.mixinsupport.sodium.CompoundLevelRendererExtension;
import dev.hephaestus.glowcase.mixinsupport.sodium.RenderSectionManagerExtension;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements CompoundLevelRendererExtension {


	private @Unique @Nullable BlockPos glowcase$lastTranslucentSortBlockPos;

	@Inject(at = @At("RETURN"), method = "scheduleTranslucentSectionResort")
	private void scheduleBakedBEResort(Vec3 cameraPos, CallbackInfo ci) {
		GlowcaseLevelRenderer levelRenderer = glowcase$getLevelRenderer();
		if (levelRenderer.visibleSections().isEmpty()) return;
		RenderSectionManagerExtension extension = (RenderSectionManagerExtension) ((SodiumWorldRendererAccessor) sodium$getWorldRenderer()).getRenderSectionManager();

		BlockPos cameraBlockPos = BlockPos.containing(cameraPos);
		boolean blockPosChanged = !cameraBlockPos.equals(this.glowcase$lastTranslucentSortBlockPos);
		TranslucencyPointOfView pointOfView = new TranslucencyPointOfView();

		for (VisibleSections.Entry section : levelRenderer.visibleSections()) {
			long sectionNode = section.sectionNode();
			pointOfView.set(cameraPos, sectionNode);
			boolean pointOfViewChanged = section.sectionInfo().isDifferentPointOfView(pointOfView);
			boolean resortBecauseBlockPosChanged = blockPosChanged && (pointOfView.isAxisAligned() || section.isNearby());
			if (pointOfViewChanged || resortBecauseBlockPosChanged) {
				extension.glowcase$scheduleBakedBESort(sectionNode);
			}
		}

		this.glowcase$lastTranslucentSortBlockPos = cameraBlockPos;
	}
}
