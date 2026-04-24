package dev.hephaestus.glowcase.mixinsupport.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicTopoData;

public interface DirectTriggerTranslucentData {
	DynamicTopoData glowcase$getDummyTopoData();
	boolean glowcase$hasDummyTopoData();

	void glowcase$setDummyData();
	boolean glowcase$isDummyData();
}
