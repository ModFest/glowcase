package dev.hephaestus.glowcase.client.render.bakedbe.chunk.concurrent;

import com.mojang.blaze3d.vertex.MeshData;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.RenderSectionPos;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;

import java.util.Map;

public class AllocateTask extends SectionTask {
	private final RenderSectionPos sectionPos;
	private final Map<RenderType, MeshData> meshes;

	public AllocateTask(final long sectionNode, Map<RenderType, MeshData> meshes) {
		// Ensure that the buffers for the render types exist
		GlowcaseLevelRenderer.getInstance().createUberBuffers(meshes.keySet());

		this.sectionPos = new RenderSectionPos(sectionNode);
		this.meshes = meshes;
	}

	@Override
	public void doTask() {
		if (isCancelled()) return;

		try (Zone _ = Profiler.get().zone("Allocate section to uber buffer")) {
			GlowcaseLevelRenderer.getInstance().allocateSectionMeshes(sectionPos.asLong(), meshes);
		}
	}

	@Override
	public BlockPos getOrigin() {
		return sectionPos.origin();
	}

	@Override
	public long getSectionNode() {
		return sectionPos.asLong();
	}
}
