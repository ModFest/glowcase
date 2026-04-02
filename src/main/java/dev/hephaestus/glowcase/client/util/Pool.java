package dev.hephaestus.glowcase.client.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.util.Util;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Cleaner;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/// A simple resource pool with a semaphore to limit it's size and leak warnings for debugging. The resources are created lazily,
/// if there is none in the pool and there is a free slot.
///
/// This does not have a close method or approach, it should only be used with classes that can be left for GC to collect
// If needed, this can get a closing strategy, but it would have to overcome some things for that, like what to do with acquired resources.
@NullMarked
public class Pool<T> {
	public static final Logger LOGGER = LoggerFactory.getLogger(Pool.class);
	private static final Cleaner CLEANER = Cleaner.create();
	private final Supplier<T> objectSupplier;
	private final Stack<T> pool = new Stack<>();
	private final Map<Integer, Lifetime> pending = new Object2ObjectArrayMap<>();
	private final Semaphore semaphore;

	public Pool(Supplier<T> objectSupplier, int max) {
		this.objectSupplier = objectSupplier;
		this.semaphore = new Semaphore(max);
	}

	/// Provides a resource from the pool if available, or creates a new one if needed.
	public synchronized T acquire() {
		if (semaphore.availablePermits() == 0) {
			LOGGER.warn("Resource pool ran out of available slots, possible resource leak!");
		}

		try {
			int seconds = 0;
			while (!semaphore.tryAcquire(3, TimeUnit.SECONDS)) {
				LOGGER.warn("Waiting for pool slot took over {} seconds, possible resource leak!", seconds += 3);
			}
		} catch (InterruptedException e) {
			LOGGER.error("Aborting pool wait, thread interrupted!", e);
		}

		T resource = pool.empty() ? objectSupplier.get() : pool.pop();
		Lifetime lifetime = new Lifetime(resource);
		pending.put(lifetime.resourceId, lifetime);

		return resource;
	}

	public synchronized void release(T resource) {
		int resourceId = System.identityHashCode(resource);
		if (pending.remove(resourceId) == null) {
			LOGGER.warn("Unexpected resource released into pool!");
		}
		pool.push(resource);
		semaphore.release();
	}

	public void check() {
		for (Lifetime lifetime : pending.values()) {
			lifetime.check();
		}
	}

	private class Lifetime {
		private static final long LIFETIME = 3000;
		private final long polledAt = Util.getMillis();
		private final StackTraceElement[] stacktrace;
		private final String resourceClassName;
		private final int resourceId;
		private long nextWarning = polledAt + LIFETIME;

		// Never hold a reference to the resource, let GC take it if it's not returned and lost
		private Lifetime(T resource) {
			this.stacktrace = new Exception().getStackTrace();
			this.resourceClassName = resource.getClass().getCanonicalName();
			this.resourceId = System.identityHashCode(resource);

			CLEANER.register(resource, () -> {
				Exception exception = new Exception();
				exception.setStackTrace(stacktrace);
				LOGGER.warn("Pool resource was collected by GC and may have not been closed properly, this can lead to unrecoverable resource leak!");
				LOGGER.error("""
						[RESOURCE LEAK] POOL RESOURCE LOST TO GC
						  Resource class: {},
						  Polled at: {},
						  Id: {},
						  Caller: {}""",
					resourceClassName,
					polledAt,
					resourceId,
					stacktrace[4], // The class is quite abstract but this is hardcoded to account for the wrapper methods
					exception
				);
				semaphore.release(); // Release to the semaphore to prevent thread starvation
				pending.remove(resourceId);
			});
		}

		void check() {
			long now = Util.getMillis();
			if (nextWarning <= now) {
				LOGGER.warn("Resource {} allocated for too long! (over {} seconds)", resourceId, (now - polledAt) / 1000f);
				nextWarning = now + LIFETIME;
			}
		}
	}
}
