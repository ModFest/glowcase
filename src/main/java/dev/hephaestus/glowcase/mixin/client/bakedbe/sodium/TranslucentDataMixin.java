package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import dev.hephaestus.glowcase.mixinsupport.sodium.DirectTriggerTranslucentData;
import dev.hephaestus.glowcase.mixinsupport.sodium.SodiumCompatShortcuts;
import dev.hephaestus.glowcase.mixinsupport.sodium.TranslucentDataExtension;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicTopoData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.SectionPos;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin(TranslucentData.class)
public class TranslucentDataMixin implements TranslucentDataExtension, DirectTriggerTranslucentData {
	@Shadow @Final public SectionPos sectionPos;

	private @Unique DynamicTopoData dummyTopoData = null;
	private @Unique boolean shouldTrickSodiumForResorting = false;
	private @Unique boolean shouldBypassEqualsCheck = false;
	private @Unique boolean isDummyData = false;
	private @Unique Vector3dc glowcase$initialCameraPos;

	@Override
	public void glowcase$trickSodiumForResorting(boolean trickSodium) {
		// Avoid resetting the equals bypass
		if (shouldTrickSodiumForResorting == trickSodium) return;

		shouldTrickSodiumForResorting = trickSodium;
		shouldBypassEqualsCheck = true;
	}

	@Override
	public boolean glowcase$shouldTrickSodiumForResorting() {
		return shouldTrickSodiumForResorting;
	}

	@Override
	public boolean glowcase$shouldBypassEqualsCheck() {
		return shouldBypassEqualsCheck;
	}

	@Override
	public void glowcase$passedEqualsCheck() {
		shouldBypassEqualsCheck = false;
	}

	@Override
	public DynamicTopoData glowcase$getDummyTopoData() {
		if (dummyTopoData == null) {
			dummyTopoData = SodiumCompatShortcuts.createDummyTopoData(this.sectionPos);
		}

		return dummyTopoData;
	}

	@Override
	public boolean glowcase$hasDummyTopoData() {
		return dummyTopoData != null;
	}

	@Override
	public void glowcase$initialCameraPos(Vector3dc cameraPos) {
		glowcase$initialCameraPos = cameraPos;
	}

	@Override
	public Vector3dc glowcase$initialCameraPos() {
		return glowcase$initialCameraPos;
	}

	@Override
	public void glowcase$setDummyData() {
		isDummyData = true;
	}

	@Override
	public boolean glowcase$isDummyData() {
		return isDummyData;
	}
}
