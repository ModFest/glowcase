package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.GlowcaseClient;
import dev.hephaestus.glowcase.client.util.RRVClientUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class RecipeBlockEntity extends GlowcaseBlockEntity {
	public String recipe = "diamond_sword";
	public TextBlockEntity.ZOffset zOffset = TextBlockEntity.ZOffset.CENTER;

	//TODO: maybe move XYZ rotation to nbt? or use vec2f
	public float rotationX = 0f;
	public float rotationY = 0f;

	public RecipeBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.RECIPE_BLOCK_ENTITY.get(), pos, state);
	}

	@Environment(EnvType.CLIENT)
	public void openRecipe() {
		Identifier rid = Identifier.tryParse(recipe);
		if (GlowcaseClient.RRV_LOADED) {
			RRVClientUtils.displayRecipe(rid);
		}
	}

	public void setRecipe(String newRecipe) {
		recipe = newRecipe;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);

		view.putString("recipe", this.recipe);
		view.store("z_offset", TextBlockEntity.ZOffset.CODEC, this.zOffset);
		view.putFloat("rotationX", this.rotationX);
		view.putFloat("rotationY", this.rotationY);
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		this.recipe = view.getStringOr("recipe", "diamond_sword");
		this.zOffset = view.read("z_offset", TextBlockEntity.ZOffset.CODEC).orElse(TextBlockEntity.ZOffset.CENTER);
		this.rotationX = view.getFloatOr("rotationX", 0);
		this.rotationY = view.getFloatOr("rotationY", 0);
	}
}
