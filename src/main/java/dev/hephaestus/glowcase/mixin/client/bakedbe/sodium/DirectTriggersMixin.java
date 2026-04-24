package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import dev.hephaestus.glowcase.mixinsupport.sodium.DirectTriggerTranslucentData;
import dev.hephaestus.glowcase.mixinsupport.sodium.DirectTriggersExtension;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicTopoData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.CameraMovement;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.SortTriggering;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.DirectTriggers")
public abstract class DirectTriggersMixin implements DirectTriggersExtension {
	@Shadow public abstract void integrateSection(SortTriggering ts, SectionPos sectionPos, DynamicTopoData data, CameraMovement movement);

	@Shadow
	public abstract void removeSection(long sectionPos, TranslucentData data);

	@Override
	public void glowcase$integrateSection(SortTriggering ts, SectionPos sectionPos, TranslucentData data, CameraMovement movement) {
		this.integrateSection(ts, sectionPos, ((DirectTriggerTranslucentData) data).glowcase$getDummyTopoData(), movement);
	}

	@Inject(at = @At("RETURN"), method = "removeSection")
	private void removeBakedBE(long sectionPos, TranslucentData data, CallbackInfo ci) {
		if (data == null) return;
		DirectTriggerTranslucentData directTriggerData = (DirectTriggerTranslucentData) data;
		// We don't want to initialize it on removal, nor to recurse infinitely with the dummy data
		if (!directTriggerData.glowcase$hasDummyTopoData() || directTriggerData.glowcase$isDummyData()) return;

		removeSection(sectionPos, directTriggerData.glowcase$getDummyTopoData());
	}
}
