package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class HyperlinkBlockEntity extends GlowcaseBlockEntity {
	public static final int TITLE_MAX_LENGTH = 1024;
	public static final int URL_MAX_LENGTH = 1024;
	private String title = "";
	private String url = "";

	public HyperlinkBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.HYPERLINK_BLOCK_ENTITY.get(), pos, state);
	}

	public String getText() {
		return !title.isEmpty() ? title : url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String newTitle) {
		title = newTitle;
		setChanged();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String newUrl) {
		url = newUrl;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);

		view.putString("title", this.title);
		view.putString("url", this.url);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);


		this.title = view.getStringOr("title", "");
		this.url = view.getStringOr("url", "");
	}
}
