package dev.hephaestus.glowcase.mixinsupport.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.CameraMovement;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.SortTriggering;
import net.minecraft.core.SectionPos;

public interface DirectTriggersExtension {
	void glowcase$integrateSection(SortTriggering ts, SectionPos sectionPos, TranslucentData data, CameraMovement movement);
}
