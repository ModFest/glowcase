package dev.hephaestus.glowcase.mixinsupport;

import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public interface BakeryRenderDebug {
	List<String> glowcase$getBakeryStats();
}
