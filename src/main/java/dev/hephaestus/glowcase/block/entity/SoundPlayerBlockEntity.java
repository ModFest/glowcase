package dev.hephaestus.glowcase.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import dev.hephaestus.glowcase.Glowcase;
//import dev.hephaestus.glowcase.client.util.SoundPlayerProxy;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.util.Locale;

public class SoundPlayerBlockEntity extends GlowcaseBlockEntity {
	private static final Logger LOGGER = LogUtils.getLogger();

	public Identifier soundId = SoundEvents.ENTITY_CAT_PURREOW.id();
	public SoundCategory category = SoundCategory.BLOCKS;
	public float volume = 1;
	public float pitch = 1;
	public int repeatDelay = 0;
	public float distance = 16;
	public boolean relative = false;
	public Vec3d offset = Vec3d.ZERO;
	public boolean cancelOthers = false;
	public PositionSampler volumeSampler = PositionSampler.CAMERA;

	public PositionedSoundLoop nowPlaying = null;

	public SoundPlayerBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.SOUND_BLOCK_ENTITY.get(), pos, state);
	}

	public void cycleCategory() {
		this.category = SoundCategory.values()[(this.category.ordinal() + 1) % SoundCategory.values().length];
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);

		view.put("sound", Identifier.CODEC, this.soundId);
		view.putString("category", this.category.name());
		view.putFloat("volume", this.volume);
		view.putFloat("pitch", this.pitch);
		view.putInt("repeatDelay", this.repeatDelay);
		view.putFloat("distance", this.distance);
		view.putBoolean("relative", this.relative);
		view.putBoolean("cancelOthers", this.cancelOthers);
		view.put("offset", Vec3d.CODEC, this.offset);
		view.put("volumeSampler", PositionSampler.CODEC, volumeSampler);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);

		this.soundId = view.read("sound", Identifier.CODEC).orElseGet(SoundEvents.ENTITY_CAT_PURREOW::id);

		this.category = SoundCategory.valueOf(view.getString("category", SoundCategory.BLOCKS.name()));
		this.volume = view.getFloat("volume", 1);
		this.pitch = view.getFloat("pitch", 1);
		this.repeatDelay = view.getInt("repeatDelay", 0);
		this.distance = view.getFloat("distance", 16);
		this.relative = view.getBoolean("relative", false);
		this.cancelOthers = view.getBoolean("cancelOthers", false);
		this.offset = view.read("offset", Vec3d.CODEC).orElse(Vec3d.ZERO);
		this.volumeSampler = view.read("volumeSampler", PositionSampler.CODEC).orElse(PositionSampler.CAMERA);
	}

	@Environment(EnvType.CLIENT)
	public static void clientTick(World world, BlockPos pos, BlockState state, SoundPlayerBlockEntity entity) {
		final MinecraftClient client = MinecraftClient.getInstance();
		final SoundManager soundManager = client.getSoundManager();

		final PositionedSoundLoop oldInstance = entity.nowPlaying;
		if (oldInstance != null) {
			// Todo
			//if (oldInstance.isCompatible() && ((SoundPlayerProxy) soundManager).glowcase$isQueuedOrPlaying(oldInstance)) {
				// no-op when already playing something, or waiting to be played
			//	return;
			//}

			soundManager.stop(oldInstance);
		}

		final Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
		final Vec3d sourcePos = entity.getSourcePos();

		if (cameraPos.squaredDistanceTo(sourcePos) > entity.distanceSquared()) {
			return;
		}

		if (entity.cancelOthers) {
			soundManager.stopSounds(null, entity.category);
		}

		PositionedSoundLoop sound = new PositionedSoundLoop(entity);

		entity.nowPlaying = sound;

		soundManager.play(sound);
	}

	private Vec3d getSoundPos() {
		if (relative) {
			return offset;
		}

		return pos.toCenterPos().add(offset);
	}

	private Vec3d getSourcePos() {
		if (relative) {
			return pos.toCenterPos();
		}

		return pos.toCenterPos().add(offset);
	}

	private float distanceSquared() {
		return this.distance * this.distance;
	}


	public enum PositionSampler implements StringIdentifiable {
		CAMERA {
			@Override
			public Vec3d getPosition(final MinecraftClient client) {
				return client.gameRenderer.getCamera().getPos();
			}
		},
		PLAYER {
			@Override
			public Vec3d getPosition(final MinecraftClient client) {
				if (client.player == null) {
					return Vec3d.ZERO;
				}
				return client.player.getEntityPos();
			}
		};

		public static final Codec<PositionSampler> CODEC = StringIdentifiable.createCodec(PositionSampler::values);

		public abstract Vec3d getPosition(MinecraftClient client);

		@Override
		public String asString() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	// I don't think the repeat is necessary on this at this point
	public static class PositionedSoundLoop extends AbstractSoundInstance implements TickableSoundInstance {
		private final SoundPlayerBlockEntity soundBlock;

		private boolean done;

		public PositionedSoundLoop(SoundPlayerBlockEntity soundBlock) {
			super(soundBlock.soundId, soundBlock.category, SoundInstance.createRandom());
			this.repeat = true;
			this.attenuationType = AttenuationType.NONE;
			this.relative = soundBlock.relative;
			this.soundBlock = soundBlock;
			this.done = false;
			this.copyData();
		}

		@Override
		public boolean isDone() {
			return this.done;
		}

		public void setDone() {
			this.repeat = false;
			this.done = true;
		}

		@Override
		public void tick() {
			if (this.soundBlock.isRemoved() || this.soundBlock.nowPlaying != this) {
				this.setDone();
				return;
			}

			final MinecraftClient client = MinecraftClient.getInstance();

			// If the worlds don't match, stop.
			if (this.soundBlock.getWorld() != client.world) {
				this.setDone();
				return;
			}

			if (!inRange(client.player, client.gameRenderer.getCamera())) {
				setDone();
				return;
			}

			copyData();
		}

		private void copyData() {
			this.setPos(soundBlock.getSoundPos());
			this.volume = this.soundBlock.volume;
			this.pitch = this.soundBlock.pitch;
			this.repeatDelay = this.soundBlock.repeatDelay;
		}

		private void setPos(Vec3d pos) {
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
		}

		@Override
		public float getVolume() {
			var originalVolume = super.getVolume();

			return originalVolume * linearFalloff();
		}

		private float linearFalloff() {
			final Vec3d position = this.soundBlock.volumeSampler.getPosition(MinecraftClient.getInstance());
			float distanceToCamera = (float) this.soundBlock.getSourcePos().distanceTo(position);
			return 1 - (distanceToCamera / this.soundBlock.distance);
		}

		public boolean inRange(ClientPlayerEntity player, Camera camera) {
			final float maxDistSquared = this.soundBlock.distanceSquared();
			final Vec3d sourcePos = this.soundBlock.getSourcePos();

			if (camera.getPos().squaredDistanceTo(sourcePos) <= maxDistSquared) {
				return true;
			}

			return player.squaredDistanceTo(sourcePos) <= maxDistSquared;
		}

		public boolean isCompatible() {
			if (this.isDone() || this.soundBlock.nowPlaying != this) {
				return false;
			}

			return this.relative == this.soundBlock.relative &&
				this.id.equals(this.soundBlock.soundId) &&
				this.category.equals(this.soundBlock.category);
		}
	}
}
