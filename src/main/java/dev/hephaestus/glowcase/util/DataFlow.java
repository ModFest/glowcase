package dev.hephaestus.glowcase.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.function.Function;

public class DataFlow {
    @Contract("null, _ -> null")
	public static <T, R> @Nullable R nullable(@Nullable T value, @NonNull Function<@NonNull T, @Nullable R> ifNotNull) {
        if (value == null) return null;
        return ifNotNull.apply(value);
    }
}
