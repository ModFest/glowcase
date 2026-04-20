package dev.hephaestus.glowcase.mixin.client.bakedbe;

import dev.hephaestus.glowcase.client.render.block.entity.BakedBlockEntityRenderer;
import dev.hephaestus.glowcase.mixinsupport.BakingBlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin implements BakingBlockEntityRenderDispatcher {
	@Shadow private Vec3 cameraPos;

	@Shadow public abstract @Nullable <E extends BlockEntity, S extends BlockEntityRenderState> BlockEntityRenderer<E, S> getRenderer(E blockEntity);

	@Nullable
	@Override
	public <E extends BlockEntity, S extends BlockEntityRenderState> S glowcase$tryExtractBakingRenderState(final E blockEntity) {
		BlockEntityRenderer<E, ?> entityRenderer = this.getRenderer(blockEntity);

		if (!(entityRenderer instanceof BakedBlockEntityRenderer<?, ?, ?>)) return null;
		if (!blockEntity.hasLevel() || !blockEntity.getType().isValid(blockEntity.getBlockState())) return null;

		BakedBlockEntityRenderer<E, ?, S> renderer = (BakedBlockEntityRenderer<E, ?, S>) entityRenderer;
		if (!renderer.shouldBake(blockEntity)) return null;

		S state = renderer.createBakedRenderState();
		renderer.extractBakingRenderState(blockEntity, state);
		return state;
	}
}
