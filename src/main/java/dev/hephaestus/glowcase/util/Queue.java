package dev.hephaestus.glowcase.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@NullMarked
public class Queue<T> {
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
			for (int i = removeIndex, putIndex = this.putIndex; ; ) {
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
