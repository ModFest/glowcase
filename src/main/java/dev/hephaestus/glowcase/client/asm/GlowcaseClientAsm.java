package dev.hephaestus.glowcase.client.asm;

import com.chocohead.mm.api.ClassTinkerers;
import dev.hephaestus.glowcase.asm.MethodGenerator;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.PUTFIELD;

public class GlowcaseClientAsm implements Runnable {
	@Override
	public void run() {
		outlineBufferConstructors();
		byteBufferBuilderConstructors();
		bakedBeBufferSourceSodiumCompat();
	}

	private void outlineBufferConstructors() {
		ClassTinkerers.addTransformation("net.minecraft.client.renderer.OutlineBufferSource", classNode -> {
			var altConstructor = MethodGenerator.create(
				classNode,
				ACC_PROTECTED,
				"<init>",
				"(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"
			);

			// Call super
			altConstructor.loadThis();
			altConstructor.invokeSuper();
			// Set the buffer source to the 1st constructor param
			altConstructor.loadThis();
			altConstructor.loadArg(0);
			altConstructor.putInstanceField(
				"outlineBufferSource",
				"Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
			);
			// Set the outline color to the default value of -1
			altConstructor.loadThis();
			altConstructor.visitInsn(ICONST_M1);
			altConstructor.visitFieldInsn(PUTFIELD, "net/minecraft/client/renderer/OutlineBufferSource", "outlineColor", "I");

			// End
			altConstructor.returnValue();
			altConstructor.endMethod();
		});

		ClassTinkerers.addTransformation("dev.hephaestus.glowcase.client.render.bakedbe.buffers.DummyOutlineBufferSource", classNode -> {
			var constructor = MethodGenerator.of(classNode, ACC_PUBLIC, "<init>", MethodGenerator.NOARG_VOID_DESCRIPTOR);

			constructor.loadThis();
			// Create a dummy buffer source instance
			constructor.newInstance("dev/hephaestus/glowcase/client/render/bakedbe/buffers/DummyBufferSource");
			constructor.dup();
			constructor.invokeConstructor("dev/hephaestus/glowcase/client/render/bakedbe/buffers/DummyBufferSource");
			// Call super with the dummy buffer source as param
			constructor.invokeSuper("(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V");

			// End
			constructor.returnValue();
			constructor.endMethod();
		});
	}

	private void byteBufferBuilderConstructors() {
		ClassTinkerers.addTransformation("com.mojang.blaze3d.vertex.ByteBufferBuilder", classNode -> {
			var altConstructor = MethodGenerator.create(classNode, ACC_PROTECTED, "<init>", MethodGenerator.NOARG_VOID_DESCRIPTOR);

			// Call Object super
			altConstructor.loadThis();
			altConstructor.invokeSuper();

			// Set capacity
			altConstructor.loadThis();
			altConstructor.visitInsn(LCONST_0);
			altConstructor.putInstanceField("capacity", "J");

			// Set maxCapacity
			altConstructor.loadThis();
			altConstructor.visitInsn(LCONST_0);
			altConstructor.putInstanceField("maxCapacity", "J");

			// Set pointer
			altConstructor.loadThis();
			altConstructor.visitInsn(LCONST_0);
			altConstructor.putInstanceField("pointer", "J");

			// Set generation
			altConstructor.loadThis();
			altConstructor.visitInsn(ICONST_M1);
			altConstructor.putInstanceField("generation", "I");

			// End
			altConstructor.returnValue();
			altConstructor.endMethod();
		});

		ClassTinkerers.addTransformation("dev.hephaestus.glowcase.client.render.bakedbe.buffers.DummyByteBufferBuilder", classNode -> {
			var constructor = MethodGenerator.replace(classNode, ACC_PUBLIC, "<init>", MethodGenerator.NOARG_VOID_DESCRIPTOR);

			// Call super
			constructor.loadThis();
			constructor.invokeSuper();

			// End
			constructor.returnValue();
			constructor.endMethod();
		});
	}

	private void bakedBeBufferSourceSodiumCompat() {
		if (!FabricLoader.getInstance().isModLoaded("sodium")) return;

		ClassTinkerers.addTransformation("dev.hephaestus.glowcase.client.render.bakedbe.buffers.BakedBEBufferSource", classNode -> {
			var method = MethodGenerator.replaceFirst(classNode, ACC_PRIVATE, "useSodiumQuadSort");
			String vertexSortingExtended = "net/caffeinemc/mods/sodium/client/util/sorting/VertexSortingExtended";

			Label useVanilla = method.newLabel();
			Label end = method.newLabel();
			// Check if instanceof
			method.loadArg(2);
			method.instanceOf(vertexSortingExtended);
			method.visitJumpInsn(IFEQ, useVanilla);
			// Store instanceof cast
			method.loadArg(2);
			method.checkCast(vertexSortingExtended);
			int sortingExtended = method.storeLocal("L" + vertexSortingExtended + ";");

			// If instanceof
			method.loadArg(0);
			method.loadArg(1);
			method.loadLocal(sortingExtended);
			method.invokeStatic(
				"net/minecraft/client/renderer/MultiBufferSource$BufferSource",
				"acceleratedSort",
				"(Lcom/mojang/blaze3d/vertex/MeshData;Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;Lnet/caffeinemc/mods/sodium/client/util/sorting/VertexSortingExtended;)V"
			);
			method.goTo(end);
			method.mark(useVanilla);
			method.loadArg(0);
			method.loadArg(1);
			method.loadArg(2);
			method.invokeVirtual("com/mojang/blaze3d/vertex/MeshData", "sortQuads", "(Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;Lcom/mojang/blaze3d/vertex/VertexSorting;)Lcom/mojang/blaze3d/vertex/MeshData$SortState;");
			method.pop();

			// End
			method.mark(end);
			method.returnValue();
			method.endMethod();
		});
	}
}
