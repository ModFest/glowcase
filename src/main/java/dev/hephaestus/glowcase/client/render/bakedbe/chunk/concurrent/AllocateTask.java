package dev.hephaestus.glowcase.client.render.bakedbe.chunk.concurrent;

import dev.hephaestus.glowcase.client.render.bakedbe.BakedMeshes;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.RenderSectionPos;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;

public class AllocateTask implements SectionTask {
	private final RenderSectionPos sectionPos;
	private final BakedMeshes meshes;

	public AllocateTask(final long sectionNode, BakedMeshes meshes) {
		this.sectionPos = new RenderSectionPos(sectionNode);
		this.meshes = meshes;
	}

	@Override
	public void execute() {
		try (Zone _ = Profiler.get().zone("Allocate section to uber buffer")) {
			GlowcaseLevelRenderer.getInstance().allocateSectionMeshes(sectionPos.asLong(), meshes);
		}
	}

	@Override
	public void cancel() {
		meshes.close();
	}
}
