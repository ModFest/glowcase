package dev.hephaestus.glowcase.mixin.client.sound;

import dev.hephaestus.glowcase.client.util.SoundPlayerProxy;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.TickableSoundInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;

/**
 * @author Ampflower
 **/
@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin implements SoundPlayerProxy {
	@Shadow
	public abstract boolean isPlaying(final SoundInstance sound);

	@Shadow
	@Final
	private Map<SoundInstance, Integer> startTicks;

	@Shadow
	@Final
	private List<TickableSoundInstance> soundsToPlayNextTick;

	@Override
	public boolean glowcase$isQueued(final SoundInstance sound) {
		return this.startTicks.containsKey(sound) ||
			this.soundsToPlayNextTick.contains(sound);
	}

	@Override
	public boolean glowcase$isQueuedOrPlaying(final SoundInstance sound) {
		return this.isPlaying(sound) || this.glowcase$isQueued(sound);
	}
}
