package dev.hephaestus.glowcase.mixinsupport;

import net.minecraft.client.renderer.SubmitNodeStorage;
import org.jspecify.annotations.NullUnmarked;

@NullUnmarked
public interface BakedBECompilerResultExtension {
	void glowcase$setNodeStorage(SubmitNodeStorage nodeStorage);
	SubmitNodeStorage glowcase$getNodeStorage();
}
