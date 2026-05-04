package dev.hephaestus.glowcase.util;

/**
 * Just a dummy class to make it harder to accidentally ClassCastException when passing to methods that expect a
 * specific class that can't be explicitly specified due to it being optionally loaded.
 */
public interface RequiresRRVLoaded { }
