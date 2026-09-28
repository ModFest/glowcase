package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.llamalad7.mixinextras.sugar.Local;
import dev.hephaestus.glowcase.client.render.block.entity.BakedBlockEntityRenderer;
import dev.hephaestus.glowcase.mixinsupport.BakingBlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;extractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", shift = At.Shift.AFTER), method = "tryExtractRenderState")
	private <E extends BlockEntity, S extends BlockEntityRenderState> void checkAndMarkAsBaking(
		CallbackInfoReturnable<S> cir,
		@Local(name = "renderer") BlockEntityRenderer<E, S> renderer,
		@Local(name = "state") S state
		) {
		if (renderer instanceof BakedBlockEntityRenderer<E, S> bakery && bakery.shouldBake(state)) {
			var identity = bakery.renderStateIdentity(state);
			((BakingBlockEntityRenderState) state).glowcase$setIdentity(identity);
		}
	}
}
