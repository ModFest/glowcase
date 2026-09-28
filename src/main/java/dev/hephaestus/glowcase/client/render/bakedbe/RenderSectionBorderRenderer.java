package dev.hephaestus.glowcase.client.render.bakedbe;

import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryNoop;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NullMarked;

import static dev.hephaestus.glowcase.client.render.bakedbe.RenderSectionPos.SECTION_SIZE;

@NullMarked
public class RenderSectionBorderRenderer implements DebugRenderer.SimpleDebugRenderer {

	private static final float THICK_WIDTH = 4;
	private static final float THIN_WIDTH = 1;
	private static final int CELL_BORDER = ARGB.color(255, 0, 155, 155);
	private static final int YELLOW = ARGB.color(255, 255, 255, 0);
	private static final int MAJOR_LINES = ARGB.colorFromFloat(1.0F, 0.25F, 0.25F, 1.0F);
	private final Minecraft minecraft;

	public RenderSectionBorderRenderer(final Minecraft minecraft) {
		this.minecraft = minecraft;
	}

	@Override
	public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
		Entity cameraEntity = this.minecraft.getCameraEntity();
		float ymin = this.minecraft.level.getMinY();
		float ymax = this.minecraft.level.getMaxY() + 1;
		RenderSectionPos cameraPos = RenderSectionPos.fromVec3i(cameraEntity.blockPosition());
		double xstart = cameraPos.origin().getX();
		double zstart = cameraPos.origin().getZ();

		final int size = SECTION_SIZE;
		for (int x = -size; x <= size * 2; x += size) {
			for (int z = -size; z <= size * 2; z += size) {
				Gizmos.line(new Vec3(xstart + x, ymin, zstart + z), new Vec3(xstart + x, ymax, zstart + z), ARGB.colorFromFloat(0.5F, 1.0F, 0.0F, 0.0F), THICK_WIDTH);
			}
		}

		for (int x = 2; x < size; x += 2) {
			int color = x % 4 == 0 ? CELL_BORDER : YELLOW;
			Gizmos.line(new Vec3(xstart + x, ymin, zstart), new Vec3(xstart + x, ymax, zstart), color, THIN_WIDTH);
			Gizmos.line(new Vec3(xstart + x, ymin, zstart + size), new Vec3(xstart + x, ymax, zstart + size), color, THIN_WIDTH);
		}

		for (int z = 2; z < size; z += 2) {
			int color = z % 4 == 0 ? CELL_BORDER : YELLOW;
			Gizmos.line(new Vec3(xstart, ymin, zstart + z), new Vec3(xstart, ymax, zstart + z), color, THIN_WIDTH);
			Gizmos.line(new Vec3(xstart + size, ymin, zstart + z), new Vec3(xstart + size, ymax, zstart + z), color, THIN_WIDTH);
		}

		for (int y = this.minecraft.level.getMinY(); y <= this.minecraft.level.getMaxY() + 1; y += 2) {
			float yline = y;
			int color = y % 8 == 0 ? CELL_BORDER : YELLOW;
			Gizmos.line(new Vec3(xstart, yline, zstart), new Vec3(xstart, yline, zstart + size), color, THIN_WIDTH);
			Gizmos.line(new Vec3(xstart, yline, zstart + size), new Vec3(xstart + size, yline, zstart + size), color, THIN_WIDTH);
			Gizmos.line(new Vec3(xstart + size, yline, zstart + size), new Vec3(xstart + size, yline, zstart), color, THIN_WIDTH);
			Gizmos.line(new Vec3(xstart + size, yline, zstart), new Vec3(xstart, yline, zstart), color, THIN_WIDTH);
		}

		for (int x = 0; x <= size; x += size) {
			for (int z = 0; z <= size; z += size) {
				Gizmos.line(new Vec3(xstart + x, ymin, zstart + z), new Vec3(xstart + x, ymax, zstart + z), MAJOR_LINES, THICK_WIDTH);
			}
		}

		Gizmos.cuboid(cameraPos.boundingBox(), GizmoStyle.stroke(MAJOR_LINES, 1.0F)).setAlwaysOnTop();

		for (int y = this.minecraft.level.getMinY(); y <= this.minecraft.level.getMaxY() + 1; y += size) {
			Gizmos.line(new Vec3(xstart, y, zstart), new Vec3(xstart, y, zstart + size), MAJOR_LINES, THICK_WIDTH);
			Gizmos.line(new Vec3(xstart, y, zstart + size), new Vec3(xstart + size, y, zstart + size), MAJOR_LINES, THICK_WIDTH);
			Gizmos.line(new Vec3(xstart + size, y, zstart + size), new Vec3(xstart + size, y, zstart), MAJOR_LINES, THICK_WIDTH);
			Gizmos.line(new Vec3(xstart + size, y, zstart), new Vec3(xstart, y, zstart), MAJOR_LINES, THICK_WIDTH);
		}
	}
}
