package dev.hephaestus.glowcase.mixin.client.sound;

import dev.hephaestus.glowcase.client.util.SoundPlayerProxy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;

/**
 * @author Ampflower
 **/
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin implements SoundPlayerProxy {
	@Shadow
	public abstract boolean isActive(final SoundInstance sound);

	@Shadow
	@Final
	private Map<SoundInstance, Integer> queuedSounds;

	@Shadow
	@Final
	private List<TickableSoundInstance> queuedTickableSounds;

	@Override
	public boolean glowcase$isQueued(final SoundInstance sound) {
		return this.queuedSounds.containsKey(sound) ||
			this.queuedTickableSounds.contains(sound);
	}

	@Override
	public boolean glowcase$isQueuedOrPlaying(final SoundInstance sound) {
		return this.isActive(sound) || this.glowcase$isQueued(sound);
	}
}
