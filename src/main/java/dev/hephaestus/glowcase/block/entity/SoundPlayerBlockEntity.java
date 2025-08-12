package dev.hephaestus.glowcase.block.entity;

import com.mojang.logging.LogUtils;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.util.SoundPlayerProxy;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SoundPlayerBlockEntity extends GlowcaseBlockEntity {
	private static final Logger LOGGER = LogUtils.getLogger();

	public Identifier soundId = SoundEvents.ENTITY_CAT_PURREOW.getId();
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
	protected void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(tag, registryLookup);

		RegistryOps<NbtElement> ops = registryLookup.getOps(NbtOps.INSTANCE);
		Identifier.CODEC.encodeStart(ops, this.soundId)
			.resultOrPartial(LOGGER::error)
			.ifPresent(result -> tag.put("sound", result));
		tag.putString("category", this.category.toString());
		tag.putFloat("volume", this.volume);
		tag.putFloat("pitch", this.pitch);
		tag.putInt("repeatDelay", this.repeatDelay);
		tag.putFloat("distance", this.distance);
		tag.putBoolean("relative", this.relative);
		tag.putBoolean("cancelOthers", this.cancelOthers);
		Vec3d.CODEC.encodeStart(ops, this.offset)
			.resultOrPartial(LOGGER::error)
			.ifPresent(result -> tag.put("offset", result));
		tag.putString("volumeSampler", volumeSampler.name());
	}

	@Override
	protected void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(tag, registryLookup);

		RegistryOps<NbtElement> ops = registryLookup.getOps(NbtOps.INSTANCE);
		if (tag.contains("sound"))
			Identifier.CODEC.parse(ops, tag.get("sound"))
				.resultOrPartial(LOGGER::error)
				.ifPresent(result -> this.soundId = result);
		this.category = SoundCategory.valueOf(tag.getString("category"));
		this.volume = tag.getFloat("volume");
		this.pitch = tag.getFloat("pitch");
		this.repeatDelay = tag.getInt("repeatDelay");
		this.distance = tag.getFloat("distance");
		this.relative = tag.getBoolean("relative");
		this.cancelOthers = tag.getBoolean("cancelOthers");
		if (tag.contains("offset"))
			Vec3d.CODEC.parse(ops, tag.get("offset"))
				.resultOrPartial(LOGGER::error)
				.ifPresent(result -> this.offset = result);

		if (tag.contains("volumeSampler", NbtElement.STRING_TYPE)) {
			final PositionSampler sampler = PositionSampler.getByName(tag.getString("volumeSampler"));
			if (sampler != null) {
				this.volumeSampler = sampler;
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static void clientTick(World world, BlockPos pos, BlockState state, SoundPlayerBlockEntity entity) {
		final MinecraftClient client = MinecraftClient.getInstance();
		final SoundManager soundManager = client.getSoundManager();

		final PositionedSoundLoop oldInstance = entity.nowPlaying;
		if (oldInstance != null) {
			if (oldInstance.isCompatible() && ((SoundPlayerProxy) soundManager).glowcase$isQueuedOrPlaying(oldInstance)) {
				// no-op when already playing something, or waiting to be played
				return;
			}

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


	public enum PositionSampler {
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
				return client.player.getPos();
			}
		},
		;

		private static final Map<String, PositionSampler> lookup;

		static {
			final Map<String, PositionSampler> samplers = new HashMap<>();
			for (final PositionSampler sampler : values()) {
				samplers.put(sampler.name().toLowerCase(Locale.ROOT), sampler);
			}
			lookup = Map.copyOf(samplers);
		}

		public static PositionSampler getByName(String value) {
			if (value == null) {
				return null;
			}
			return lookup.get(value.toLowerCase(Locale.ROOT));
		}

		public abstract Vec3d getPosition(MinecraftClient client);
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
