package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import dev.hephaestus.glowcase.mixinsupport.BakingRendererExtension;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface BakedBlockEntityRenderer<T extends BlockEntity, U extends BlockEntityRenderState, B extends BlockEntityRenderState> extends BlockEntityRenderer<T, U>, BakingRendererExtension {
	/// The extract stage for the unbaked rendering
	@Override
	default void extractRenderState(T blockEntity, U state, float partialTicks, Vec3 cameraPosition, @Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
	}

	B createBakedRenderState();

	/// The extract stage for the baked rendering
	default void extractBakingRenderState(final T blockEntity, final B state) {
		BlockEntityRenderState.extractBase(blockEntity, state, null);
	}

	/// Internal override of the submit method for renaming it into a clearer one
	///
	/// @deprecated Don't call directly, use {@link #submitForRendering} or {@link #submitForBaking}
	@Deprecated
	@ApiStatus.Internal
	@ApiStatus.NonExtendable
	default void submit(U state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		submitForRendering(state, poseStack, submitNodeCollector, camera);
	}

	/// Submit steps for unbaked rendering. This works exactly the same way as a normal BER render method, and can be used for dynamic
	/// rendering that changes every frame. \
	/// In this method you can also check for render invalidation and call {@link GlowcaseLevelRenderer#setBlockDirty} as appropriate.
	void submitForRendering(U state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera);

	/// Submit steps for baking into the render region. This method will be called every time the render region is rebuilt - so
	/// you should only render vertices that don't move here. You can call {@link GlowcaseLevelRenderer#setBlockDirty} to
	/// cause the render region to be rebuilt, but do not call this too frequently as it will affect performance. \
	/// You must use the provided SubmitNodeCollector and PoseStack to render your vertices - any use of Tesselator
	/// or RenderSystem here will not work.
	void submitForBaking(B state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector);

	/// Defines if the part should be baked. This is only checked when baking is required.
	///
	/// @param blockEntity The part being checked
	/// @return if part should be baked
	boolean shouldBake(T blockEntity);

	@Override
	@ApiStatus.Internal
	@ApiStatus.NonExtendable
	default boolean glowcase$isBakingRenderer() {
		return true;
	}
}
