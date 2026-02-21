package dev.hephaestus.glowcase.client.util;

import dev.hephaestus.glowcase.mixin.client.sound.SoundEngineMixin;
import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * A peek into the sound system to query whether sounds are queued.
 *
 * @author Ampflower
 * @see dev.hephaestus.glowcase.mixin.client.sound.SoundManagerMixin
 * @see SoundEngineMixin
 **/
public interface SoundPlayerProxy {
	/**
	 * Returns whether the sound instance is currently queued, but not playing.
	 *
	 * @param sound The sound instance to check for.
	 * @return Whether the given sound instance is queued, but not playing.
	 * @see #glowcase$isQueuedOrPlaying(SoundInstance)
	 * @see net.minecraft.client.sounds.SoundManager#isActive(SoundInstance)
	 */
	boolean glowcase$isQueued(SoundInstance sound);

	/**
	 * Returns whether the sound instance is currently queued or playing.
	 *
	 * @param sound The sound instance to check for.
	 * @return Whether the given sound instance is queued or currently playing.
	 * @see #glowcase$isQueued(SoundInstance)
	 * @see net.minecraft.client.sounds.SoundManager#isActive(SoundInstance)
	 */
	boolean glowcase$isQueuedOrPlaying(SoundInstance sound);
}
