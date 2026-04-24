package dev.hephaestus.glowcase.mixin.client.bakedbe.chunk;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(SectionRenderDispatcher.class)
public interface SectionRenderDispatcherAccessor {
	@Accessor LevelRenderer getRenderer();
	@Accessor AtomicReference<Vec3> getCameraPosition();
}
