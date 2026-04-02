package dev.hephaestus.glowcase.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@NullMarked
public interface DefaultedMapBase<K, V> extends Map<K, V> {
	<M extends Map<K, V>> M getDelegate();

	void assertPresent(K key);

	/// Returns the value to which the specified key is mapped, or null if this map contains no mapping for the key. \
	/// If no value is present at the given key, a default value is put and is returned instead. This may return null if the value is set `null` \
	/// This is effectively {@link Map#computeIfAbsent} but with null support and a fixed function.
	///
	/// @param key the key whose associated value is to be returned
	/// @return the value to which the specified key is mapped, or a new default if this map contains no mapping for the key
	V getValue(K key);

	/// Returns the value to which the specified key is mapped, without adding it to the map. \
	/// This is the same as the delegate's `get` method.
	/// @see Map#get
	@Nullable V getWithoutDefault(K key);

	/// Returns the value to which the specified key is mapped, or null if this map contains no mapping for the key. \
	/// If no value is present at the given key, a default value is put and is returned instead. This may return null if the value is set `null` \
	/// This is effectively {@link Map#computeIfAbsent} but with null support and a fixed function.
	///
	/// @param key the key whose associated value is to be returned
	/// @return the value to which the specified key is mapped, or a new default if this map contains no mapping for the key
	/// @deprecated Use {@link #getValue} instead.
	@Override
	@Deprecated
	V get(Object key);
}
