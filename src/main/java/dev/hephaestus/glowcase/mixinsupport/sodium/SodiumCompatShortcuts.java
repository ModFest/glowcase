package dev.hephaestus.glowcase.mixinsupport.sodium;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder.Result;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hephaestus.glowcase.mixin.client.bakedbe.sodium.BufferSourceSodiumInjectionsAccessor;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicTopoData;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;

public class SodiumCompatShortcuts {
	@SuppressWarnings("DataFlowIssue") // We don't care about nullability here
	public static @Nullable Result buildSortedIndexBuffer(VertexFormat.IndexType indexType, ByteBufferBuilder bufferBuilder, int[] primitiveIds) {
		return BufferSourceSodiumInjectionsAccessor.callBuildSortedIndexBuffer(
			new MeshData(null, new MeshData.DrawState(null, 0, 0, null, indexType)),
			bufferBuilder,
			primitiveIds
		);
	}

	@SuppressWarnings("DataFlowIssue") // We don't care about nullability here
	public static DynamicTopoData createDummyTopoData(SectionPos sectionPos) {
		return DynamicTopoData.fromMesh(null, null, sectionPos, null);
	}
}
