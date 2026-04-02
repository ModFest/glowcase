package dev.hephaestus.glowcase.client.render.bakedbe.chunk.concurrent;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.ListIterator;

@NullMarked
public class PriorityTaskQueue<T extends SectionTask> {
	private final List<T> tasks = new ObjectArrayList<>();

	public synchronized void add(final T task) {
		this.tasks.add(task);
	}

	public synchronized void remove(long sectionNode) {
		ListIterator<T> iterator = this.tasks.listIterator();
		while (iterator.hasNext()) {
			T task = iterator.next();
			if (task.getSectionNode() != sectionNode) continue;

			task.cancel();
			iterator.remove();
		}
	}

	public synchronized @Nullable T poll(final Vec3 cameraPos) {
		int bestTaskIndex = -1;
		double bestDistance = Double.MAX_VALUE;

		ListIterator<T> iterator = this.tasks.listIterator();
		while (iterator.hasNext()) {
			int taskIndex = iterator.nextIndex();
			T task = iterator.next();
			if (task.isCancelled.get()) {
				iterator.remove();
				continue;
			}

			double distance = task.getOrigin().distToCenterSqr(cameraPos);
			if (distance < bestDistance) {
				bestDistance = distance;
				bestTaskIndex = taskIndex;
			}
		}

		return this.removeTaskByIndex(bestTaskIndex);
	}

	private @Nullable T removeTaskByIndex(final int taskIndex) {
		return taskIndex >= 0 ? this.tasks.remove(taskIndex) : null;
	}

	public synchronized void clear() {
		for (T task : this.tasks) {
			task.cancel();
		}

		this.tasks.clear();
	}
}
