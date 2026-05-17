package dev.hephaestus.glowcase.client.render.font;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.hephaestus.glowcase.mixin.client.bakedbe.RenderSystemAccessor;
import dev.hephaestus.glowcase.util.ThreadManagement;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@NullMarked
public abstract class QueuedBakedGlyph implements BakedGlyph {
	protected final AtomicBoolean baked = new AtomicBoolean();
	protected final CountDownLatch finished = new CountDownLatch(1);

	public abstract BakedGlyph bake();

	@Override
	public TextRenderable.@Nullable Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
		if (!RenderSystem.isOnRenderThread()) {
			try {
				// This waits for the render thread to bake the glyph. It will cause the async thread to take longer, but since glyph
				// uploads are a one time thing it should be fine, unless you have obfuscated text, it triggers this almost every time
				while (!finished.await(100, TimeUnit.MILLISECONDS)) {
					// Sodium blocks the render thread waiting for tasks to complete, so we need to abort if we enter a deadlock
					if (ThreadManagement.isLongWaiting(RenderSystemAccessor.getRenderThread(), 100).isLong) {
						// The main thread is waiting indefinitely and has waited a long time or is disabled, just return, this is getting nowhere otherwise
						return null;
					}
				}
			} catch (InterruptedException e) {
				return null;
			}
		}

		return this.bake().createGlyph(x, y, color, shadowColor, style, boldOffset, shadowOffset);
	}
}
