package dev.hephaestus.glowcase.block.entity;

import com.mojang.serialization.Codec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

public class TextBlockEntity extends GlowcaseBlockEntity {
	public static final int PLATE_BACKGROUND = 0x44000000;

	public List<Component> lines = new ArrayList<>();
	public TextAlignment textAlignment = TextAlignment.CENTER;
	public HorizontalAlignment horizontalAlignment = HorizontalAlignment.CENTER;
	public ZOffset zOffset = ZOffset.CENTER;
	public boolean shadow = true;
	public float scale = 1F;
	public int color = ColorUtil.WHITE;
	public int backgroundColor = 0;

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

		view.store("text_alignment", TextAlignment.CODEC, this.textAlignment);
		view.store("horizontal_alignment", HorizontalAlignment.CODEC, this.horizontalAlignment);
		view.store("z_offset", ZOffset.CODEC, this.zOffset);
		view.putBoolean("shadow", this.shadow);

		view.store("lines", ComponentSerialization.CODEC.listOf(), lines);
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
		this.textAlignment = view.read("text_alignment", TextAlignment.CODEC).orElse(TextAlignment.CENTER);
		this.horizontalAlignment = view.read("horizontal_alignment", HorizontalAlignment.CODEC).orElse(HorizontalAlignment.CENTER);
		this.zOffset = view.read("z_offset", ZOffset.CODEC).orElse(ZOffset.CENTER);
		this.lines = new ArrayList<>(view.read("lines", ComponentSerialization.CODEC.listOf()).orElseGet(List::of));
		this.rebake(false);
	}

	public void rebake(boolean immediate) {
		if (!this.hasLevel() || !this.getLevel().isClientSide()) return;
		this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), immediate ? Block.UPDATE_IMMEDIATE : 0);
	}

	public enum TextAlignment implements StringRepresentable {
		LEFT,
		CENTER,
		@Deprecated
		CENTER_LEFT,
		@Deprecated
		CENTER_RIGHT,
		RIGHT;

		public static final Codec<TextAlignment> CODEC = StringRepresentable.fromEnum(TextAlignment::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}
	}

	public enum ZOffset implements StringRepresentable {
		FRONT, CENTER, BACK;

		public static final Codec<ZOffset> CODEC = StringRepresentable.fromEnum(ZOffset::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}
	}

	public enum HorizontalAlignment implements StringRepresentable {
		LEFT, CENTER, RIGHT;

		public static final Codec<HorizontalAlignment> CODEC = StringRepresentable.fromEnum(HorizontalAlignment::values);
		public static final StreamCodec<ByteBuf, HorizontalAlignment> STREAM_CODEC = ByteBufCodecs.BYTE.map(index -> TextBlockEntity.HorizontalAlignment.values()[index], textAlignment -> (byte) textAlignment.ordinal());

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}
	}

	public enum Anchor implements StringRepresentable {
		TOP_LEFT(-1, 1), TOP(0, 1), TOP_RIGHT(1, 1),
		MIDDLE_LEFT(-1, 0), MIDDLE(0, 0), MIDDLE_RIGHT(1, 0),
		BOTTOM_LEFT(-1, -1), BOTTOM(0, -1), BOTTOM_RIGHT(1, -1);

		private final int x;
		private final int y;
		Anchor(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public int getX() {
			return this.x;
		}

		public int getY() {
			return this.y;
		}

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase();
		}
	}
}
