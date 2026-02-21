package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class OutlineBlockEntity extends GlowcaseBlockEntity {
	public Vec3i offset = Vec3i.ZERO;
	public Vec3i scale = new Vec3i(1, 1, 1);
	public int color = 0xFFFFFF;

	public OutlineBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.OUTLINE_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);

		view.store("offset", Vec3i.CODEC, this.offset);
		view.store("scale", Vec3i.CODEC, this.scale);
		view.putInt("color", this.color);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		this.offset = view.read("offset", Vec3i.CODEC).orElse(Vec3i.ZERO);
		this.scale = view.read("scale", Vec3i.CODEC).orElseGet(() -> new Vec3i(1, 1, 1));
		this.color = view.getIntOr("color", 0xFFFFFF);
	}
}
