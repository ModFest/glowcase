package dev.hephaestus.glowcase.client.render.bakedbe.buffers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DummyOutlineBufferSource extends OutlineBufferSource {
	private final VertexConsumer dummyConsumer = new DummyVertexConsumer();

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		return dummyConsumer;
	}

	@Override
	public void setColor(int color) {}

	@Override
	public void endOutlineBatch() {}
}
