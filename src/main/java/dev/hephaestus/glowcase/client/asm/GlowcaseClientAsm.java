package dev.hephaestus.glowcase.client.asm;

import com.chocohead.mm.api.ClassTinkerers;
import dev.hephaestus.glowcase.asm.MethodGenerator;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.*;

import static dev.hephaestus.glowcase.asm.MethodGenerator.NOARG_VOID_DESCRIPTOR;
import static org.objectweb.asm.Opcodes.*;

public class GlowcaseClientAsm implements Runnable {
	@Override
	public void run() {
		outlineBufferConstructors();
		byteBufferBuilderConstructors();
		concurrentSheets();
		// sodiumCompat();
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
			var constructor = MethodGenerator.of(classNode, ACC_PUBLIC, "<init>", NOARG_VOID_DESCRIPTOR);

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
			var altConstructor = MethodGenerator.create(classNode, ACC_PROTECTED, "<init>", NOARG_VOID_DESCRIPTOR);

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
			var constructor = MethodGenerator.replace(classNode, ACC_PUBLIC, "<init>", NOARG_VOID_DESCRIPTOR);

			// Call super
			constructor.loadThis();
			constructor.invokeSuper();

			// End
			constructor.returnValue();
			constructor.endMethod();
		});
	}

	private void concurrentSheets() {
		ClassTinkerers.addTransformation("net.minecraft.client.renderer.Sheets", classNode -> {
			var clinit = MethodGenerator.of(classNode, ACC_STATIC, "<clinit>", NOARG_VOID_DESCRIPTOR);
			for (TypeInsnNode node : clinit.<TypeInsnNode>findInst(NEW, node -> node.desc.equals("java/util/HashMap"))) {
				node.desc = "java/util/concurrent/ConcurrentHashMap";
			}

			for (MethodInsnNode node : clinit.<MethodInsnNode>findInst(INVOKESPECIAL, node ->
				node.owner.equals("java/util/HashMap") &&
				node.name.equals("<init>") &&
				node.desc.equals(NOARG_VOID_DESCRIPTOR)
			)) {
				node.owner = "java/util/concurrent/ConcurrentHashMap";
			}
		});
	}

	private void sodiumCompat() {
		if (!FabricLoader.getInstance().isModLoaded("sodium")) return;

		ClassTinkerers.addTransformation("net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.SortTriggering", classNode -> {
			var method = MethodGenerator.create(classNode, ACC_PRIVATE, "glowcase$getDirect", "()Ldev/hephaestus/glowcase/mixinsupport/sodium/DirectTriggersExtension;");
			method.loadThis();
			// GETFIELD net/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/trigger/SortTriggering.direct : Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/trigger/DirectTriggers;
			method.getField(classNode.name, "direct", "Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/trigger/DirectTriggers;");
			method.returnValue();
			method.endMethod();
		});

		/*ClassTinkerers.addTransformation("net.minecraft.client.renderer.MultiBufferSource.BufferSource", classNode -> {
			var original = MethodGenerator.getFirstMethod(classNode, ACC_PRIVATE | ACC_STATIC, "buildSortedIndexBuffer");
			// (Lcom/mojang/blaze3d/vertex/MeshData;Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;[I)Lcom/mojang/blaze3d/vertex/ByteBufferBuilder$Result;
			var firstParamEnd = original.desc.indexOf(';');
			assert firstParamEnd != -1;
			var descStart = original.desc.substring(0, 2);
			var descRemainder = original.desc.substring(firstParamEnd);
			var desc = descStart + "com/mojang/blaze3d/vertex/VertexFormat$IndexType" + descRemainder;
			MethodGenerator method = MethodGenerator.create(classNode, original.access, original.name, desc, original.signature, null);
			original.accept(method); // Clone into the new one

			InsnList instructions = method.methodNode().instructions;
			AbstractInsnNode firstInvoke = method.findFirstInst(INVOKEVIRTUAL);
			AbstractInsnNode secondInvoke = firstInvoke.getNext();
			var labelNode = new LabelNode();
			instructions.insert(secondInvoke, labelNode);
			// Remove invokes using the MeshData param
			instructions.remove(firstInvoke);
			instructions.remove(secondInvoke);
			// Start writing where the old instructions were
			method.visitLabel(labelNode.getLabel());
			// Just load the 1st param and let it be stored as a local variable, it's a lot easier this way
			method.loadArg(0);

			method.endMethod();
		});*/
	}
}
