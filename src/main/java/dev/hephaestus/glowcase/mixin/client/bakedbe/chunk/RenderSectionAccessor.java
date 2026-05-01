package dev.hephaestus.glowcase.mixin.client.bakedbe.chunk;

import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public interface RenderSectionAccessor {
	@Accessor SectionRenderDispatcher getThis$0();
	@Invoker void callReleaseSectionMesh(final SectionMesh oldMesh);
}
