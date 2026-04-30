package dev.hephaestus.glowcase.client.render.bakedbe.buffers;

public class MeshTooComplex extends Exception {
	public MeshTooComplex(Exception cause) {
		super("Section mesh is too complex!", cause);
	}

	public MeshTooComplex() {
		super("Section mesh is too complex!");
	}
}
