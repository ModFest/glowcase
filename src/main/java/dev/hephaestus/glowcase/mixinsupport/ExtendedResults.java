package dev.hephaestus.glowcase.mixinsupport;

import dev.hephaestus.glowcase.client.render.bakedbe.BakedMeshes;
import org.jspecify.annotations.Nullable;

public interface ExtendedResults {
	boolean glowcase$shouldTrickMinecraft();
	void glowcase$trickMinecraft();

	void glowcase$setBakedMeshes(BakedMeshes meshes);
	@Nullable BakedMeshes glowcase$getBakedMeshes();
}
