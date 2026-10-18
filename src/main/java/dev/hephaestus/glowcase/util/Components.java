package dev.hephaestus.glowcase.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.List;

/**
 * Various high-use components here
 *
 * @author Ampflower
 **/
// Note: avoid mutating any component here. There's no immutable component variant.
public final class Components {
	public static final List<Component> AXES = List.of(
		Component.translatable("gui.glowcase.x"),
		Component.translatable("gui.glowcase.y"),
		Component.translatable("gui.glowcase.z")
	);

	public static final List<Component> SCALE_AXES = List.of(
		Component.translatable("gui.glowcase.x_scale_label"),
		Component.translatable("gui.glowcase.y_scale_label"),
		Component.translatable("gui.glowcase.z_scale_label")
	);

	public static final List<Component> OFFSET_AXES = List.of(
		Component.translatable("gui.glowcase.x_offset_label"),
		Component.translatable("gui.glowcase.y_offset_label"),
		Component.translatable("gui.glowcase.z_offset_label")
	);

	public static final List<Component> ROTATIONS = List.of(
		Component.translatable("gui.glowcase.pitch"),
		Component.translatable("gui.glowcase.yaw"),
		Component.translatable("gui.glowcase.roll")
	);

	public static final Component scale = Component.translatable("gui.glowcase.scale_label");
	public static final Component offset = Component.translatable("gui.glowcase.offset");
	public static final Component rotation = Component.translatable("gui.glowcase.rotation");

	public static final Component where = Component.translatable("gui.glowcase.input.where");

	public static final Component paragraphSeparator = Component.literal("\n\n");
	public static final Component newLineBullet = Component.literal("\n • ");

	public static final class Color {
		public static final Component color = Component.translatable("gui.glowcase.color");
		public static final Component namedColors = Component.translatable("gui.glowcase.input.named_colors");

		// Not translation candidates
		public static final Component a = Component.literal("A").withColor(TextColor.GRAY);
		public static final Component r = Component.literal("R").withColor(TextColor.RED);
		public static final Component g = Component.literal("G").withColor(TextColor.GREEN);
		public static final Component b = Component.literal("B").withColor(TextColor.BLUE);

		// Not translation candidates
		public static final Component aa = Component.literal("AA").withColor(TextColor.GRAY);
		public static final Component rr = Component.literal("RR").withColor(TextColor.RED);
		public static final Component gg = Component.literal("GG").withColor(TextColor.GREEN);
		public static final Component bb = Component.literal("BB").withColor(TextColor.BLUE);

		public static final Component alpha = Component.translatable("color.glowcase.alpha").withColor(TextColor.GRAY);
		public static final Component red = Component.translatable("color.minecraft.red").withColor(TextColor.RED);
		public static final Component green = Component.translatable("color.minecraft.green")
			.withColor(TextColor.GREEN);
		public static final Component blue = Component.translatable("color.minecraft.blue").withColor(TextColor.BLUE);

		public static final Component aIsAlpha = Component.translatable("gui.glowcase.input.alpha.hint", a, alpha);
		public static final Component rIsRed = Component.translatable("gui.glowcase.input.red.hint", r, red);
		public static final Component gIsGreen = Component.translatable("gui.glowcase.input.green.hint", g, green);
		public static final Component bIsBlue = Component.translatable("gui.glowcase.input.blue.hint", b, blue);

		// Despite looking like a translation candidate: this is referring to a code literal.
		// Unless we wish to pipe in Minecraft to compare the raw literal to the translation value,
		// this shall suffice for now.
		public static final Component transparent = Component.literal("transparent").withColor(TextColor.GRAY);


		// Must be stitched here.
		public static final Component singleRgb = Component.literal("#")
			.append(r)
			.append(g)
			.append(b);

		public static final Component doubleRgb = Component.literal("#")
			.append(rr)
			.append(gg)
			.append(bb);

		public static final Component singleArgb = Component.literal("#")
			.append(a)
			.append(r)
			.append(g)
			.append(b);

		public static final Component doubleArgb = Component.literal("#")
			.append(aa)
			.append(rr)
			.append(gg)
			.append(bb);

		public static final Component singleRgba = Component.literal("#")
			.append(r)
			.append(g)
			.append(b)
			.append(a);

		public static final Component doubleRgba = Component.literal("#")
			.append(rr)
			.append(gg)
			.append(bb)
			.append(aa);


		public static final Component rgbTooltip = Component.translatable("gui.glowcase.input.where")
			.append(newLineBullet).append(rIsRed)
			.append(newLineBullet).append(gIsGreen)
			.append(newLineBullet).append(bIsBlue)
			.append("\n").append(Component.translatable("gui.glowcase.input.accepts_rgb"))
			.append(newLineBullet).append(singleRgb)
			.append(newLineBullet).append(doubleRgb)
			.append(newLineBullet).append(namedColors);

		public static final Component argbTooltip = Component.translatable("gui.glowcase.input.where")
			.append(newLineBullet).append(aIsAlpha)
			.append(newLineBullet).append(rIsRed)
			.append(newLineBullet).append(gIsGreen)
			.append(newLineBullet).append(bIsBlue)
			.append("\n").append(Component.translatable("gui.glowcase.input.accepts_argb"))
			.append(newLineBullet).append(singleRgb)
			.append(newLineBullet).append(doubleRgb)
			.append(newLineBullet).append(singleArgb)
			.append(newLineBullet).append(doubleArgb)
			.append(newLineBullet).append(namedColors)
			.append(newLineBullet).append(transparent);

		public static final Component rgbaTooltip = Component.translatable("gui.glowcase.input.where")
			.append(newLineBullet).append(rIsRed)
			.append(newLineBullet).append(gIsGreen)
			.append(newLineBullet).append(bIsBlue)
			.append(newLineBullet).append(aIsAlpha)
			.append("\n").append(Component.translatable("gui.glowcase.input.accepts_rgba"))
			.append(newLineBullet).append(singleRgb)
			.append(newLineBullet).append(doubleRgb)
			.append(newLineBullet).append(singleRgba)
			.append(newLineBullet).append(doubleRgba)
			.append(newLineBullet).append(namedColors)
			.append(newLineBullet).append(transparent);
	}
}
