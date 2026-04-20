package dev.hephaestus.glowcase.client.render.bakedbe.chunk.concurrent;

import dev.hephaestus.glowcase.client.render.bakedbe.BakedMeshes;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.RenderSectionPos;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;

public class AllocateTask extends SectionTask {
	private final RenderSectionPos sectionPos;
	private final BakedMeshes meshes;

	public AllocateTask(final long sectionNode, BakedMeshes meshes) {
		// Ensure that the buffers for the render types exist
		// GlowcaseLevelRenderer.getInstance().createUberBuffers(meshes.keySet());

		this.sectionPos = new RenderSectionPos(sectionNode);
		this.meshes = meshes;
	}

	@Override
	public void doTask() {
		if (isCancelled()) {
			meshes.close();
			return;
		}

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
