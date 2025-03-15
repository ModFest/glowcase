package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagParser;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class TextBlockEntity extends GlowcaseBlockEntity {
	public static final NodeParser PARSER = TagParser.DEFAULT;
	public List<Text> lines = new ArrayList<>();
	public TextAlignment textAlignment = TextAlignment.CENTER;
	public ZOffset zOffset = ZOffset.CENTER;
	public ShadowType shadowType = ShadowType.DROP;
	public float scale = 1F;
	public int color = 0xFFFFFF;
	public boolean renderDirty = true;

	public TextBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.TEXT_BLOCK_ENTITY.get(), pos, state);
		lines.add(Text.empty());
	}

	@Override
	protected void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(tag, registryLookup);

		tag.putFloat("scale", this.scale);
		tag.putInt("color", this.color);

		tag.putString("text_alignment", this.textAlignment.name());
		tag.putString("z_offset", this.zOffset.name());
		tag.putString("shadow_type", this.shadowType.name());

		NbtList lines = tag.getList("lines", 8);
		for (var text : this.lines) {
			lines.add(NbtString.of(Text.Serialization.toJsonString(text, registryLookup)));
		}

		tag.put("lines", lines);
	}

	@Override
	protected void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(tag, registryLookup);

		this.lines = new ArrayList<>();
		this.scale = tag.getFloat("scale");
		this.color = tag.getInt("color");

		this.textAlignment = TextAlignment.valueOf(tag.getString("text_alignment"));
		this.zOffset = ZOffset.valueOf(tag.getString("z_offset"));
		this.shadowType = tag.contains("shadow_type") ? ShadowType.valueOf(tag.getString("shadow_type")) : ShadowType.DROP;

		NbtList lines = tag.getList("lines", 8);

		for (NbtElement line : lines) {
			if (line.getType() == NbtElement.END_TYPE) break;
			this.lines.add(Text.Serialization.fromJson(line.asString(), registryLookup));
		}

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

	public enum TextAlignment {
		LEFT, CENTER, CENTER_LEFT, CENTER_RIGHT, RIGHT
	}

	public enum ZOffset {
		FRONT, CENTER, BACK
	}

	public enum ShadowType {
		DROP, PLATE, NONE
	}
}
