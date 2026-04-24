package dev.hephaestus.glowcase.asm;

import dev.hephaestus.glowcase.mixinsupport.RequireMod;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.io.IOException;
import java.util.*;

public class GlowcaseMixinPlugin implements IMixinConfigPlugin {
	private Map<String, RequireMod> annotatedPackages = new HashMap<>();

	@Override
	public void onLoad(String mixinPackage) {}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		try {
			IClassProvider classProvider = MixinService.getService().getClassProvider();
			RequireMod annotation = packageAnnotation(classProvider, mixinClassName);
			if (annotation != null) {
				return FabricLoader.getInstance().isModLoaded(annotation.value()) == annotation.present();
			}

			List<AnnotationNode> annotationNodes = MixinService.getService()
				.getBytecodeProvider()
				.getClassNode(mixinClassName)
				.visibleAnnotations;

			AnnotationNode node = Annotations.get(annotationNodes, Type.getDescriptor(RequireMod.class));
			if (node == null) return true;

			String modId = Annotations.getValue(node, "value");
			boolean present = Annotations.getValue(node, "present", Boolean.TRUE);

			return FabricLoader.getInstance().isModLoaded(modId) == present;
		} catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private RequireMod packageAnnotation(IClassProvider classProvider, String mixinClassName) {
		String packageName = mixinClassName.substring(0, mixinClassName.lastIndexOf('.'));
		return annotatedPackages.computeIfAbsent(packageName, _ -> {
			try {
				return classProvider.findClass(packageName + ".package-info").getDeclaredAnnotation(RequireMod.class);
			} catch (ClassNotFoundException _) {
				return null;
			}
		});
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
