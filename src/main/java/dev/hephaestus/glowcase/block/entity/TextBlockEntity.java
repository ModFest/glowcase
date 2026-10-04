package dev.hephaestus.glowcase.block.entity;

import com.mojang.serialization.Codec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import dev.hephaestus.glowcase.util.Anchor;
import dev.hephaestus.glowcase.util.TextJustify;
import dev.hephaestus.glowcase.util.ZOffset;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TextBlockEntity extends GlowcaseBlockEntity {
	public static final int PLATE_BACKGROUND = 0x44000000;

	public List<Component> lines = new ArrayList<>();
	public TextJustify textAlignment = TextJustify.CENTER;
	public Anchor anchor = Anchor.MIDDLE;
	public boolean shadow = true;
	public float scale = 1F;
	public int color = ColorUtil.WHITE;
	public int backgroundColor = 0;
	public Vec3 offset = Vec3.ZERO;
	public Vec3 rotation = Vec3.ZERO; // Yaw, pitch, roll

	public TextBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.TEXT_BLOCK_ENTITY.get(), pos, state);
		lines.add(Component.empty());
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);

		view.putFloat("scale", this.scale);
		view.putInt("color", this.color);
		view.putInt("background_color", this.backgroundColor);

		view.store("text_alignment", TextJustify.CODEC, this.textAlignment);
		view.store("anchor", Anchor.CODEC, this.anchor);
		view.putBoolean("shadow", this.shadow);

		view.store("lines", ComponentSerialization.CODEC.listOf(), this.lines);

		view.store("offset", Vec3.CODEC, this.offset);
		view.store("rotation", Vec3.CODEC, this.rotation);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		this.scale = view.getFloatOr("scale", 1);
		this.color = view.getIntOr("color", 0xFFFFFFFF);

		// Force-fix alpha of 0 to opaque.
		this.color = ColorUtil.alphaFallback(this.color);

		this.backgroundColor = view.getIntOr("background_color", 0);
		this.shadow = view.getBooleanOr("shadow", true);
		Anchor anchor = null;
		this.textAlignment = view.read("text_alignment", TextJustify.CODEC).orElse(TextJustify.CENTER);
		switch (this.textAlignment) {
			case CENTER_LEFT, CENTER_RIGHT -> {
				anchor = this.textAlignment.anchor;
				this.textAlignment = TextJustify.CENTER;
			}
		}
		if (anchor == null) {
			anchor = view.read("horizontal_alignment", HorizontalAlignment.CODEC)
				.map(HorizontalAlignment::getAnchor)
				.or(() -> view.read("anchor", Anchor.CODEC))
				.orElse(this.anchor);
		}
		this.anchor = anchor;
		this.lines = new ArrayList<>(view.read("lines", ComponentSerialization.CODEC.listOf()).orElseGet(List::of));

		this.offset = view.read("offset", Vec3.CODEC)
			.or(() -> view.read("z_offset", ZOffset.CODEC).map(z -> z.setZ(Vec3.ZERO)))
			.orElse(Vec3.ZERO);
		this.rotation = view.read("rotation", Vec3.CODEC).orElse(Vec3.ZERO);

		this.rebake(false);
	}

	public void rebake(boolean immediate) {
		if (!this.hasLevel() || !this.getLevel().isClientSide()) return;
		this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), immediate ? Block.UPDATE_IMMEDIATE : 0);
	}

	@Deprecated(forRemoval = true)
	public enum HorizontalAlignment implements StringRepresentable {
		LEFT(Anchor.MIDDLE_LEFT),
		CENTER(Anchor.MIDDLE),
		RIGHT(Anchor.MIDDLE_RIGHT);

		public static final Codec<HorizontalAlignment> CODEC = StringRepresentable.fromEnum(HorizontalAlignment::values);
		public static final StreamCodec<ByteBuf, HorizontalAlignment> STREAM_CODEC = ByteBufCodecs.BYTE.map(index -> TextBlockEntity.HorizontalAlignment.values()[index], textAlignment -> (byte) textAlignment.ordinal());

		public final Anchor anchor;

		HorizontalAlignment(final Anchor anchor) {
			this.anchor = anchor;
		}

		public final Anchor getAnchor() {
			return this.anchor;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}
	}

}
