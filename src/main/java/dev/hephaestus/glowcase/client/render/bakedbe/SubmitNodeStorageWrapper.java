package dev.hephaestus.glowcase.client.render.bakedbe;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SubmitNodeStorageWrapper extends SubmitNodeStorage {
	private @Nullable SubmitNodeStorage delegate;

	public void setDelegate(@Nullable SubmitNodeStorage delegate) {
		if (delegate != null && this.delegate != null) {
			throw new IllegalStateException("Tried to set render queue delegate while one was already set");
		}
		this.delegate = delegate;
	}

	// Take delegate as argument as for some reason IntelliJ doesn't recognize using this.delegate as a null check
	private void assertDelegate(@Nullable SubmitNodeStorage delegate) {
		if (delegate == null) throw new IllegalStateException("Tried to use delegate while it wasn't set");
	}

	@Override
	public SubmitNodeCollection order(int order) {
		assertDelegate(delegate);
		return this.delegate.order(order);
	}

	@Override
	public void clear() {
		assertDelegate(delegate);
		delegate.clear();
	}

	@Override
	public void endFrame() {
		assertDelegate(delegate);
		delegate.endFrame();
	}

	@Override
	public Int2ObjectAVLTreeMap<SubmitNodeCollection> getSubmitsPerOrder() {
		assertDelegate(delegate);
		return this.delegate.getSubmitsPerOrder();
	}
}
