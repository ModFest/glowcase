package dev.hephaestus.glowcase.client.render.bakedbe.chunk.concurrent;

public interface SectionTask {
	void execute();
	void cancel();
}
