package dev.hephaestus.glowcase.client.render.font;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GlyphBakeQueue {
	private static final Queue<QueuedBakedGlyph> glyphs = new ConcurrentLinkedQueue<>();

	public static QueuedBakedGlyph enqueue(QueuedBakedGlyph glyph) {
	    glyphs.offer(glyph);
		return glyph;
	}

	public static void bake() {
		while(!glyphs.isEmpty()) {
			glyphs.poll().bake();
		}
	}
}
