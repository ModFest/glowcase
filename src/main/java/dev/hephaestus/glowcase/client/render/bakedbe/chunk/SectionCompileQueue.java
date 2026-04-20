package dev.hephaestus.glowcase.client.render.bakedbe.chunk;

import com.mojang.blaze3d.vertex.VertexSorting;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.concurrent.AllocateTask;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.util.Pool;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// On chunk rendering, the mesh is compiled and allocated on the chunk task, but to build the mesh for renderers like text, it has to be done in the main thread
// as some things are only uploaded to the texture on demand. This builds a queue to compile the mesh on the main thread and allocates on a dedicated thread.
@NullMarked
public class SectionCompileQueue implements Closeable {
	public static final Logger LOGGER = LoggerFactory.getLogger(SectionCompileQueue.class);
	// It should never run out. If it does, something is horribly wrong.
	private static final Pool<SubmitNodeStorage> nodeStoragePool = new Pool<>(SubmitNodeStorage::new, Runtime.getRuntime().availableProcessors() * 10);

	// CPU Threads x 4 should be more than enough
	private final Queue<SubmitNodeStorage> compileQueue = new Queue<>(Runtime.getRuntime().availableProcessors() * 4);
	private final Queue<AllocateTask> allocationQueue = new Queue<>(Runtime.getRuntime().availableProcessors() * 4);

	public static SubmitNodeStorage getNodeStorage() {
		return nodeStoragePool.acquire();
	}

	public static void returnNodeStorage(SubmitNodeStorage nodeStorage) {
		// Ensure this is clean for the next use
		nodeStorage.clear();
		nodeStorage.endFrame();

		nodeStoragePool.release(nodeStorage);
	}

	public void compile(long sectionPos, SubmitNodeStorage nodeStorage, VertexSorting vertexSorting) {
		// We're on the render thread already, there is no need to use the queue.
		// If we are already in the render thread, this is a priority render, so allocate it immediately.
		ProfilerFiller profiler = Profiler.get();
		profiler.push("glowcase:baked_be/compile");
		compileNow(sectionPos, nodeStorage, vertexSorting);

		nodeStoragePool.check();
		profiler.pop();
	}

	public void compileNow(long sectionNode, SubmitNodeStorage nodeStorage, VertexSorting vertexSorting) {
		GlowcaseLevelRenderer levelRenderer = GlowcaseLevelRenderer.getInstance();
		var meshes = GlowcaseLevelRenderer.getInstance().renderDispatcher().buildAllFeatures(nodeStorage, vertexSorting);

		// Release the node storage
		returnNodeStorage(nodeStorage);

		GlowcaseSectionRenderDispatcher dispatcher = levelRenderer.getSectionRenderDispatcher();
		if (dispatcher == null) {
			meshes.close();
			return;
		}

		if (dispatcher.hasAllRenderTypes(meshes.renderTypes())) {
			try (Zone _ = Profiler.get().zone("Allocate section to uber buffer")) {
				levelRenderer.allocateSectionMeshes(sectionNode, meshes);
			}
		} else {
			// Allocate on the main thread as some buffers need to be created
			allocationQueue.enqueue(sectionNode, new AllocateTask(sectionNode, meshes));
		}
	}

	public void remove(long sectionPos) {
		allocationQueue.remove(sectionPos);
		SubmitNodeStorage nodeStorage = compileQueue.remove(sectionPos);
		if (nodeStorage != null) {
			returnNodeStorage(nodeStorage);
		}
	}

	public void allocatePending() {
		allocationQueue.consume((_, item) -> {
			item.doTask();
		});
	}

	@Override
	public void close() {
		this.clear();
	}

	public void clear() {
		this.compileQueue.clear();
		this.allocationQueue.clear();
	}

	@SuppressWarnings("unchecked")
	@NullMarked
	private static class Queue<T> {
		private final long[] sectionNodes;
		private final Object[] items;
		private int takeIndex;
		private int putIndex;
		private int count;

		private final ReentrantLock lock = new ReentrantLock(false);
		private final Condition notFull = lock.newCondition();

		public Queue(int capacity) {
			if (capacity <= 0) throw new IllegalArgumentException("Capacity can not be equals or lower to 0");
			this.items = new Object[capacity];
			this.sectionNodes = new long[capacity];
		}

		/// Call only when holding lock.
		private void add(long sectionNode, T item) {
			sectionNodes[putIndex] = sectionNode;
			items[putIndex] = item;
			if (++putIndex == sectionNodes.length) putIndex = 0;
			count++;
		}

		public void enqueue(long sectionNode, T item) {
			try {
				lock.lockInterruptibly();
				try {
					while (count == items.length) notFull.await();
					add(sectionNode, item);
				} finally {
					lock.unlock();
				}
			} catch (InterruptedException _) {
				// Idk how we got here, but I guess we aren't adding this to the queue anymore
			}
		}

		public void consume(QueueConsumer<T> consumer) {
			lock.lock();
			try {
				if (count == 0) return;

				int signals = count;
				final long sectionNode = sectionNodes[takeIndex];
				final T item = (T) items[takeIndex];
				sectionNodes[takeIndex] = 0;
				//noinspection DataFlowIssue
				items[takeIndex] = null;
				if (++takeIndex == sectionNodes.length) takeIndex = 0;
				count--;
				consumer.consume(sectionNode, item);

				for (; signals > 0 && lock.hasWaiters(notFull); signals--) notFull.signal();
			} finally {
				lock.unlock();
			}
		}

		/// Call only when holding lock.
		private void removeAt(final int removeIndex) {
			if (removeIndex == takeIndex) {
				// removing front item; just advance
				sectionNodes[takeIndex] = 0;
				//noinspection DataFlowIssue
				items[takeIndex] = null;
				if (++takeIndex == sectionNodes.length) takeIndex = 0;
			} else {
				for (int i = removeIndex, putIndex = this.putIndex;;) {
					int pred = i;
					if (++i == sectionNodes.length) i = 0;
					if (i == putIndex) {
						sectionNodes[pred] = 0;
						//noinspection DataFlowIssue
						items[pred] = null;
						this.putIndex = pred;
						break;
					}
					sectionNodes[pred] = sectionNodes[i];
					items[pred] = items[i];
				}
			}

			count--;
			notFull.signal();
		}

		public @Nullable T remove(long sectionNode) {
			lock.lock();
			try {
				if (count == 0) return null;
				for (int i = takeIndex, len = sectionNodes.length; i != putIndex; i = (++i == len) ? 0 : i) {
					if (sectionNodes[i] == sectionNode) {
						T nodeStorage = (T) items[i];
						removeAt(i);
						return nodeStorage;
					}
				}

				return null;
			} finally {
				lock.unlock();
			}
		}

		public void clear() {
			lock.lock();
			try {
				if (count == 0) return;
				for (int i = takeIndex, len = sectionNodes.length; i != putIndex; i = (++i == len) ? 0 : i) {
					sectionNodes[i] = 0;
					//noinspection DataFlowIssue
					items[i] = null;
				}

				int signals = count;
				takeIndex = putIndex;
				count = 0;

				for (; signals > 0 && lock.hasWaiters(notFull); signals--) notFull.signal();
			} finally {
				lock.unlock();
			}
		}

		@FunctionalInterface
		public interface QueueConsumer<T> {
			void consume(long sectionNode, T item);
		}
	}
}
