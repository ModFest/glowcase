package dev.hephaestus.glowcase.client.render.bakedbe.buffers;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DummyBufferSource extends MultiBufferSource.BufferSource {
	private final VertexConsumer dummyConsumer = new DummyVertexConsumer();

	protected DummyBufferSource() {
		super(new DummyByteBufferBuilder(), Object2ObjectSortedMaps.emptyMap());
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		return dummyConsumer;
	}

	@Override
	public void endLastBatch() {}

	@Override
	public void endBatch() {}

	public void endBatch(final RenderType type) {}

	@Override
	protected void endBatch(RenderType type, BufferBuilder builder) {}
}
