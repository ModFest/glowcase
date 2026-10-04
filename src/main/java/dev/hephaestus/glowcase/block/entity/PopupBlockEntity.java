package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.util.TextJustify;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

public class PopupBlockEntity extends GlowcaseBlockEntity {
	public String title = "";
	public List<Component> lines = new ArrayList<>();
	public TextJustify textAlignment = TextJustify.CENTER;
	public int color = 0xFFFFFFFF;
	public boolean renderDirty = true;
	public boolean viewScreenTitle = true;

	public PopupBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.POPUP_BLOCK_ENTITY.get(), pos, state);
		lines.add(Component.empty());
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);

		view.putString("title", this.title);
		view.putBoolean("view_screen_title", this.viewScreenTitle);
		view.putInt("color", this.color);

		view.store("text_alignment", TextJustify.CODEC, this.textAlignment);

		view.store("lines", ComponentSerialization.CODEC.listOf(), this.lines);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		this.title = view.getStringOr("title", "");
		this.viewScreenTitle = view.getBooleanOr("view_screen_title", true);
		this.color = view.getIntOr("color", 0xFFFFFF);

		this.textAlignment = view.read("text_alignment", TextJustify.CODEC).orElse(TextJustify.CENTER);

		this.lines = new ArrayList<>(view.read("lines", ComponentSerialization.CODEC.listOf()).orElse(List.of(Component.empty())));

		this.renderDirty = true;
	}
}
