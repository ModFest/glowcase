package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.mojang.blaze3d.vertex.MeshData;
import dev.hephaestus.glowcase.client.render.bakedbe.vertex.CompiledMesh;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MeshData.SortState.class)
public abstract class SortStateMixin implements CompiledMesh.Sorter {}
