package dev.hephaestus.glowcase.util.collections;

import it.unimi.dsi.fastutil.objects.*;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static it.unimi.dsi.fastutil.objects.ObjectArrayList.DEFAULT_INITIAL_CAPACITY;

public class ObjectPairArrayList<K1, K2> extends AbstractObjectList<ReferenceReferencePair<K1, K2>> implements RandomAccess {
	protected final EmptyIterator emptyIterator = new EmptyIterator();
	/// The backing array for the first object.
	protected transient K1[] a1;
	/// The backing array for the second object.
	protected transient K2[] a2;

	/// The current actual size of the list (never greater than the backing-array length).
	protected int size;

	@SuppressWarnings("unchecked")
	private void initArraysFromCapacity(final int capacity) {
		if (capacity < 0) throw new IllegalArgumentException("Initial capacity (" + capacity + ") is negative");
		if (capacity == 0) {
			a1 = (K1[]) ObjectArrays.EMPTY_ARRAY;
			a2 = (K2[]) ObjectArrays.EMPTY_ARRAY;
		} else {
			a1 = (K1[]) new Object[capacity];
			a2 = (K2[]) new Object[capacity];
		}
	}

	public ObjectPairArrayList(final int capacity) {
		initArraysFromCapacity(capacity);
	}

	/// Creates a new array list with [ObjectArrayList#DEFAULT_INITIAL_CAPACITY] capacity.
	@SuppressWarnings("unchecked")
	public ObjectPairArrayList() {
		// We delay allocation
		a1 = (K1[])ObjectArrays.DEFAULT_EMPTY_ARRAY;
		a2 = (K2[])ObjectArrays.DEFAULT_EMPTY_ARRAY;
	}

	/// Ensures that this array list can contain the given number of entries without resizing.
	///
	/// @param capacity the new minimum capacity for this array list.
	@SuppressWarnings("unchecked")
	public void ensureCapacity(final int capacity) {
		if (capacity <= a1.length || (a1 == ObjectArrays.DEFAULT_EMPTY_ARRAY && capacity <= DEFAULT_INITIAL_CAPACITY)) return;
		if (capacity > a1.length) {
			final Object[] t1 = new Object[capacity];
			System.arraycopy(a1, 0, t1, 0, size);
			a1 = (K1[]) t1;

			final Object[] t2 = new Object[capacity];
			System.arraycopy(a2, 0, t2, 0, size);
			a2 = (K2[]) t2;
		}

		assert a1.length == a2.length;
		assert size <= a1.length;
	}

	/// Grows this array list, ensuring that it can contain the given number of entries without resizing,
	/// and in case increasing the current capacity at least by a factor of 50%.
	///
	/// @param capacity the new minimum capacity for this array list.
	@SuppressWarnings("unchecked")
	private void grow(int capacity) {
		if (capacity <= a1.length) return;
		if (a1 != ObjectArrays.DEFAULT_EMPTY_ARRAY) capacity = Math.clamp((long) a1.length + (a1.length >> 1), capacity, it.unimi.dsi.fastutil.Arrays.MAX_ARRAY_SIZE);
		else if (capacity < DEFAULT_INITIAL_CAPACITY) capacity = DEFAULT_INITIAL_CAPACITY;

		final Object[] t1 = new Object[capacity];
		System.arraycopy(a1, 0, t1, 0, size);
		a1 = (K1[]) t1;

		final Object[] t2 = new Object[capacity];
		System.arraycopy(a2, 0, t2, 0, size);
		a2 = (K2[]) t2;

		assert a1.length == a2.length;
		assert size <= a1.length;
	}

	// Add

	public void add(final int index, final K1 k1, final K2 k2) {
		ensureIndex(index);
		grow(size + 1);
		if (index != size) {
			System.arraycopy(a1, index, a1, index + 1, size - index);
			System.arraycopy(a2, index, a2, index + 1, size - index);
		}

		a1[index] = k1;
		a2[index] = k2;

		size++;
		assert a1.length == a2.length;
		assert size <= a1.length;
	}

	public boolean add(final K1 k1, final K2 k2) {
		grow(size + 1);
		a1[size] = k1;
		a2[size] = k2;

		size++;
		assert a1.length == a2.length;
		assert size <= a1.length;
		return true;
	}

	/// @deprecated Use {@link #add(Object, Object)} instead
	@Override
	@Deprecated
	public boolean add(@NonNull ReferenceReferencePair<K1, K2> pair) {
		return add(pair.left(), pair.right());
	}

	/// @deprecated Use {@link #add(int, Object, Object)} instead
	@Override
	@Deprecated
	public void add(int index, @NonNull ReferenceReferencePair<K1, K2> pair) {
		this.add(index, pair.left(), pair.right());
	}

