package dev.hephaestus.glowcase.util;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import net.minecraft.client.renderer.rendertype.RenderType;

public class RenderTypeUtil {
	public static boolean hasAlphaBlending(RenderType renderType) {
		return !renderType.pipeline().getColorTargetState().blendFunction()
			.map(BlendFunction::destAlpha)
			.map(destAlpha ->
				destAlpha == DestFactor.CONSTANT_ALPHA ||
				destAlpha == DestFactor.ONE ||
				destAlpha == DestFactor.ZERO
			)
			.orElse(true);
	}
}
