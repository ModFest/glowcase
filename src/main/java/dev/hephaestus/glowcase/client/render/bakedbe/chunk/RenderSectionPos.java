package dev.hephaestus.glowcase.client.render.bakedbe.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class RenderSectionPos extends Vec3i {
	public static final int SECTION_BITS = 4;
	public static final int SECTION_SIZE = 1 << SECTION_BITS;
	public static final int SECTION_HALF_SIZE = SECTION_SIZE / 2;
	public static final int SECTION_MAX_INDEX = SECTION_SIZE - 1;
	public static final int SECTION_MASK = SECTION_MAX_INDEX;

	// The number of bits needed to represent all regions. In vanilla, it is 22
	private static final int HORIZONTAL_LENGTH_BITS = BlockPos.PACKED_HORIZONTAL_LENGTH - SECTION_BITS;
	private static final int X_BITS = /* 22 */ HORIZONTAL_LENGTH_BITS;
	private static final int Z_BITS = /* 22 */ HORIZONTAL_LENGTH_BITS;
	private static final int Y_BITS = /* 20 */ Long.SIZE - X_BITS - Z_BITS;

	private static final long X_MASK = (1L << X_BITS) - 1;
	private static final long Y_MASK = (1L << Y_BITS) - 1;
	private static final long Z_MASK = (1L << Z_BITS) - 1;

	// I have no idea why the order is Y, Z, X
	private static final int Y_OFFSET = 0;
	private static final int Z_OFFSET = Y_BITS; // 20
	private static final int X_OFFSET = Z_OFFSET + Z_BITS; // 42


	private static final int RELATIVE_Y_SHIFT = 0;
	private static final int RELATIVE_Z_SHIFT = SECTION_BITS; // 4
	private static final int RELATIVE_X_SHIFT = RELATIVE_Z_SHIFT + SECTION_BITS; // 8

	public RenderSectionPos(int x, int y, int z) {
		super(x, y, z);
	}

	public RenderSectionPos(long sectionNode) {
		this(xFromNode(sectionNode), yFromNode(sectionNode), zFromNode(sectionNode));
		if (sectionNode != asLong()) throw new RuntimeException("Math isn't mathing!");
	}

	public RenderSectionPos(BlockPos pos) {
		this(pos.getX() >> SECTION_BITS, pos.getY() >> SECTION_BITS, pos.getZ() >> SECTION_BITS);
	}

	public long asLong() {
		long node = 0;
		node |= (x() & X_MASK) << X_OFFSET;
		node |= (y() & Y_MASK) << Y_OFFSET;
		return node | (z() & Z_MASK) << Z_OFFSET;
	}

	public BlockPos origin() {
		return new BlockPos(x() << SECTION_BITS, y() << SECTION_BITS, z() << SECTION_BITS);
	}

	public BlockPos maxBlockPos() {
		return origin().offset(SECTION_MAX_INDEX, SECTION_MAX_INDEX, SECTION_MAX_INDEX);
	}

	public String toSimpleString() {
		return "(%d,%d,%d)".formatted(x(), y(), z());
	}

	public int x() {
		return this.getX();
	}

	public int y() {
		return this.getY();
	}

	public int z() {
		return this.getZ();
	}

	public static BlockPos maskToSection(BlockPos pos) {
		return new BlockPos(pos.getX() & SECTION_MASK, pos.getY() & SECTION_MASK, pos.getZ() & SECTION_MASK);
	}

	public static int xFromNode(final long sectionNode) {
		// No need to use the mask, there is no bit to mask
		return (int)(sectionNode >> X_OFFSET);
	}

	public static int yFromNode(final long sectionNode) {
		// No need to offset, it's already at the right position
		return (int)(sectionNode & Y_MASK);
	}

	public static int zFromNode(final long sectionNode) {
		return (int)(sectionNode >> Z_OFFSET & Z_MASK);
	}
}
