package dev.hephaestus.glowcase.mixinsupport.sodium;

import org.joml.Vector3dc;

public interface TranslucentDataExtension {
	void glowcase$trickSodiumForResorting(boolean trickSodium);
	boolean glowcase$shouldTrickSodiumForResorting();

	boolean glowcase$shouldBypassEqualsCheck();
	void glowcase$passedEqualsCheck();

	void glowcase$initialCameraPos(Vector3dc cameraPos);
	Vector3dc glowcase$initialCameraPos();
}
