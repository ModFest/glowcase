package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SpriteBlockEntity extends GlowcaseBlockEntity {
	protected String sprite = "arrow";
	protected @Nullable ItemStack renderItem = null;
	public int rotation = 0;
	public TextBlockEntity.ZOffset zOffset = TextBlockEntity.ZOffset.BACK;
	public int color = 0xFFFFFF;
	public float scale = 1;

	public SpriteBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.SPRITE_BLOCK_ENTITY.get(), pos, state);
	}

	public void setSprite(String newSprite) {
		sprite = newSprite;
		if (newSprite.contains(":")) {
			Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(newSprite));
			renderItem = item.map(ItemStack::new).orElse(null);
		} else {
			renderItem = null;
		}
	}

	public String getSprite() {
		return sprite;
	}

	@Nullable
	public ItemStack getRenderItem() {
		return renderItem;
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);

		view.putString("sprite", this.sprite);
		view.putInt("rotation", this.rotation);
		view.store("z_offset", TextBlockEntity.ZOffset.CODEC, this.zOffset);
		view.putInt("color", this.color);
		view.putFloat("scale", this.scale);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		setSprite(view.getStringOr("sprite", "arrow"));
		this.rotation = view.getIntOr("rotation", 0);
		this.zOffset = view.read("z_offset", TextBlockEntity.ZOffset.CODEC).orElse(TextBlockEntity.ZOffset.BACK);
		this.color = view.getIntOr("color", 0xFFFFFF);
		this.scale = view.getFloatOr("scale", 1);
	}

	public void setRotation(int rotation) {
		this.rotation = rotation;
		setChanged();
	}
}
