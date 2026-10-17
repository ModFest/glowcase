package dev.hephaestus.glowcase.block.entity;

import com.mojang.serialization.Codec;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.util.PartPoseInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@NullMarked
public class EntityDisplayBlockEntity extends DisplayBlockEntity implements StackInteractable {
	public static final TagKey<EntityType<?>> TICK = TagKey.create(Registries.ENTITY_TYPE, Glowcase.id("tick_in_display"));

	protected int entityId;
	protected @Nullable Entity displayEntity = null;
	protected @Nullable EntityType<?> entityType = null;
	protected Map<String, PartPoseInfo> partPoses = new HashMap<>();
	protected boolean isBaby = false;

	public EntityDisplayBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.ENTITY_DISPLAY_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	public boolean matchesStack(ItemStack stack) {
		return (stack.isEmpty() && displayEntity == null) || (stack.getItem() instanceof SpawnEggItem eggItem && SpawnEggItem.spawnsEntity( stack, entityType));
	}

	@Override
	public void setFromStack(ItemStack stack) {
		if (level == null) return;

		if (stack.getItem() instanceof SpawnEggItem) {
			EntityType<?> type = SpawnEggItem.getType(stack);
			if (type == null) return;

			setDisplayEntity(type.create(level, EntitySpawnReason.EVENT));
			if (displayEntity == null) return;

			setScale(new Vector3f(Math.clamp(Math.round(Math.min(1F / displayEntity.getBbHeight(), 1F / displayEntity.getBbWidth()) * 8F) / 8F, 0.125F, 10F)));
		}
	}

	@Override
	public void unsetFromStack() {
		clearPartPoses();
		setDisplayEntity(null);
	}

	public @Nullable Entity getDisplayEntity() {
		return this.displayEntity;
	}

	public void setDisplayEntity(@Nullable Entity displayEntity) {
		this.displayEntity = displayEntity;
		this.entityType = displayEntity == null ? null : displayEntity.getType();
		applyBabyStateToDisplayEntity();
		this.setChanged();
	}

	public static void tick(Level world, BlockPos blockPos, BlockState state, EntityDisplayBlockEntity blockEntity) {
		if (blockEntity.displayEntity == null && blockEntity.entityType != null) {
			Entity displayEntity = blockEntity.entityType.create(world, EntitySpawnReason.EVENT);
			if (displayEntity == null) return;

			displayEntity.snapTo(blockPos.getX(),blockPos.getY(),blockPos.getZ());

			if (blockEntity.entityId != 0) displayEntity.setId(blockEntity.entityId);
			blockEntity.setDisplayEntity(displayEntity);
		}

		if (blockEntity.displayEntity != null && blockEntity.displayEntity.getType().builtInRegistryHolder().is(TICK)) {
			blockEntity.displayEntity.tickCount++;
		}
	}

	public Map<String,PartPoseInfo> getPartPoses(){
		return partPoses;
	}

	public void loadPartPoses(Map<String, PartPoseInfo> newPartPoses) {
		this.partPoses.putAll(newPartPoses);
	}

	public void clearPartPoses(){
		this.partPoses.clear();
	}

	public PartPoseInfo getPartPostInfo(String name) {
		if(partPoses.containsKey(name)) {
			return partPoses.get(name);
		}
		return new PartPoseInfo(name,new Vector3f(0), new Vector3f(0));
	}

	public void setPartPoseInfo(String name, PartPoseInfo partPoseInfo) {
		this.partPoses.put(name,partPoseInfo);
	}

	public void setBaby(boolean baby) {
		this.isBaby = baby;
		applyBabyStateToDisplayEntity();
	}

	public void applyBabyStateToDisplayEntity(){
		if(displayEntity != null && displayEntity instanceof Mob displayMob) {
			displayMob.setBaby(isBaby());
		}
	}

	public boolean isBaby(){
		return this.isBaby;
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);
		if (entityType != null) {
			view.store("type", EntityType.CODEC, entityType);
			view.store("isBaby", Codec.BOOL, isBaby);
		}
		if(!partPoses.isEmpty()) {
			view.store("partPoses", Codec.unboundedMap(Codec.STRING, PartPoseInfo.CODEC), partPoses);
		}
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		this.entityId = view.getIntOr("entityId", 0);
		this.entityType = view.read("type", EntityType.CODEC).orElse(null);
		this.displayEntity = null;

		this.isBaby = view.read("isBaby", Codec.BOOL).orElse(false);

		this.partPoses.clear();
		Optional<Map<String, PartPoseInfo>> optionalStringPartPoseInfoMap = view.read("partPoses", Codec.unboundedMap(Codec.STRING, PartPoseInfo.CODEC));
		optionalStringPartPoseInfoMap.ifPresent(stringPartPoseInfoMap -> this.partPoses.putAll(stringPartPoseInfoMap));
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
		var tag = super.getUpdateTag(registryLookup);
		if (displayEntity != null) {
			tag.putInt("entityId", displayEntity.getId());
		}
		return tag;
	}
}
