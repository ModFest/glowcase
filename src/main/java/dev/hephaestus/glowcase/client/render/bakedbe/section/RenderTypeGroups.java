package dev.hephaestus.glowcase.client.render.bakedbe.section;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.List;

public final class RenderTypeGroups {
	public final List<RenderType> unsorted;
	public final List<RenderType> sorted;

	public RenderTypeGroups(Builder builder) {
		this.unsorted = ObjectLists.unmodifiable(builder.unsorted);
		this.sorted = ObjectLists.unmodifiable(builder.sorted);
	}

	public static class Builder {
		public final ObjectList<RenderType> unsorted = new ObjectArraySet<>();
		public final ObjectList<RenderType> sorted = new ObjectArraySet<>();

	}

	public static class ObjectArraySet<T> extends ObjectArrayList<T> {
		@Override
		public boolean add(T t) {
			if (contains(t)) return false;
			return super.add(t);
		}
	}
}
