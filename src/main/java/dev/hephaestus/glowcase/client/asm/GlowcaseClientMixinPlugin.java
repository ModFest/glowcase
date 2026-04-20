package dev.hephaestus.glowcase.client.asm;

import dev.hephaestus.glowcase.asm.GlowcaseMixinPlugin;
import dev.hephaestus.glowcase.asm.MethodGenerator;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class GlowcaseClientMixinPlugin implements GlowcaseMixinPlugin.SidedMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (!FabricLoader.getInstance().isModLoaded("sodium")) {
			return !mixinClassName.startsWith("dev.hephaestus.glowcase.mixin.client.bakedbe.sodium");
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		sodiumCompat(targetClassName, targetClass, mixinClassName, mixinInfo);
	}

	private void sodiumCompat(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		if (!FabricLoader.getInstance().isModLoaded("sodium")) return;

		if (targetClassName.equals("net.minecraft.client.renderer.MultiBufferSource$BufferSource")) {
			var acceleratedSort = MethodGenerator.getFirstMethod(targetClass, ACC_PRIVATE | ACC_STATIC, "acceleratedSort");
			acceleratedSort.access &= ~ACC_PRIVATE;
			acceleratedSort.access |= ACC_PROTECTED;
		}
	}
}
