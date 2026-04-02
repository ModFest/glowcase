package dev.hephaestus.glowcase.client.render.bakedbe.chunk;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Set;

public final class RenderTypeGroups {
	public final Set<RenderType> unsorted;
	public final Set<RenderType> sorted;

	public RenderTypeGroups(Set<RenderType> renderTypes) {
		final ObjectSet<RenderType> unsorted = new ObjectArraySet<>();
		final ObjectSet<RenderType> sorted = new ObjectArraySet<>();

		for (RenderType renderType : renderTypes) {
			(renderType.sortOnUpload() ? sorted : unsorted).add(renderType);
		}

		this.unsorted = ObjectSets.unmodifiable(unsorted);
		this.sorted = ObjectSets.unmodifiable(sorted);
	}
}
