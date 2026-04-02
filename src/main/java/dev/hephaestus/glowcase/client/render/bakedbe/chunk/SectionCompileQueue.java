package dev.hephaestus.glowcase.client.render.bakedbe.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import dev.hephaestus.glowcase.client.render.bakedbe.SubmitNodeStorageWrapper;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.concurrent.AllocateTask;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.concurrent.PriorityTaskQueue;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.client.util.Pool;
import dev.hephaestus.glowcase.mixinsupport.BakingBufferSource;
import net.minecraft.CrashReport;
import net.minecraft.TracingExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Tuple;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// On chunk rendering, the mesh is compiled and allocated on the chunk task, but to build the mesh for renderers like text, it has to be done in the main thread
// as some things are only uploaded to the texture on demand. This builds a queue to compile the mesh on the main thread and allocates on a dedicated thread.
@NullMarked
public class SectionCompileQueue implements Closeable {
	public static final Logger LOGGER = LoggerFactory.getLogger(SectionCompileQueue.class);
	// It should never run out. If it does, something is horribly wrong.
	private static final Pool<SubmitNodeStorage> nodeStoragePool = new Pool<>(SubmitNodeStorage::new, Runtime.getRuntime().availableProcessors() * 10);
	private final TracingExecutor executor = Util.backgroundExecutor();
	private final FeatureRenderDispatcher renderDispatcher;
	// CPU Threads x 4 should be more than enough
	private final Queue compileQueue = new Queue(Runtime.getRuntime().availableProcessors() * 4);
	private final PriorityTaskQueue<AllocateTask> allocationQueue = new PriorityTaskQueue<>();
	// The node storage for the render dispatcher. This is a delegated version of it so we can reuse the dispatcher.
	private final SubmitNodeStorageWrapper dynamicNodeStorage;
	private final AtomicReference<Vec3> cameraPosition = new AtomicReference<>(Vec3.ZERO);

	public SectionCompileQueue() {
		this.dynamicNodeStorage = new SubmitNodeStorageWrapper();
		this.renderDispatcher = GlowcaseLevelRenderer.getInstance().createFeatureRenderDispatcher(this.dynamicNodeStorage);
	}

	public static SubmitNodeStorage getNodeStorage() {
		return nodeStoragePool.acquire();
	}

	public static void returnNodeStorage(SubmitNodeStorage nodeStorage) {
		// Ensure this is clean for the next use
		nodeStorage.clear();
		nodeStorage.endFrame();

		nodeStoragePool.release(nodeStorage);
	}

	private void releaseNodeStorage(SubmitNodeStorage nodeStorage) {
		// Ensure this is clean for the next use
		renderDispatcher.endFrame();

		// Remove the node storage from the dispatcher
		dynamicNodeStorage.setDelegate(null);

		returnNodeStorage(nodeStorage);
	}

	public void enqueue(long sectionPos, SubmitNodeStorage nodeStorage) {
		if (!RenderSystem.isOnRenderThread()) {
			compileQueue.enqueue(sectionPos, nodeStorage);
			return;
		}

		// We're on the render thread already, there is no need to use the queue.
		// If we are already in the render thread, this is a priority render, so allocate it immediately.
		ProfilerFiller profiler = Profiler.get();
		profiler.push("glowcase:baked_be/compile");
		compile(sectionPos, nodeStorage, false);

		profiler.popPush("glowcase:baked_be/compile/pool_check");
		nodeStoragePool.check();
		profiler.pop();
	}

	public void remove(long sectionPos) {
		allocationQueue.remove(sectionPos);
		SubmitNodeStorage nodeStorage = compileQueue.remove(sectionPos);
		if (nodeStorage != null) {
			returnNodeStorage(nodeStorage);
		}
	}

	public void setCameraPosition(Vec3 position) {
		cameraPosition.set(position);
	}

	public void compilePending() {
		ProfilerFiller profiler = Profiler.get();
		profiler.push("glowcase:baked_be/compile");

		compileQueue.consume((sectionNode, nodeStorage) -> compile(sectionNode, nodeStorage, true));

		profiler.popPush("glowcase:baked_be/compile/pool_check");
		nodeStoragePool.check();
		profiler.pop();
	}

