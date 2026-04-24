package dev.hephaestus.glowcase.mixinsupport;

import dev.hephaestus.glowcase.client.render.bakedbe.level.GlowcaseLevelRenderer;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;

public interface LevelRendererExtension {
    GlowcaseLevelRenderer glowcase$getLevelRenderer();
}
