package dev.hephaestus.glowcase.mixinsupport;

import com.mojang.blaze3d.vertex.MeshData.SortState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface BakeryDrawInfo {
	@Nullable SortState glowcase$getSortState();
	void glowcase$setSortState(SortState sortState);
}