	public void compile(long sectionNode, SubmitNodeStorage nodeStorage, boolean allocateAsync) {
		ProfilerFiller profiler = Profiler.get();
		profiler.push("build_buffers");
		dynamicNodeStorage.setDelegate(nodeStorage);
		renderDispatcher.renderAllFeatures();

		profiler.popPush("build_mesh");
		BakingBufferSource bakingBuffer = (BakingBufferSource) GlowcaseLevelRenderer.getInstance().getRenderBuffers().bufferSource();
		Map<RenderType, MeshData> meshes = bakingBuffer.glowcase$bakeAllBatches();

		// Release the node storage
		releaseNodeStorage(nodeStorage);

		if (allocateAsync) {
			// Schedule for allocating asynchronously
			schedule(new AllocateTask(sectionNode, meshes));
		} else {
			try (Zone _ = Profiler.get().zone("Allocate section to uber buffer")) {
				GlowcaseLevelRenderer.getInstance().allocateSectionMeshes(sectionNode, meshes);
			}
		}
		profiler.pop();
	}

	private void schedule(AllocateTask task) {
		allocationQueue.add(task);
		executor.execute(this::allocateNext);
	}

	private void allocateNext() {
		AllocateTask task = this.allocationQueue.poll(this.cameraPosition.get());
		if (task == null || task.isCompleted() || task.isCancelled()) return;

		try {
			task.execute();
		} catch (Exception e) {
			Minecraft.getInstance().delayCrash(CrashReport.forThrowable(e, "(Glowcase) Batching baked BE sections"));
		}
	}

	@Override
	public void close() {
		this.clear();
	}

	public void clear() {
		this.compileQueue.clear();
		this.allocationQueue.clear();
	}

	@NullMarked
	private static class Queue {
		private final long[] sectionNodes;
		private final SubmitNodeStorage[] nodeStorages;
		private int takeIndex;
		private int putIndex;
		private int count;

		private final ReentrantLock lock = new ReentrantLock(false);
		private final Condition notFull = lock.newCondition();

		public Queue(int capacity) {
			if (capacity <= 0) throw new IllegalArgumentException("Capacity can not be equals or lower to 0");
			this.nodeStorages = new SubmitNodeStorage[capacity];
			this.sectionNodes = new long[capacity];
		}

		/// Call only when holding lock.
		private void add(long sectionNode, SubmitNodeStorage nodeStorage) {
			sectionNodes[putIndex] = sectionNode;
			nodeStorages[putIndex] = nodeStorage;
			if (++putIndex == sectionNodes.length) putIndex = 0;
			count++;
		}

		public void enqueue(long sectionNode, SubmitNodeStorage nodeStorage) {
			try {
				lock.lockInterruptibly();
				try {
					while (count == nodeStorages.length) notFull.await();
					add(sectionNode, nodeStorage);
				} finally {
					lock.unlock();
				}
			} catch (InterruptedException _) {
				// Idk how we got here, but I guess we aren't adding this to the queue anymore
			}
		}

		public void consume(QueueConsumer consumer) {
			lock.lock();
			try {
				if (count == 0) return;

				int signals = count;
				final long sectionNode = sectionNodes[takeIndex];
				final SubmitNodeStorage nodeStorage = nodeStorages[takeIndex];
				sectionNodes[takeIndex] = 0;
				//noinspection DataFlowIssue
				nodeStorages[takeIndex] = null;
				if (++takeIndex == sectionNodes.length) takeIndex = 0;
				count--;
				consumer.consume(sectionNode, nodeStorage);

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
				nodeStorages[takeIndex] = null;
				if (++takeIndex == sectionNodes.length) takeIndex = 0;
			} else {
				for (int i = removeIndex, putIndex = this.putIndex;;) {
					int pred = i;
					if (++i == sectionNodes.length) i = 0;
					if (i == putIndex) {
						sectionNodes[pred] = 0;
						//noinspection DataFlowIssue
						nodeStorages[pred] = null;
						this.putIndex = pred;
						break;
					}
					sectionNodes[pred] = sectionNodes[i];
					nodeStorages[pred] = nodeStorages[i];
				}
			}

			count--;
			notFull.signal();
		}

		public @Nullable SubmitNodeStorage remove(long sectionNode) {
			lock.lock();
			try {
				if (count == 0) return null;
				for (int i = takeIndex, len = sectionNodes.length; i != putIndex; i = (++i == len) ? 0 : i) {
					if (sectionNodes[i] == sectionNode) {
						SubmitNodeStorage nodeStorage = nodeStorages[i];
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
					nodeStorages[i] = null;
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
		public interface QueueConsumer {
			void consume(long sectionNode, SubmitNodeStorage nodeStorage);
		}
	}
}
