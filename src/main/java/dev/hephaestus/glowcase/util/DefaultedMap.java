package dev.hephaestus.glowcase.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

@NullMarked
public class DefaultedMap<M extends Map<K, V>, K, V> implements DefaultedMapBase<K, V> {
	private final Function<K, V> defaultValue;
	private final M delegate;

	public DefaultedMap(M delegate, Function<K, V> defaultValue) {
		this.defaultValue = Objects.requireNonNull(defaultValue);
		this.delegate = Objects.requireNonNull(delegate);
	}

	public static <K, V> DefaultedMap<HashMap<K, V>, K, V> hashMap(Function<K, V> defaultValue) {
		return new DefaultedMap<>(new HashMap<>(), defaultValue);
	}

	public static <K, V> DefaultedMap<Object2ObjectOpenHashMap<K, V>, K, V> openHashMap(Function<K, V> defaultValue) {
		return new DefaultedMap<>(new Object2ObjectOpenHashMap<>(), defaultValue);
	}

	@SuppressWarnings("unchecked")
	@Override
	public M getDelegate() {
		return delegate;
	}

	@Override
	public void assertPresent(K key) {
		if (!delegate.containsKey(key)) {
			delegate.put(key, defaultValue.apply(key));
		}
	}

	@Override
	public V getValue(K key) {
		assertPresent(key);
		return delegate.get(key);
	}

	@Override
	public @Nullable V getWithoutDefault(K key) {
		return delegate.get(key);
	}

	@Override
	@SuppressWarnings({"SuspiciousMethodCalls", "unchecked", "DataFlowIssue"})
	public V get(Object key) {
		V value = delegate.get(key);
		if (value == null && !delegate.containsKey(key)) {
			try {
				K mapKey = (K) key;
				value = defaultValue.apply(mapKey);
				delegate.put(mapKey, value);
			} catch (ClassCastException ignored) {
				// The key is not of valid type, so it should not get a default value
			}
		}

		return value;
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	@Override
	public V put(K key, V value) {
		return delegate.put(key, value);
	}

	@Override
	public V remove(Object key) {
		return delegate.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		delegate.putAll(m);
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public Set<K> keySet() {
		return delegate.keySet();
	}

	@Override
	public Collection<V> values() {
		return delegate.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return delegate.entrySet();
	}
}
