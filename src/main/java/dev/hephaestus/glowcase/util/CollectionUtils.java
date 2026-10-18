package dev.hephaestus.glowcase.util;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Ampflower
 **/
public final class CollectionUtils {
	@SafeVarargs
	public static <T> List<T> ofSizedList(final int expected, final T... t) {
		if (t.length != expected) {
			throw new IllegalArgumentException("expected " + expected + ", got " + t.length);
		}
		return List.of(t);
	}

	@SafeVarargs
	public static <T> List<T> ofSizedElse(final List<T> fallback, final T... t) {
		if (t.length != fallback.size()) {
			return fallback;
		}
		return List.of(t);
	}

	public static <T> List<T> ofSizedElse(final List<T> fallback, final Collection<T> t) {
		if (t == null || t.size() != fallback.size()) {
			return fallback;
		}
		return List.copyOf(t);
	}

	@SafeVarargs
	public static <T> Set<T> ofSizedSet(final int expected, final T... t) {
		if (t.length != expected) {
			throw new IllegalArgumentException("expected " + expected + ", got " + t.length);
		}
		return Set.of(t);
	}

	@SafeVarargs
	public static <T> Set<T> ofSizedElse(final Set<T> fallback, final T... t) {
		if (t.length != fallback.size()) {
			return fallback;
		}
		return Set.of(t);
	}

	public static <T> Set<T> ofSizedElse(final Set<T> fallback, final Collection<T> t) {
		if (t == null || t.size() != fallback.size()) {
			return fallback;
		}
		return Set.copyOf(t);
	}
}
