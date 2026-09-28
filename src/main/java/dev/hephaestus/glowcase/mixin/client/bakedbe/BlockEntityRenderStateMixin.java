package dev.hephaestus.glowcase.mixin.client.bakedbe;

import dev.hephaestus.glowcase.client.render.bakedbe.Identity;
import dev.hephaestus.glowcase.mixinsupport.BakingBlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntityRenderState.class)
public class BlockEntityRenderStateMixin implements BakingBlockEntityRenderState {
	@Unique public @Nullable Identity glowcase$identity;

	@Shadow public BlockPos blockPos;

	@Override
	public void glowcase$setIdentity(Object identity) {
		this.glowcase$identity = new Identity(blockPos, identity);
	}

	@Override
	public @Nullable Identity glowcase$getIdentity() {
		return glowcase$identity;
	}
}
