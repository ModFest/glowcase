package dev.hephaestus.glowcase.mixinsupport;

import dev.hephaestus.glowcase.client.render.bakedbe.Identity;
import org.jspecify.annotations.Nullable;

public interface BakingBlockEntityRenderState {
	void glowcase$setIdentity(Object identity);
	@Nullable Identity glowcase$getIdentity();
}
