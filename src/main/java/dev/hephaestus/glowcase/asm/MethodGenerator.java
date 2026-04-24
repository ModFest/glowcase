package dev.hephaestus.glowcase.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;

public class MethodGenerator extends GeneratorAdapter {
	public static final String NOARG_VOID_DESCRIPTOR = "()V";
	private final ClassNode classNode;
	private final MethodNode methodNode;
	private LabelNode start;
	private LabelNode end;

	protected MethodGenerator(final ClassNode classNode, final MethodNode methodNode) {
		this(classNode, methodNode, methodNode.access, methodNode.name, methodNode.desc);
	}

	protected MethodGenerator(final ClassNode classNode, final MethodVisitor methodVisitor, final int access, final String name, final String descriptor) {
		super(Opcodes.ASM9, methodVisitor, access, name, descriptor);
		this.classNode = classNode;
		this.methodNode = (mv instanceof MethodNode node) ? node : null;
	}

	public static MethodGenerator create(final ClassNode classNode, final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
		MethodVisitor methodVisitor = classNode.visitMethod(access, name, descriptor, signature, exceptions);
		return new MethodGenerator(classNode, methodVisitor, access, name, descriptor);
	}

	public static MethodGenerator create(final ClassNode classVisitor, final int access, final String name, final String descriptor) {
		return create(classVisitor, access, name, descriptor, null, null);
	}

	public static MethodGenerator of(final ClassNode classNode, final int access, final String name, final String descriptor) {
		for (MethodNode method : classNode.methods) {
			if (method.name.equals(name) && method.access == access && method.desc.equals(descriptor)) {
				return new MethodGenerator(classNode, method);
			}
		}

		throw new NoSuchElementException("No method with access " + access + ", name " + name + " and descriptor " + descriptor + " found");
	}

	public static MethodGenerator replace(final ClassNode classNode, final int access, final String name, final String descriptor) {
		ListIterator<MethodNode> iterator = classNode.methods.listIterator();
		while (iterator.hasNext()) {
			MethodNode method = iterator.next();
			if (method.name.equals(name) && method.access == access && method.desc.equals(descriptor)) {
				iterator.remove();
				return create(classNode, method.access, method.name, method.desc, method.signature, method.exceptions.toArray(new String[0]));
			}
		}

		throw new NoSuchElementException("No method with access " + access + ", name " + name + " and descriptor " + descriptor + " found");
	}

	public static MethodNode getFirstMethod(final ClassNode classNode, final int access, final String name) {
		for (MethodNode method : classNode.methods) {
			if (method.name.equals(name) && method.access == access) {
				return method;
			}
		}

		throw new NoSuchElementException("No method with access " + access + " and name " + name + " found");
	}

	public static MethodNode getMethod(final ClassNode classNode, final int access, final String name, final String descriptor) {
		for (MethodNode method : classNode.methods) {
			if (method.desc.equals(descriptor) && method.name.equals(name) && method.access == access) {
				return method;
			}
		}

		throw new NoSuchElementException("No method with access " + access + " and name " + name + " found");
	}

	public void invokeConstructor(final String owner) {
		invokeConstructor(owner, NOARG_VOID_DESCRIPTOR);
	}

	public void invokeSuper() {
		invokeSuper(NOARG_VOID_DESCRIPTOR);
	}

	public void invokeSuper(final String descriptor) {
		if (classNode.superName == null) throw new UnsupportedOperationException("Cannot ASM into Object");
		invokeConstructor(classNode.superName, descriptor);
	}

	public void invokeConstructor(final String owner, final String descriptor) {
		mv.visitMethodInsn(INVOKESPECIAL, owner, "<init>", descriptor, false);
	}

	public void putInstanceField(final String name, final String descriptor) {
		mv.visitFieldInsn(PUTFIELD, classNode.name, name, descriptor);
	}

	public void putField(final String owner, final String name, final String descriptor) {
		mv.visitFieldInsn(PUTFIELD, owner, name, descriptor);
	}

	public void getField(final String owner, final String name, final String descriptor) {
		mv.visitFieldInsn(GETFIELD, owner, name, descriptor);
	}

	public void newInstance(final String type) {
		mv.visitTypeInsn(NEW, type);
	}

	public void instanceOf(final String type) {
		mv.visitTypeInsn(INSTANCEOF, type);
	}

	public void checkCast(final String type) {
		mv.visitTypeInsn(Opcodes.CHECKCAST, type);
	}

	public int storeLocal(final String type) {
		Type asmType = Type.getType(type);
		int index = newLocal(asmType);
		mv.visitVarInsn(asmType.getOpcode(Opcodes.ISTORE), index);
		return index;
	}

	public void invokeStatic(String owner, String name, String descriptor) {
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, descriptor, false);
	}

	public void invokeVirtual(String owner, String name, String descriptor) {
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, descriptor, false);
	}

	public void pushNull() {
		mv.visitInsn(Opcodes.ACONST_NULL);
	}

	public Label getStartLabel() {
		assertMethodNode();
		if (start != null) return start.getLabel();

		var instructions = methodNode.instructions;
		start = new LabelNode(newLabel());

		instructions.insert(start);
		return start.getLabel();
	}

	public void visitStart() {
		visitLabel(getStartLabel());
	}

	public AbstractInsnNode findFirstInst(int opcode) {
		assertMethodNode();
		for (AbstractInsnNode node : methodNode.instructions) {
			if (node.getOpcode() == opcode) {
				return node;
			}
		}

		throw new NoSuchElementException("Could not find an instruction with opcode " + opcode);
	}

	public MethodNode methodNode() {
		return methodNode;
	}

	public void assertMethodNode() {
		if (methodNode == null) throw new IllegalStateException("Method visitor is not a MethodNode");
	}
}
