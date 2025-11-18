package dev.hephaestus.glowcase.block.entity;

import com.mojang.serialization.Codec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.util.ColorUtil;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagParser;
import net.minecraft.block.BlockState;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class TextBlockEntity extends GlowcaseBlockEntity {
	public static final NodeParser PARSER = TagParser.DEFAULT;

	public static final int PLATE_BACKGROUND = 0x44000000;

	public List<Text> lines = new ArrayList<>();
	public TextAlignment textAlignment = TextAlignment.CENTER;
	public ZOffset zOffset = ZOffset.CENTER;
	public boolean shadow = true;
	public float scale = 1F;
	public int color = ColorUtil.WHITE;
	public int backgroundColor = 0;
	public boolean renderDirty = true;
	public float viewDistance = -1.0F;

	public TextBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.TEXT_BLOCK_ENTITY.get(), pos, state);
		lines.add(Text.empty());
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);

		view.putFloat("scale", this.scale);
		view.putInt("color", this.color);
		view.putInt("background_color", this.backgroundColor);

		view.put("text_alignment", TextAlignment.CODEC, this.textAlignment);
		view.put("z_offset", ZOffset.CODEC, this.zOffset);
		view.putBoolean("shadow", this.shadow);
		view.putFloat("viewDistance", this.viewDistance);

		view.put("lines", TextCodecs.CODEC.listOf(), lines);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);

		this.scale = view.getFloat("scale", 1);
		this.color = view.getInt("color", 0xFFFFFFFF);

		// Force-fix alpha of 0 to opaque.
		if ((this.color & ColorUtil.ALPHA_MASK) == 0) {
			this.color |= ColorUtil.ALPHA_MASK;
		}

		this.backgroundColor = view.getInt("background_color", 0);
		this.shadow = view.getBoolean("shadow", true);
		this.textAlignment = view.read("text_alignment", TextAlignment.CODEC).orElse(TextAlignment.CENTER);
		this.zOffset = view.read("z_offset", ZOffset.CODEC).orElse(ZOffset.CENTER);
		this.viewDistance = view.getFloat("viewDistance", -1);
		this.lines = new ArrayList<>(view.read("lines", TextCodecs.CODEC.listOf()).orElseGet(List::of));
		this.renderDirty = true;
	}

	public String getRawLine(int i) {
		var line = this.lines.get(i);

		if (line.getStyle() == null) {
			return line.getString();
		}

		var insert = line.getStyle().getInsertion();

		if (insert == null) {
			return line.getString();
		}
		return insert;
	}

	public void addRawLine(int i, String string) {
		var parsed = PARSER.parseText(string, ParserContext.of());

		if (parsed.getString().equals(string)) {
			this.lines.add(i, Text.literal(string));
		} else {
			this.lines.add(i, Text.empty().append(parsed).setStyle(Style.EMPTY.withInsertion(string)));
		}
	}

	public void setRawLine(int i, String string) {
		var parsed = PARSER.parseText(string, ParserContext.of());

		if (parsed.getString().equals(string)) {
			this.lines.set(i, Text.literal(string));
		} else {
			this.lines.set(i, Text.empty().append(parsed).setStyle(Style.EMPTY.withInsertion(string)));
		}
	}

	public enum TextAlignment implements StringIdentifiable {
		LEFT, CENTER, CENTER_LEFT, CENTER_RIGHT, RIGHT;

		public static final Codec<TextAlignment> CODEC = StringIdentifiable.createCodec(TextAlignment::values);

		@Override
		public String asString() {
			return name().toLowerCase();
		}
	}

	public enum ZOffset implements StringIdentifiable {
		FRONT, CENTER, BACK;

		public static final Codec<ZOffset> CODEC = StringIdentifiable.createCodec(ZOffset::values);

		@Override
		public String asString() {
			return name().toLowerCase();
		}
	}
}
