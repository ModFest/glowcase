package dev.hephaestus.glowcase.asm;

import dev.hephaestus.glowcase.client.asm.GlowcaseClientMixinPlugin;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GlowcaseMixinPlugin implements IMixinConfigPlugin {
	private static final boolean IS_CLIENT = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	private final @Nullable SidedMixinConfigPlugin clientMixinPlugin;

	public GlowcaseMixinPlugin() {
		this.clientMixinPlugin = IS_CLIENT ? new GlowcaseClientMixinPlugin() : null;
	}

	@Override
	public void onLoad(String mixinPackage) {
		if (clientMixinPlugin != null) clientMixinPlugin.onLoad(mixinPackage);
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		boolean shouldApply = true;
		if (clientMixinPlugin != null) {
			shouldApply &= clientMixinPlugin.shouldApplyMixin(targetClassName, mixinClassName);
		}

		return shouldApply;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
		if (clientMixinPlugin != null) {
			clientMixinPlugin.acceptTargets(myTargets, otherTargets);
		}
	}

	@Override
	public List<String> getMixins() {
		List<String> mixins = null;
		if (clientMixinPlugin != null) {
			var sideMixins = clientMixinPlugin.getMixins();
			if (sideMixins != null) {
				if (mixins == null) mixins = new ArrayList<>();
				mixins.addAll(sideMixins);
			}
		}
		return mixins;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		if (clientMixinPlugin != null) {
			clientMixinPlugin.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
		}
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		if (clientMixinPlugin != null) {
			clientMixinPlugin.postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
		}
	}

	public interface SidedMixinConfigPlugin extends IMixinConfigPlugin {
		@Override
		default String getRefMapperConfig() {
			return null;
		}
	}
}
