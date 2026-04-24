package dev.hephaestus.glowcase.client.render.bakedbe.buffers;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.jspecify.annotations.Nullable;

public class DummyByteBufferBuilder extends ByteBufferBuilder {
	public DummyByteBufferBuilder() {
		super(0, 0);
		throw new IllegalStateException("This should be replaced by ASM");
	}

	@Override
	public long reserve(int size) {
		// Don't return 0, this is a pointer return
		throw new UnsupportedOperationException("Can't allocate memory on a null buffer! This should not be called, if it was reached something is wrong.");
	}

	@Override
	public @Nullable Result build() {
		return null;
	}

	@Override
	public void clear() {}

	@Override
	public void discard() {}

	@Override
	public void close() {}
}
