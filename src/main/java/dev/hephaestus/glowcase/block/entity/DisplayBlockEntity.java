package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.util.DisplayBlockSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

public abstract class DisplayBlockEntity extends GlowcaseBlockEntity {
	private Vector3f offset = new Vector3f(0.0F);
	private Vector3f scale = new Vector3f(1.0F);
	private float pitch = 0.0F;
	private float yaw = 0.0F;

	public DisplayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public DisplayBlockSettings toSettings() {
		return new DisplayBlockSettings(
			new Vector3f(offset.x(), offset.y(), offset.z()),
			new Vector3f(scale.x(), scale.y(), scale.z()),
			pitch,
			yaw
		);
	}

	public void loadSettings(DisplayBlockSettings settings) {
		this.offset.set(settings.offset().x(), settings.offset().y(), settings.offset().z());
		this.scale.set(settings.scale().x(), settings.scale().y(), settings.scale().z());
		this.pitch = settings.pitch();
		this.yaw = settings.yaw();
		markDirty();
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		DisplayBlockSettings settings = toSettings();
		if (!settings.isEmpty()) nbt.put("display", DisplayBlockSettings.CODEC.encode(settings, NbtOps.INSTANCE, nbt).getOrThrow());
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		if (nbt.contains("display")) {
			var result = DisplayBlockSettings.CODEC.decode(NbtOps.INSTANCE, nbt.getCompound("display"));
			if (result.isSuccess()) {
				loadSettings(result.getOrThrow().getFirst());
				return;
			}
		}
		loadSettings(new DisplayBlockSettings());
	}

	public Vector3f getOffset() {
		return offset;
	}

	public Vector3f getScale() {
		return scale;
	}

	public float getYaw() {
		return yaw;
	}

	public float getPitch() {
		return pitch;
	}

	public void setOffset(Vector3f offset) {
		this.offset = offset;
		markDirty();
	}

	public void setScale(Vector3f scale) {
		this.scale = scale;
		markDirty();
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
		markDirty();
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
		markDirty();
	}
}
