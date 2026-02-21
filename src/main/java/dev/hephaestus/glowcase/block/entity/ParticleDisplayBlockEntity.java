package dev.hephaestus.glowcase.block.entity;

import com.mojang.logging.LogUtils;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.util.DeviatedInteger;
import dev.hephaestus.glowcase.util.DeviatedVec3d;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ParticleDisplayBlockEntity extends GlowcaseBlockEntity {
	private static final Logger LOGGER = LogUtils.getLogger();

	public ParticleOptions particle = ParticleTypes.FLAME;
	public DeviatedVec3d position = DeviatedVec3d.ZERO;
	public DeviatedVec3d velocity = DeviatedVec3d.ZERO;
	public DeviatedInteger count = DeviatedInteger.ZERO;
	public DeviatedInteger tickRate = DeviatedInteger.ZERO;

	private int tickCounter = 0;

	public ParticleDisplayBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.PARTICLE_DISPLAY_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);

		view.store("particle", ParticleTypes.CODEC, this.particle);
		view.store("position", DeviatedVec3d.CODEC, this.position);
		view.store("velocity", DeviatedVec3d.CODEC, this.velocity);
		view.store("count", DeviatedInteger.CODEC, this.count);
		view.store("tick_rate", DeviatedInteger.CODEC, this.tickRate);
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		this.particle = view.read("particle", ParticleTypes.CODEC).orElse(ParticleTypes.FLAME);
		this.position = view.read("position", DeviatedVec3d.CODEC).orElse(DeviatedVec3d.ZERO);
		this.velocity = view.read("velocity", DeviatedVec3d.CODEC).orElse(DeviatedVec3d.ZERO);
		this.count = view.read("count", DeviatedInteger.CODEC).orElse(DeviatedInteger.ZERO);
		this.tickRate = view.read("tick_rate", DeviatedInteger.CODEC).orElse(DeviatedInteger.ZERO);
	}

	@Environment(EnvType.CLIENT)
	public static void clientTick(Level world, BlockPos pos, BlockState state, ParticleDisplayBlockEntity entity) {
		entity.tickCounter--;
		if (entity.tickCounter > 0) return;

		entity.tickCounter = entity.tickRate.get(world.random::nextDouble);
		for (int i = 0; i < entity.count.get(world.random::nextDouble); i++) {
			Vec3 particlePos = entity.position.get(world.random::nextGaussian).add(pos.getCenter());
			Vec3 particleVelocity = entity.velocity.get(world.random::nextGaussian);

			world.addParticle(
				entity.particle,
				particlePos.x, particlePos.y, particlePos.z,
				particleVelocity.x, particleVelocity.y, particleVelocity.z
			);
		}
	}
}
