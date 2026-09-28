package dev.hephaestus.glowcase.mixin.client.bakedbe;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.hephaestus.glowcase.client.render.bakedbe.RenderOffset;
import dev.hephaestus.glowcase.mixinsupport.BakeryDynamicUniforms;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import net.minecraft.core.Vec3i;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@NullMarked
@Mixin(DynamicGpuData.class)
public class DynamicGpuDataMixin implements BakeryDynamicUniforms {
	// int -> enabled
	// iVec3 -> block pos
	// vec3 -> decimal offset from block pos
	@Unique private static final int glowcase$FOG_OFFSET_UBO_SIZE = new Std140SizeCalculator()
		.putInt()
		.putIVec3()
		.putVec3()
		.get();
	@Unique private DynamicGpuDataStorageMapped<RenderOffset> glowcase$fogOffset;
	@Unique private GpuBuffer glowcase$noFogOffset;

	@SuppressWarnings("ShadowFinalModification")
	@Inject(at = @At("RETURN"), method = "<init>")
	private void setFogOffsetValues(CallbackInfo ci) {
		this.glowcase$fogOffset = new DynamicGpuDataStorageMapped<>("Glowcase Fog Offset UBO", glowcase$FOG_OFFSET_UBO_SIZE, GpuBuffer.USAGE_UNIFORM, 2);

		try (MemoryStack stack = MemoryStack.stackPush()) {
			ByteBuffer byteBuffer = stack.malloc(glowcase$FOG_OFFSET_UBO_SIZE);
			Std140Builder.intoBuffer(byteBuffer)
				.putInt(0)
				.putIVec3(0, 0, 0)
				.putVec3(0, 0, 0);
			this.glowcase$noFogOffset = RenderSystem.getDevice().createBuffer(() -> "Empty Glowcase Fog Offset", GpuBuffer.USAGE_UNIFORM, byteBuffer.flip());
		}
	}

	@Override
	public GpuBufferSlice glowcase$writeFogOffset(Vec3i blockPos, Vector3f offset) {
		return this.glowcase$fogOffset.writeData(new RenderOffset(null, blockPos.toMutable(), offset, new Vector3f().add(blockPos.getX(), blockPos.getY(), blockPos.getZ()).add(offset)));
	}

	@Override
	public GpuBufferSlice[] glowcase$writeFogOffsets(RenderOffset... fogTransforms) {
		return this.glowcase$fogOffset.writeData(fogTransforms);
	}

	@Override
	public GpuBufferSlice glowcase$noFogOffset() {
		return glowcase$noFogOffset.slice();
	}

	@Inject(at = @At("RETURN"), method = "reset")
	private void resetGlowcaseStuff(CallbackInfo ci) {
		this.glowcase$fogOffset.endFrame();
	}

	@Inject(at = @At("RETURN"), method = "close")
	private void closeGlowcaseStuff(CallbackInfo ci) {
		this.glowcase$fogOffset.close();
		this.glowcase$noFogOffset.close();
	}
}
