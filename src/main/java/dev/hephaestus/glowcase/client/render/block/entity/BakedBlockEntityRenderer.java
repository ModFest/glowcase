package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BakedBlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> extends BlockEntityRenderer<T, S> {
	/// An identity for the render state, used for the baked rendering, similar to how the model identity used by the GUI item atlas.
	///
	/// This is the key for getting the cached mesh, a different value will cause the mesh to be rebaked. \
	/// The returned objects must be comparable with {@link #equals}
	Object renderStateIdentity(final S state);

	/// Submit steps for unbaked rendering. This works exactly the same way as a normal BER render method, and can be used for dynamic
	/// rendering that changes every frame.
	@Override
	void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera);

	/// Submit steps for baking into the render region. This method will be called every time the render region is rebuilt - so
	/// you should only render vertices that don't move here. \
	/// You must use the provided SubmitNodeCollector and PoseStack to render your vertices - any use of Tesselator
	/// or RenderSystem here will not work.
	void submitForBaking(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector);

	/// Defines if baking should be done for this block entity.
	///
	/// @param state The render state
	/// @return if it should be baked
	boolean shouldBake(S state);
}
