package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.util.DisplayBlockSettings;
import org.joml.Vector3f;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class DisplayBlockEntity extends GlowcaseBlockEntity {
	private Vector3f offset = new Vector3f(0.0F);
	private Vector3f scale = new Vector3f(1.0F);
	private float pitch = 0.0F;
	private float yaw = 0.0F;
	private boolean renderAsBlock = false;

	public DisplayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public DisplayBlockSettings toSettings() {
		return new DisplayBlockSettings(
			new Vector3f(offset.x(), offset.y(), offset.z()),
			new Vector3f(scale.x(), scale.y(), scale.z()),
			pitch,
			yaw,
			renderAsBlock
		);
	}

	public void loadSettings(DisplayBlockSettings settings) {
		this.offset.set(settings.offset().x(), settings.offset().y(), settings.offset().z());
		this.scale.set(settings.scale().x(), settings.scale().y(), settings.scale().z());
		this.pitch = settings.pitch();
		this.yaw = settings.yaw();
		this.renderAsBlock = settings.renderAsBlock();
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);
		DisplayBlockSettings settings = toSettings();
		if (!settings.isEmpty()) view.store("display", DisplayBlockSettings.CODEC, settings);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);
		loadSettings(view.read("display", DisplayBlockSettings.CODEC).orElseGet(DisplayBlockSettings::new));
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
		setChanged();
	}

	public void setScale(Vector3f scale) {
		this.scale = scale;
		setChanged();
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
		setChanged();
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
		setChanged();
	}

	public boolean getRenderAsBlock() {
		return renderAsBlock;
	}

	public void setRenderAsBlock(boolean renderAsBlock) {
		this.renderAsBlock = renderAsBlock;
		setChanged();
	}
}