	// Get

	public K1 getLeft(final int index) {
		if (index >= size) throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + size + ")");
		return a1[index];
	}

	public K2 getRight(final int index) {
		if (index >= size) throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + size + ")");
		return a2[index];
	}

	/// @deprecated It's preferred to use the {@link #getLeft} and {@link #getRight} methods instead
	@Deprecated
	public ReferenceReferencePair<K1, K2> get(final int index) {
		if (index >= size) throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + size + ")");
		return ReferenceReferencePair.of(a1[index], a2[index]);
	}

	// Index of

	public int indexOfLeft(final Object k) {
		final Object[] a1 = this.a1;
		for (int i = 0; i < size; i++) if (Objects.equals(k, a1[i])) return i;

		return -1;
	}

	public int indexOfRight(final Object k) {
		final Object[] a2 = this.a2;
		for (int i = 0; i < size; i++) if (Objects.equals(k, a2[i])) return i;

		return -1;
	}

	public int indexOf(final Object k1, final Object k2) {
		final Object[] a1 = this.a1, a2 = this.a2;
		for (int i = 0; i < size; i++) if (Objects.equals(k1, a1[i]) && Objects.equals(k2, a2[i])) return i;

		return -1;
	}

	@Override
	public int indexOf(final Object k) {
		int i = indexOfLeft(k);
		if (i != -1) return i;

		return indexOfRight(k);
	}

	// Last index of

	public int lastIndexOfLeft(final Object k) {
		final Object[] a1 = this.a1;
		for (int i = size; i-- != 0;) if (Objects.equals(k, a1[i])) return i;

		return -1;
	}

	public int lastIndexOfRight(final Object k) {
		final Object[] a2 = this.a2;
		for (int i = size; i-- != 0;) if (Objects.equals(k, a2[i])) return i;

		return -1;
	}

	public int lastIndexOf(final Object k1, final Object k2) {
		final Object[] a1 = this.a1, a2 = this.a2;
		for (int i = size; i-- != 0;) if (Objects.equals(k1, a1[i]) && Objects.equals(k2, a2[i])) return i;

		return -1;
	}

	@Override
	public int lastIndexOf(final Object k) {
		int i = lastIndexOfLeft(k);
		if (i != -1) return i;

		return lastIndexOfRight(k);
	}

	// Remove

	public boolean removeLeft(final Object k) {
		int index = indexOfLeft(k);
		if (index == -1) return false;
		remove(index);

		assert a1.length == a2.length;
		assert size <= a1.length;
		return true;
	}

	public boolean removeRight(final Object k) {
		int index = indexOfRight(k);
		if (index == -1) return false;
		remove(index);

		assert a1.length == a2.length;
		assert size <= a1.length;
		return true;
	}

	public boolean remove(final Object k1, final Object k2) {
		int index = indexOf(k1, k2);
		if (index == -1) return false;
		remove(index);

		assert a1.length == a2.length;
		assert size <= a1.length;
		return true;
	}

	@Override
	public boolean remove(final Object k) {
		int index = indexOf(k);
		if (index == -1) return false;
		remove(index);

		assert a1.length == a2.length;
		assert size <= a1.length;
		return true;
	}

	@SuppressWarnings("ConstantValue")
	@Override
	public ReferenceReferencePair<K1, K2> remove(final int index) {
		if (index >= size) throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + size + ")");
		final K1[] a1 = this.a1;
		final K2[] a2 = this.a2;

		final K1 oldK1 = a1[index];
		final K2 oldK2 = a2[index];

		size--;

		if (index != size) {
			System.arraycopy(a1, index + 1, a1, index, size - index);
			System.arraycopy(a2, index + 1, a2, index, size - index);
		}

		a1[size] = null;
		a2[size] = null;

		assert a1.length == a2.length;
		assert size <= a1.length;

		return ReferenceReferencePair.of(oldK1, oldK2);
	}

	// Set

	public ReferenceReferencePair<K1, K2> set(final int index, final K1 k1, final K2 k2) {
		if (index >= size) throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + size + ")");
		K1 oldK1 = a1[index];
		K2 oldK2 = a2[index];

		a1[index] = k1;
		a2[index] = k2;

		return ReferenceReferencePair.of(oldK1, oldK2);
	}

	@SuppressWarnings("ConstantValue")
	@Override
	public void clear() {
		Arrays.fill(a1, 0, size, null);
		Arrays.fill(a2, 0, size, null);
		size = 0;

		assert a1.length == a2.length;
		assert size <= a1.length;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/// Trims this array list so that the capacity is equal to the size.
	///
	/// @see java.util.ArrayList#trimToSize()
	public void trim() {
		trim(0);
	}

	/// Trims the backing array if it is too large.
	///
	/// If the current array length is smaller than or equal to `n`, this method does nothing.
	/// Otherwise, it trims the array length to the maximum between `n` and [#size()].
	///
	/// This method is useful when reusing lists. [Clearing a list][#clear()] leaves the array
	/// length untouched. If you are reusing a list many times, you can call this method with a typical
	/// size to avoid keeping around a very large array just because of a few large transient lists.
	///
	/// @param n the threshold for the trimming.
	@SuppressWarnings("unchecked")
	public void trim(final int n) {
		// TODO: use Arrays.trim() and preserve type only if necessary
		if (n >= a1.length || size == a1.length) return;

		final K1[] t1 = (K1[])new Object[Math.max(n, size)];
		System.arraycopy(a1, 0, t1, 0, size);
		a1 = t1;

		final K2[] t2 = (K2[])new Object[Math.max(n, size)];
		System.arraycopy(a2, 0, t2, 0, size);
		a2 = t2;

		assert a1.length == a2.length;
		assert size <= a1.length;
	}

	@Override
	protected void ensureIndex(final int index) {
		Objects.checkIndex(index, size() + 1);
	}

	/// @deprecated Use {@link #forEach(BiConsumer)} instead
	@Override
	@Deprecated
	public void forEach(final Consumer<? super ReferenceReferencePair<K1, K2>> action) {
		forEach((k1, k2) -> action.accept(ReferenceReferencePair.of(k1, k2)));
	}

	public void forEach(final BiConsumer<? super K1, ? super K2> action) {
		final K1[] a1 = this.a1;
		final K2[] a2 = this.a2;

		for (int i = 0; i < size; ++i) {
			action.accept(a1[i], a2[i]);
		}
	}

	public void forEachLeft(final Consumer<? super K1> action) {
		final K1[] a1 = this.a1;

		for (int i = 0; i < size; ++i) {
			action.accept(a1[i]);
		}
	}

	public void forEachRight(final Consumer<? super K2> action) {
		final K2[] a2 = this.a2;

		for (int i = 0; i < size; ++i) {
			action.accept(a2[i]);
		}
	}

	@Override
	public @NonNull FastIterator iterator() {
		return listIterator();
	}

	@Override
	public @NonNull FastIterator listIterator() {
		return listIterator(0);
	}

	@SuppressWarnings("unchecked")
	@Override
	public @NonNull FastIterator listIterator(int index) {
		if (isEmpty()) return emptyIterator;
		return new ListIterator(index);
	}

	/// @deprecated I'm not bothering implementing a fast version of this
	@Override
	@Deprecated
	public @NonNull ObjectSpliterator<ReferenceReferencePair<K1, K2>> spliterator() {
		return super.spliterator();
	}

	@SuppressWarnings("unchecked")
	public class FastEntry implements ReferenceReferencePair<K1, K2> {
		private final Object[] a1 = ObjectPairArrayList.this.a1;
		private final Object[] a2 = ObjectPairArrayList.this.a2;
		private int index;

		@Override
		public K1 left() {
			return (K1) a1[index];
		}

		@Override
		public K2 right() {
			return (K2) a2[index];
		}
	}

	public abstract class FastIterator implements ObjectListIterator<ReferenceReferencePair<K1, K2>> {}

	protected class EmptyIterator extends FastIterator {
		@Override
		public ReferenceReferencePair<K1, K2> previous() {
			return null;
		}

		@Override
		public int nextIndex() {
			return 0;
		}

		@Override
		public int previousIndex() {
			return 0;
		}

		@Override
		public boolean hasPrevious() {
			return false;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public ReferenceReferencePair<K1, K2> next() {
			return null;
		}
	}

	protected class ListIterator extends FastIterator {
		private final FastEntry entry = new FastEntry();
		private final int size = ObjectPairArrayList.this.size;
		private int nextIndex;

		public ListIterator(int index) {
		    nextIndex = index;
		}

		@Override
		public FastEntry previous() {
			entry.index = nextIndex--;
			return entry;
		}

		@Override
		public int nextIndex() {
			return nextIndex;
		}

		@Override
		public int previousIndex() {
			return nextIndex - 1;
		}

		@Override
		public boolean hasPrevious() {
			return nextIndex > 0;
		}

		@Override
		public boolean hasNext() {
			return nextIndex < size;
		}

		@Override
		public FastEntry next() {
			entry.index = nextIndex++;
			return entry;
		}
	}
}
