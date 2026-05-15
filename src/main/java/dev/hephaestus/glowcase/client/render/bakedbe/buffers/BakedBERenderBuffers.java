package dev.hephaestus.glowcase.client.render.bakedbe.buffers;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Util;

import java.util.SequencedMap;

import static dev.hephaestus.glowcase.util.SizeConstants.Mi;

public class BakedBERenderBuffers {
	private static final DummyOutlineBufferSource outlineBufferSource = new DummyOutlineBufferSource();
	private static final DummyBufferSource crumblingBufferSource = new DummyBufferSource();
	private final SectionBufferBuilderPack fixedBufferPack = new SectionBufferBuilderPack();
	private final BakedBEBufferSource bufferSource;

	public BakedBERenderBuffers() {
		SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers = Util.make(new Object2ObjectLinkedOpenHashMap<>(), map -> {
			map.put(Sheets.cutoutBlockItemSheet(), this.fixedBufferPack.buffer(ChunkSectionLayer.CUTOUT));
			map.put(Sheets.translucentBlockItemSheet(), this.fixedBufferPack.buffer(ChunkSectionLayer.TRANSLUCENT));
			put(map, Sheets.cutoutItemSheet());
			put(map, Sheets.translucentItemSheet());
			put(map, RenderTypes.armorEntityGlint());
			put(map, RenderTypes.glint());
			put(map, RenderTypes.glintTranslucent());
			put(map, RenderTypes.entityGlint());
			put(map, RenderTypes.waterMask());
		});

		this.bufferSource = new BakedBEBufferSource(renderType -> new ByteBufferBuilder(renderType.bufferSize(), 32 * Mi), fixedBuffers);
	}

	private static void put(final Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder> map, final RenderType type) {
		map.put(type, new ByteBufferBuilder(type.bufferSize(), 32 * Mi));
	}

	public SectionBufferBuilderPack fixedBufferPack() {
		return this.fixedBufferPack;
	}

	public BakedBEBufferSource bufferSource() {
		return this.bufferSource;
	}

	public MultiBufferSource.BufferSource crumblingBufferSource() {
		return crumblingBufferSource;
	}

	public OutlineBufferSource outlineBufferSource() {
		return outlineBufferSource;
	}
}
