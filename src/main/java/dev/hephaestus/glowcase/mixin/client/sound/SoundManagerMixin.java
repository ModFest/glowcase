package dev.hephaestus.glowcase.mixin.client.sound;

import dev.hephaestus.glowcase.client.util.SoundPlayerProxy;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Ampflower
 * @implNote Functions here generally need to proxy to the {@link SoundSystem sound system}.
 * @see SoundSystemMixin
 **/
@Mixin(SoundManager.class)
public class SoundManagerMixin implements SoundPlayerProxy {
	@Shadow
	@Final
	private SoundSystem soundSystem;

	@Override
	public boolean glowcase$isQueued(final SoundInstance sound) {
		return ((SoundPlayerProxy) this.soundSystem).glowcase$isQueued(sound);
	}

	@Override
	public boolean glowcase$isQueuedOrPlaying(final SoundInstance sound) {
		return ((SoundPlayerProxy) this.soundSystem).glowcase$isQueuedOrPlaying(sound);
	}
}
