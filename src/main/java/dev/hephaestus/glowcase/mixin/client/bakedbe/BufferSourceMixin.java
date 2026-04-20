package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.hephaestus.glowcase.client.render.bakedbe.chunk.SectionCompileQueue;
import dev.hephaestus.glowcase.mixinsupport.BakingBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.*;

@Mixin(value = MultiBufferSource.BufferSource.class, priority = 9999)
public class BufferSourceMixin implements BakingBufferSource {
    @Shadow @Nullable protected RenderType lastSharedType;
    @Shadow @Final protected SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers;
    @Shadow @Final protected Map<RenderType, BufferBuilder> startedBuilders;

    @Shadow @Final protected ByteBufferBuilder sharedBuffer;

    public Map<RenderType, MeshData> glowcase$bakeAllBatches(VertexSorting vertexSorting) {
        Map<RenderType, MeshData> meshData = new HashMap<>();
        this.startedBuilders.forEach((renderType, bufferBuilder) -> {
            if (bufferBuilder != null) {
                MeshData mesh = this.glowcase$bakeBatch(renderType, bufferBuilder, vertexSorting);
                if (mesh != null) meshData.put(renderType, mesh);
            }
        });

        this.startedBuilders.clear();

        return meshData;
    }
    
    @Override
    public Map<RenderType, MeshData> glowcase$bakeBatch(VertexSorting vertexSorting) {
        Map<RenderType, MeshData> meshData = new HashMap<>();
        for (RenderType renderType : this.fixedBuffers.keySet()) {
            MeshData mesh = this.glowcase$bakeBatch(renderType, vertexSorting);
            if (mesh != null) {
                meshData.put(renderType, mesh);
            }
        }
        
        return meshData;
    }

    @Unique
    public MeshData glowcase$bakeBatch(RenderType renderType, VertexSorting vertexSorting) {
        BufferBuilder bufferBuilder = this.startedBuilders.remove(renderType);
        if (bufferBuilder != null) {
            return this.glowcase$bakeBatch(renderType, bufferBuilder, vertexSorting);
        }

        return null;
    }

    @Unique
    public MeshData glowcase$bakeBatch(RenderType renderType, BufferBuilder bufferBuilder, VertexSorting vertexSorting) {
        MeshData meshData = bufferBuilder.build();
        if (meshData != null) {
            if (renderType.sortOnUpload()) {
                ByteBufferBuilder byteBufferBuilder = this.fixedBuffers.getOrDefault(renderType, this.sharedBuffer);
                meshData.sortQuads(byteBufferBuilder, vertexSorting);
            }
        }

        if (renderType.equals(this.lastSharedType)) {
            this.lastSharedType = null;
        }

        return meshData;
    }
}
