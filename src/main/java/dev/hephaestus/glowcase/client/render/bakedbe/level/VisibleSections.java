package dev.hephaestus.glowcase.client.render.bakedbe.level;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.hephaestus.glowcase.client.render.bakedbe.BakedMeshes;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.GlowcaseRenderSectionInfo;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

// The sections are added/removed in the remesh threads, and so are async, so this has to handle that, and the right sorting order
@NullMarked
public class VisibleSections implements Iterable<VisibleSections.Entry> {
	public static final Logger LOGGER = LoggerFactory.getLogger(VisibleSections.class);
	// Since the changes can happen async, use a queue for them so they apply on the render thread, avoiding concurrency
	private final ConcurrentLinkedDeque<Runnable> pending = new ConcurrentLinkedDeque<>();

	// The main section data
	private final Long2ObjectLinkedOpenHashMap<GlowcaseRenderSectionInfo> visibleSections = new Long2ObjectLinkedOpenHashMap<>();

	// All sections (including ones without baked BE) with their corresponding index
	private final Long2IntOpenHashMap sectionToIndex = new Long2IntOpenHashMap();
	private final ResettableLongArrayList orderedSections = new ResettableLongArrayList();
	private final ResettableObjectArrayList<@Nullable GlowcaseRenderSectionInfo> sectionDataByIndex = new ResettableObjectArrayList<>();
	private final BitSet presence = new BitSet();
	private int indexSize = 0;

	public VisibleSections() {
		sectionToIndex.defaultReturnValue(-1);
	}

	// Main public methods

	public boolean shouldResort(final LongArrayList sortList) {
		try (Zone _ = Profiler.get().zone("should_resort")) {
			if (sortList.size() != orderedSections.size()) return true;

			return !Arrays.equals(
				sortList.elements(), 0, sortList.size(),
				orderedSections.elements(), 0, orderedSections.size()
			);
		}
	}

	public void finishSorting(final LongArrayList sortList) {
		RenderSystem.assertOnRenderThread();
		resize(sortList.size());

		if (sortList.isEmpty()) return;

		ProfilerFiller profiler = Profiler.get();
		profiler.push("rebuild_indexes");

		// This is the same as the sort list size, as it was set in resize
		final int size = this.indexSize;

		// Don't use the backing array size, as it can be larger than the list size
		orderedSections.setElements(0, sortList.elements(), 0, size);

		final long[] orderedSectionsArray = orderedSections.elements();
		for (int i = 0; i < size; i++) {
			sectionToIndex.put(orderedSectionsArray[i], i);
		}

		final var iterator = visibleSections.long2ObjectEntrySet().fastIterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			int index = sectionToIndex.get(entry.getLongKey());
			if (index != -1) {
				sectionDataByIndex.set(index, entry.getValue());
				presence.set(index);
			}
		}

		profiler.pop();
	}

	public void update() {
		while (!pending.isEmpty()) pending.poll().run();
	}

	public int indexSize() {
		return indexSize;
	}

	public void setSectionDraws(final long sectionNode, final BakedMeshes meshes) {
		if (RenderSystem.isOnRenderThread()) {
			visibleSections.get(sectionNode).setSectionDraws(meshes);
		} else {
			pending.add(() -> visibleSections.get(sectionNode).setSectionDraws(meshes));
		}
	}

	public boolean isVisible(final long sectionNode) {
		return visibleSections.containsKey(sectionNode);
	}

	// Iterator

	@Override
	public Iterator<Entry> iterator() {
		if (visibleSections.isEmpty()) return EmptyIterator.INSTANCE;
		return new FastIterator();
	}

	// Map-like methods

	public boolean containsKey(final long sectionNode) {
		return visibleSections.containsKey(sectionNode);
	}

	/// Gets the section info for the given node.
	/// Use with caution, this is not guaranteed to be up to date, as sections and section data can still be in the queue
	public GlowcaseRenderSectionInfo get(final long sectionNode) {
		return visibleSections.get(sectionNode);
	}

	public void put(final long sectionNode, final GlowcaseRenderSectionInfo sectionInfo) {
		if (RenderSystem.isOnRenderThread()) {
			putSection(sectionNode, sectionInfo);
		} else {
			pending.add(() -> putSection(sectionNode, sectionInfo));
		}
	}

	public void remove(final long sectionNode) {
		if (RenderSystem.isOnRenderThread()) {
			removeSection(sectionNode);
		} else {
			pending.add(() -> removeSection(sectionNode));
		}
	}

	public void reset() {
		RenderSystem.assertOnRenderThread();
		visibleSections.clear();
		sectionToIndex.clear();
		orderedSections.clear();
		sectionDataByIndex.clear();
		presence.clear();
	}

	// Internal methods

	private void resize(final int newSize) {
		ProfilerFiller profiler = Profiler.get();
		profiler.push("resize");

		// Set the list size so it is filled and doesn't out of bound
		orderedSections.reset(newSize);
		sectionDataByIndex.reset(newSize);
		sectionToIndex.clear();
		presence.clear();

		if (newSize > indexSize) this.sectionToIndex.ensureCapacity(newSize);

		this.indexSize = newSize;

		profiler.pop();
	}

	private void putSection(final long sectionNode, final GlowcaseRenderSectionInfo sectionInfo) {
		visibleSections.put(sectionNode, sectionInfo);
		final int index = sectionToIndex.get(sectionNode);
		if (index != -1) {
			sectionDataByIndex.set(index, sectionInfo);
			presence.set(index);
		}
	}

	private void removeSection(final long sectionNode) {
		//noinspection ConstantValue
		if (visibleSections.remove(sectionNode) != null) {
			final int index = sectionToIndex.get(sectionNode);
			if (index != -1) {
				sectionDataByIndex.set(index, null);
				presence.clear(index);
			}
		}
	}

	public static class EmptyIterator implements Iterator<Entry> {
		public static final EmptyIterator INSTANCE = new EmptyIterator();

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public @Nullable Entry next() {
			return null;
		}
	}

	public class FastIterator implements Iterator<Entry> {
		private final BitSet presence = VisibleSections.this.presence;
		private final Entry entry = new Entry();
		private int index = presence.nextSetBit(0);

		@Override
		public boolean hasNext() {
			return index != -1;
		}

		@Override
		public Entry next() {
			entry.index = index;
			index = presence.nextSetBit(index + 1);
			return entry;
		}
	}

	public class Entry {
		private final long[] orderedSections = VisibleSections.this.orderedSections.elements();
		private final Object[] sectionDataByIndex = VisibleSections.this.sectionDataByIndex.elements();
		private int index;

		public long sectionNode() {
			return orderedSections[index];
		}

		public GlowcaseRenderSectionInfo sectionInfo() {
			return (GlowcaseRenderSectionInfo) sectionDataByIndex[index];
		}
	}

	// Internal classes

	private static class ResettableObjectArrayList<K extends @Nullable Object> extends ObjectArrayList<K> {
		private void reset(int size) {
			if (size > a.length) a = ObjectArrays.forceCapacity(a, size, this.size);
			Arrays.fill(a, 0, size, null);
			this.size = size;
		}
	}

	private static class ResettableLongArrayList extends LongArrayList {
		private void reset(final int size) {
			if (size > a.length) a = LongArrays.forceCapacity(a, size, this.size);
			Arrays.fill(a, 0, size, 0);
			this.size = size;
		}
	}
}
