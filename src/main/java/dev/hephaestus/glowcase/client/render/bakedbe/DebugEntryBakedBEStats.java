package dev.hephaestus.glowcase.client.render.bakedbe;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.mixinsupport.BakeryRenderDebug;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DebugEntryBakedBEStats implements DebugScreenEntry {
	private static final Identifier GROUP = Glowcase.id("bakery_stats");
	@Override
	public void display(
		final DebugScreenDisplayer displayer,
		@Nullable final Level serverOrClientLevel,
		@Nullable final LevelChunk clientChunk,
		@Nullable final LevelChunk serverChunk
	) {
		displayer.addToGroup(GROUP, ((BakeryRenderDebug) Minecraft.getInstance().levelRenderer).glowcase$getBakeryStats());
	}

	@Override
	public boolean isAllowed(final boolean reducedDebugInfo) {
		return true;
	}
}
