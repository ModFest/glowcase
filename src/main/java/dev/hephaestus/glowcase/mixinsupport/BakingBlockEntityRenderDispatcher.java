package dev.hephaestus.glowcase.mixinsupport;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface BakingBlockEntityRenderDispatcher {
	<E extends BlockEntity, S extends BlockEntityRenderState> S glowcase$tryExtractBakingRenderState(final E blockEntity);
}
