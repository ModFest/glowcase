package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagParser;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PopupBlockEntity extends GlowcaseBlockEntity {
	public static final NodeParser PARSER = TagParser.DEFAULT;
	public String title = "";
	public List<Component> lines = new ArrayList<>();
	public TextBlockEntity.TextAlignment textAlignment = TextBlockEntity.TextAlignment.CENTER;
	public int color = 0xFFFFFFFF;
	public boolean renderDirty = true;

	public PopupBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.POPUP_BLOCK_ENTITY.get(), pos, state);
		lines.add(Component.empty());
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);

		view.putString("title", this.title);
		view.putInt("color", this.color);

		view.store("text_alignment", TextBlockEntity.TextAlignment.CODEC, this.textAlignment);

		view.store("lines", ComponentSerialization.CODEC.listOf(), this.lines);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		this.title = view.getStringOr("title", "");
		this.color = view.getIntOr("color", 0xFFFFFF);

		this.textAlignment = view.read("text_alignment", TextBlockEntity.TextAlignment.CODEC).orElse(TextBlockEntity.TextAlignment.CENTER);

		this.lines = new ArrayList<>(view.read("lines", ComponentSerialization.CODEC.listOf()).orElse(List.of(Component.empty())));

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
		var parsed = PARSER.parseComponent(string, ParserContext.of());

		if (parsed.getString().equals(string)) {
			this.lines.add(i, Component.literal(string));
		} else {
			this.lines.add(i, Component.empty().append(parsed).setStyle(Style.EMPTY.withInsertion(string)));
		}
	}

	public void setRawLine(int i, String string) {
		var parsed = PARSER.parseComponent(string, ParserContext.of());

		if (parsed.getString().equals(string)) {
			this.lines.set(i, Component.literal(string));
		} else {
			this.lines.set(i, Component.empty().append(parsed).setStyle(Style.EMPTY.withInsertion(string)));
		}
	}
}
