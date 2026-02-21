package dev.hephaestus.glowcase.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.util.SoundPlayerProxy;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Locale;

public class SoundPlayerBlockEntity extends GlowcaseBlockEntity {
	private static final Logger LOGGER = LogUtils.getLogger();

	public ResourceLocation soundId = SoundEvents.CAT_PURREOW.location();
	public SoundSource category = SoundSource.BLOCKS;
	public float volume = 1;
	public float pitch = 1;
	public int repeatDelay = 0;
	public float distance = 16;
	public boolean relative = false;
	public Vec3 offset = Vec3.ZERO;
	public boolean cancelOthers = false;
	public PositionSampler volumeSampler = PositionSampler.CAMERA;

	public PositionedSoundLoop nowPlaying = null;

	public SoundPlayerBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.SOUND_BLOCK_ENTITY.get(), pos, state);
	}

	public void cycleCategory() {
		this.category = SoundSource.values()[(this.category.ordinal() + 1) % SoundSource.values().length];
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);

		view.store("sound", ResourceLocation.CODEC, this.soundId);
		view.putString("category", this.category.name());
		view.putFloat("volume", this.volume);
		view.putFloat("pitch", this.pitch);
		view.putInt("repeatDelay", this.repeatDelay);
		view.putFloat("distance", this.distance);
		view.putBoolean("relative", this.relative);
		view.putBoolean("cancelOthers", this.cancelOthers);
		view.store("offset", Vec3.CODEC, this.offset);
		view.store("volumeSampler", PositionSampler.CODEC, volumeSampler);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		this.soundId = view.read("sound", ResourceLocation.CODEC).orElseGet(SoundEvents.CAT_PURREOW::location);

		this.category = SoundSource.valueOf(view.getStringOr("category", SoundSource.BLOCKS.name()));
		this.volume = view.getFloatOr("volume", 1);
		this.pitch = view.getFloatOr("pitch", 1);
		this.repeatDelay = view.getIntOr("repeatDelay", 0);
		this.distance = view.getFloatOr("distance", 16);
		this.relative = view.getBooleanOr("relative", false);
		this.cancelOthers = view.getBooleanOr("cancelOthers", false);
		this.offset = view.read("offset", Vec3.CODEC).orElse(Vec3.ZERO);
		this.volumeSampler = view.read("volumeSampler", PositionSampler.CODEC).orElse(PositionSampler.CAMERA);
	}

	@Environment(EnvType.CLIENT)
	public static void clientTick(Level world, BlockPos pos, BlockState state, SoundPlayerBlockEntity entity) {
		final Minecraft client = Minecraft.getInstance();
		final SoundManager soundManager = client.getSoundManager();

		final PositionedSoundLoop oldInstance = entity.nowPlaying;
		if (oldInstance != null) {
			if (oldInstance.isCompatible() && ((SoundPlayerProxy) soundManager).glowcase$isQueuedOrPlaying(oldInstance)) {
				// no-op when already playing something, or waiting to be played
				return;
			}

			soundManager.stop(oldInstance);
		}

		final Vec3 cameraPos = client.gameRenderer.getMainCamera().getPosition();
		final Vec3 sourcePos = entity.getSourcePos();

		if (cameraPos.distanceToSqr(sourcePos) > entity.distanceSquared()) {
			return;
		}

		if (entity.cancelOthers) {
			soundManager.stop(null, entity.category);
		}

		PositionedSoundLoop sound = new PositionedSoundLoop(entity);

		entity.nowPlaying = sound;

		soundManager.play(sound);
	}

	private Vec3 getSoundPos() {
		if (relative) {
			return offset;
		}

		return worldPosition.getCenter().add(offset);
	}

	private Vec3 getSourcePos() {
		if (relative) {
			return worldPosition.getCenter();
		}

		return worldPosition.getCenter().add(offset);
	}

	private float distanceSquared() {
		return this.distance * this.distance;
	}


	public enum PositionSampler implements StringRepresentable {
		CAMERA {
			@Override
			public Vec3 getPosition(final Minecraft client) {
				return client.gameRenderer.getMainCamera().getPosition();
			}
		},
		PLAYER {
			@Override
			public Vec3 getPosition(final Minecraft client) {
				if (client.player == null) {
					return Vec3.ZERO;
				}
				return client.player.position();
			}
		};

		public static final Codec<PositionSampler> CODEC = StringRepresentable.fromEnum(PositionSampler::values);

		public abstract Vec3 getPosition(Minecraft client);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	// I don't think the repeat is necessary on this at this point
	public static class PositionedSoundLoop extends AbstractSoundInstance implements TickableSoundInstance {
		private final SoundPlayerBlockEntity soundBlock;

		private boolean done;

		public PositionedSoundLoop(SoundPlayerBlockEntity soundBlock) {
			super(soundBlock.soundId, soundBlock.category, SoundInstance.createUnseededRandom());
			this.looping = true;
			this.attenuation = Attenuation.NONE;
			this.relative = soundBlock.relative;
			this.soundBlock = soundBlock;
			this.done = false;
			this.copyData();
		}

		@Override
		public boolean isStopped() {
			return this.done;
		}

		public void setDone() {
			this.looping = false;
			this.done = true;
		}

		@Override
		public void tick() {
			if (this.soundBlock.isRemoved() || this.soundBlock.nowPlaying != this) {
				this.setDone();
				return;
			}

			final Minecraft client = Minecraft.getInstance();

			// If the worlds don't match, stop.
			if (this.soundBlock.getLevel() != client.level) {
				this.setDone();
				return;
			}

			if (!inRange(client.player, client.gameRenderer.getMainCamera())) {
				setDone();
				return;
			}

			copyData();
		}

		private void copyData() {
			this.setPos(soundBlock.getSoundPos());
			this.volume = this.soundBlock.volume;
			this.pitch = this.soundBlock.pitch;
			this.delay = this.soundBlock.repeatDelay;
		}

		private void setPos(Vec3 pos) {
			this.x = pos.x();
			this.y = pos.y();
			this.z = pos.z();
		}

		@Override
		public float getVolume() {
			var originalVolume = super.getVolume();

			return originalVolume * linearFalloff();
		}

		private float linearFalloff() {
			final Vec3 position = this.soundBlock.volumeSampler.getPosition(Minecraft.getInstance());
			float distanceToCamera = (float) this.soundBlock.getSourcePos().distanceTo(position);
			return 1 - (distanceToCamera / this.soundBlock.distance);
		}

		public boolean inRange(LocalPlayer player, Camera camera) {
			final float maxDistSquared = this.soundBlock.distanceSquared();
			final Vec3 sourcePos = this.soundBlock.getSourcePos();

			if (camera.getPosition().distanceToSqr(sourcePos) <= maxDistSquared) {
				return true;
			}

			return player.distanceToSqr(sourcePos) <= maxDistSquared;
		}

		public boolean isCompatible() {
			if (this.isStopped() || this.soundBlock.nowPlaying != this) {
				return false;
			}

			return this.relative == this.soundBlock.relative &&
				this.location.equals(this.soundBlock.soundId) &&
				this.source.equals(this.soundBlock.category);
		}
	}
}
