package dev.hephaestus.glowcase.asm;

import dev.hephaestus.glowcase.client.asm.GlowcaseClientAsm;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class GlowcaseAsm implements Runnable {

	@Override
	public void run() {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			new GlowcaseClientAsm().run();
		}
	}
}
