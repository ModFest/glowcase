package dev.hephaestus.glowcase.mixin.client.bakedbe.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PoseStack.Pose.class)
public interface PoseAccessor {
	@Accessor boolean getTrustedNormals();
}
