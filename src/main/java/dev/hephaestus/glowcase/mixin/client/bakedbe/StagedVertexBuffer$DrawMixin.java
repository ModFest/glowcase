package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.mojang.blaze3d.vertex.MeshData.SortState;
import dev.hephaestus.glowcase.mixinsupport.BakeryDrawInfo;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@NullMarked
@Mixin(StagedVertexBuffer.Draw.class)
public class StagedVertexBuffer$DrawMixin implements BakeryDrawInfo {
	@Unique private @Nullable SortState glowcase$sortState;

	@Override
	public @Nullable SortState glowcase$getSortState() {
		return this.glowcase$sortState;
	}

	@Override
	public void glowcase$setSortState(SortState sortState) {
		this.glowcase$sortState = sortState;
	}
}
