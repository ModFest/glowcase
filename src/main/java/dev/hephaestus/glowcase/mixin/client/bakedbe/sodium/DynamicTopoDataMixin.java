package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import dev.hephaestus.glowcase.mixinsupport.sodium.DirectTriggerTranslucentData;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.CombinedCameraPos;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicTopoData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.TQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.GeometryPlanes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.SectionPos;
import org.joml.Vector3dc;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(DynamicTopoData.class)
public abstract class DynamicTopoDataMixin {
	private static final @Unique TQuad[] NO_QUADS = new TQuad[0];

	@Invoker("<init>")
	private static DynamicTopoData init(SectionPos sectionPos, TQuad[] quads, GeometryPlanes geometryPlanes, Vector3dc initialCameraPos, Object2ReferenceMap<Vector3fc, float[]> distancesByNormal) {
		throw new UnsupportedOperationException("Implemented via mixin");
	}

	@Inject(at = @At("HEAD"), method = "fromMesh", cancellable = true)
	private static void createDummyTopoData(CombinedCameraPos cameraPos, TQuad[] quads, SectionPos sectionPos, GeometryPlanes geometryPlanes, CallbackInfoReturnable<DynamicTopoData> cir) {
		if (geometryPlanes == null) {
			DynamicTopoData topoData = init(sectionPos, NO_QUADS, null, null, null);
			((DirectTriggerTranslucentData) topoData).glowcase$setDummyData();
			cir.setReturnValue(topoData);
		}
	}
}
