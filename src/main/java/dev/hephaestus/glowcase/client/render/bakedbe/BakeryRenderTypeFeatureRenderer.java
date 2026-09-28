package dev.hephaestus.glowcase.client.render.bakedbe;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.hephaestus.glowcase.mixinsupport.RenderTypeInfoForBaking;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.function.Consumer;

@NullMarked
public class BakeryRenderTypeFeatureRenderer<Submit extends SubmitNode> extends RenderTypeFeatureRenderer<Submit> {
	private static final Vector4fc WHITE = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
	private static final Vector3fc NO_OFFSET = new Vector3f();

	public BakeryRenderTypeFeatureRenderer(List<RenderTypeFeatureRenderer.Group> groups) {
	    this.groups.addAll(groups);
	}

	public void prepare(RenderOffset renderOffset, GpuBufferSlice offsetBuffer, List<DynamicGpuData.Transform> transforms, List<List<RenderTypeInfoForBaking>> infosForBaking) {
		if (this.groups.isEmpty()) return;
		var minecraft = Minecraft.getInstance();

		var modelViewMatrix = RenderSystem.getModelViewMatrixCopy().translate(renderOffset.vector());

		for (Group group : this.groups) {
			for (PreparedRenderType renderType : group.drawRenderTypes) {
				var infoForBaking = (RenderTypeInfoForBaking) (Object) renderType;
				boolean updateTextures = false;
				for (PreparedRenderType.Texture texture : renderType.textures()) {
					updateTextures |= texture.sampler().isClosed();
					updateTextures |= texture.textureView().isClosed();
				}

				if (updateTextures) {
					infoForBaking.glowcase$setTextures(
						infoForBaking.glowcase$getState().prepareTextures(
							minecraft.getTextureManager(),
							RenderSystem.getSamplerCache(),
							minecraft.gameRenderer.overlayTexture().getTextureView(),
							minecraft.gameRenderer.lightmap()
						)
					);
				}

				infoForBaking.glowcase$setFogOffset(offsetBuffer);

				var transform = makeDynamicTransforms(infoForBaking, modelViewMatrix);
				List<RenderTypeInfoForBaking> infos;
				var index = transforms.indexOf(transform);
				if (index != -1) {
					infos = infosForBaking.get(index);
				} else {
					transforms.add(transform);
					infosForBaking.add(infos = new ObjectArrayList<>());
				}
				infos.add(infoForBaking);
			}
		}
	}

	private DynamicGpuData.Transform makeDynamicTransforms(RenderTypeInfoForBaking infoForBaking, Matrix4f modelViewMatrix) {
		Consumer<Matrix4f> modelViewModifier = infoForBaking.glowcase$getLayeringTransform().getModifier();
		if (modelViewModifier != null) {
			modelViewModifier.accept(modelViewMatrix);
		}

		return new DynamicGpuData.Transform(modelViewMatrix, WHITE, NO_OFFSET, infoForBaking.glowcase$getTextureTransform().createMatrix());
	}

	@Override
	protected void buildGroup(FeatureFrameContext context, List<Submit> submits) {
		throw new UnsupportedOperationException();
	}
}
