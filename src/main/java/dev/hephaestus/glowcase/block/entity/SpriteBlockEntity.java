package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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
			Optional<Item> item = Registries.ITEM.getOrEmpty(Identifier.tryParse(newSprite));
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
	public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(tag, registryLookup);

		tag.putString("sprite", this.sprite);
		tag.putInt("rotation", this.rotation);
		tag.putString("z_offset", this.zOffset.name());
		tag.putInt("color", this.color);
		tag.putFloat("scale", this.scale);
	}

	@Override
	public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(tag, registryLookup);

		setSprite(tag.getString("sprite"));
		this.rotation = tag.getInt("rotation");
		this.zOffset = TextBlockEntity.ZOffset.valueOf(tag.getString("z_offset"));
		this.color = tag.getInt("color");
		this.scale = tag.getFloat("scale");
	}

	public void setRotation(int rotation) {
		this.rotation = rotation;
		markDirty();
	}
}
