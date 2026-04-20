package dev.hephaestus.glowcase.client.render.font;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
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
				// This waits for the render thread to bake the glyph. It will cause the async thread to take longer,
				// but since glyph uploads are a one time thing it should be fine
				finished.await();
			} catch (InterruptedException e) {
				throw new IllegalStateException("Thread interrupted while waiting for glyph baking", e);
			}
		}

		return this.bake().createGlyph(x, y, color, shadowColor, style, boldOffset, shadowOffset);
	}
}
