package dev.hephaestus.glowcase.client.render.bakedbe.chunk.concurrent;

import net.minecraft.core.BlockPos;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SectionTask {
	protected final AtomicBoolean isCancelled = new AtomicBoolean();
	protected final AtomicBoolean isCompleted = new AtomicBoolean();

	public void execute() {
		doTask();
		isCompleted.set(true);
	}

	protected abstract void doTask();

	public boolean isCompleted() {
		return isCompleted.get();
	}

	public boolean isCancelled() {
		return isCancelled.get();
	}

	public void cancel() {
		isCancelled.set(true);
	}

	public abstract BlockPos getOrigin();

	public abstract long getSectionNode();
}
