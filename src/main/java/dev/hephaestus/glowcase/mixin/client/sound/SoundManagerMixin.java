package dev.hephaestus.glowcase.mixin.client.sound;

import dev.hephaestus.glowcase.client.util.SoundPlayerProxy;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Ampflower
 * @implNote Functions here generally need to proxy to the {@link SoundEngine sound system}.
 * @see SoundSystemMixin
 **/
@Mixin(SoundManager.class)
public class SoundManagerMixin implements SoundPlayerProxy {
	@Shadow
	@Final
	private SoundEngine soundEngine;

	@Override
	public boolean glowcase$isQueued(final SoundInstance sound) {
		return ((SoundPlayerProxy) this.soundEngine).glowcase$isQueued(sound);
	}

	@Override
	public boolean glowcase$isQueuedOrPlaying(final SoundInstance sound) {
		return ((SoundPlayerProxy) this.soundEngine).glowcase$isQueuedOrPlaying(sound);
	}
}
