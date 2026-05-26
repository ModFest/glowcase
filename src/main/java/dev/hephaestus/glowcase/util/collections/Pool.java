package dev.hephaestus.glowcase.util.collections;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Cleaner;
import java.util.Stack;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/// A simple resource pool with a semaphore to limit it's size and leak warnings for debugging. The resources are created lazily,
/// if there is none in the pool and there is a free slot.
///
/// If a resource is lost to GC it won't be closed. If the resources have to be closed, set {@link Pool#isGCBad} to {@code true}
@NullMarked
public class Pool<T> {
	private static final boolean DEBUG_LOGGING = Boolean.getBoolean("glowcase.debug.pools");
	public static final Logger LOGGER = LoggerFactory.getLogger(Pool.class);
	private static final Cleaner CLEANER = Cleaner.create();
	private final int max;
	// Functions
	private final Supplier<T> objectSupplier;
	private final @Nullable Consumer<T> closingFunction;
	// Flags
	private final boolean isGCBad;
	private transient boolean closed;
	// Pool handling
	private final Semaphore semaphore;
	private final Stack<T> pool = new Stack<>();
	private final Int2ObjectMap<Lifetime> pending = new Int2ObjectOpenHashMap<>();
	private final Int2ObjectMap<Lifetime> pendingClosing = new Int2ObjectOpenHashMap<>();
	private final ReentrantLock lock = new ReentrantLock(false);

	public Pool(Supplier<T> objectSupplier, int max) {
		this(objectSupplier, null, max, false);
	}

	public Pool(Supplier<T> objectSupplier, @Nullable Consumer<T> closingFunction, int max, boolean isGCBad) {
		this.objectSupplier = objectSupplier;
		this.closingFunction = closingFunction;
		this.semaphore = new Semaphore(max);
		this.isGCBad = isGCBad || DEBUG_LOGGING;
		this.max = max;
	}

	/// Provides a resource from the pool if available, or creates a new one if needed.
	public T acquire() {
		final T resource;

		lock.lock();
		try {
			assertOpen();
			if (semaphore.availablePermits() == 0) {
				LOGGER.warn("Resource pool ran out of available slots, possible resource leak!");
			}

			try {
				int seconds = 0;
				while (!semaphore.tryAcquire(3, TimeUnit.SECONDS)) {
					LOGGER.warn("Waiting for pool slot took over {} seconds, possible resource leak!", seconds += 3);

				}
			} catch (InterruptedException e) {
				throw new IllegalStateException("Thread interrupted", e);
			}

			resource = pool.empty() ? objectSupplier.get() : pool.pop();
			Lifetime lifetime = new Lifetime(resource);
			pending.put(lifetime.resourceId, lifetime);
		} finally {
			lock.unlock();
		}

		return resource;
	}

	public void release(T resource) {
		lock.lock();
		try {
			if (closed) {
				if (closingFunction != null) {
					closingFunction.accept(resource);
				}
				return;
			}

			int resourceId = System.identityHashCode(resource);
			final Lifetime closing = pendingClosing.remove(resourceId);
			if (closing != null) {
				if (closingFunction != null) {
					closingFunction.accept(resource);
				}
				closing.clean();
			}

			final Lifetime pending = this.pending.remove(resourceId);
			if (pending != null) {
				pending.clean();
			} else {
				LOGGER.warn("Unexpected resource released into pool!");
			}

			pool.push(resource);
			semaphore.release();
		} finally {
			lock.unlock();
		}
	}

	public void check() {
		ProfilerFiller profiler = Profiler.get();
		profiler.push("pool_check");

		lock.lock();
		try {
			for (Lifetime lifetime : pending.values()) {
				lifetime.check();
			}
			for (Lifetime lifetime : pendingClosing.values()) {
				lifetime.check();
			}
		} finally {
			lock.unlock();
		}

		profiler.pop();
	}

	/// Clear the pool and close all resources, if needed.
	/// Any resource that was pending is closed once returned.
	private void clear() {
		lock.lock();
		try {
			if (closingFunction != null) {
				while (!pool.isEmpty()) {
					closingFunction.accept(pool.pop());
				}
			}

			pendingClosing.putAll(pending);
			pending.clear();
			semaphore.drainPermits();
			semaphore.release(this.max);
		} finally {
			lock.unlock();
		}
	}

	public void close() {
		this.closed = true;

		lock.lock();
		try {
			if (closingFunction != null) {
				while (!pool.isEmpty()) {
					closingFunction.accept(pool.pop());
				}
			}

			pendingClosing.forEach((_, v) -> v.clean());
			pendingClosing.clear();
			pending.forEach((_, v) -> v.clean());
			pending.clear();
			semaphore.drainPermits();
		} finally {
			lock.unlock();
		}
	}

	private void assertOpen() {
		if (this.closed) throw new IllegalStateException("Resource pool is closed");
	}

	private class Lifetime implements Runnable {
		private static final long MINUTE = 60 * 1000;
		private static final long LIFETIME = 3 * 1000;
		private final long polledAt = Util.getMillis();
		private final @Nullable Exception exception;
		private final String resourceClassName;
		private final int resourceId;
		private final Cleaner.Cleanable $cleanable;

		private long nextWarning = polledAt + LIFETIME;

		// if I cared do I'd do a CAS but, I don't care to.
		private volatile boolean $cleaned;

		// Never hold a reference to the resource, let GC take it if it's not returned and lost
		private Lifetime(T resource) {
			this.resourceClassName = isGCBad ? resource.getClass().getName() : resource.getClass().getSimpleName();
			this.resourceId = System.identityHashCode(resource);
			// We only need to keep the exception if GC reclaim is bad
			this.exception = isGCBad ? new Exception("Resource acquisition stacktrace") : null;

			this.$cleanable = CLEANER.register(resource, this);
		}

		@Override
		public void run() {
			if ($cleaned) {
				return;
			}

			if (isGCBad) {
				// Oh no, this getting GC'd is not good, at all
				LOGGER.error(
					"""
							[RESOURCE LEAK] POOL RESOURCE LOST TO GC  |  (Ignore if caused by game crash)
							  Resource class: {},
							  Polled at: {},
							  Lifetime: {},
							  Id: {}""",
					resourceClassName,
					polledAt,
					Util.getMillis() - polledAt,
					resourceId,
					exception
				);
			} else {
				LOGGER.warn(
					"Pool resource collected by GC, this can lead to unrecoverable resource leak! [{}] (Ignore if caused by game crash)",
					resourceClassName
				);
			}

			semaphore.release(); // Release to the semaphore to prevent thread starvation

			lock.lock();
			try {
				pending.remove(resourceId);
			} finally {
				lock.unlock();
			}
		}

		void check() {
			if (nextWarning == -1) return;

			long now = Util.getMillis();
			if (now >= polledAt + MINUTE) {
				if (exception != null) {
					LOGGER.error("Resource [class={},id={}] allocated for over a minute! Giving up on alerts.", resourceClassName, resourceId, exception);
				} else {
					LOGGER.error("Resource [class={},id={}] allocated for over a minute! Giving up on alerts.", resourceClassName, resourceId);
				}
				nextWarning = -1;
			} else if (nextWarning <= now) {
				LOGGER.warn("Resource [class={},id={}] allocated for too long! (over {} seconds)", resourceClassName, resourceId, (now - polledAt) / 1000f);
				nextWarning = now + LIFETIME;
			}
		}

		void clean() {
			if (!this.$cleaned) {
				this.$cleaned = true;
				this.$cleanable.clean();
			}
		}
	}
}
