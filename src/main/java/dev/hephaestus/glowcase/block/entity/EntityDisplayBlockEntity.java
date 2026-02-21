package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3f;

public class EntityDisplayBlockEntity extends DisplayBlockEntity implements StackInteractable {
	public static final TagKey<EntityType<?>> TICK = TagKey.create(Registries.ENTITY_TYPE, Glowcase.id("tick_in_display"));

	protected Entity displayEntity = null;
	protected EntityType<?> entityType = null;

	public EntityDisplayBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.ENTITY_DISPLAY_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	public boolean matchesStack(ItemStack stack) {
		return (stack.isEmpty() && displayEntity == null) || (stack.getItem() instanceof SpawnEggItem eggItem && eggItem.spawnsEntity(level.registryAccess(), stack, entityType));
	}

	@Override
	public void setFromStack(ItemStack stack) {
		if (stack.getItem() instanceof SpawnEggItem eggItem) {
			setDisplayEntity(eggItem.getType(level.registryAccess(), stack).create(level, EntitySpawnReason.EVENT));
			setScale(new Vector3f(Math.clamp(Math.round(Math.min(1F / displayEntity.getBbHeight(), 1F / displayEntity.getBbWidth()) * 8F) / 8F, 0.125F, 10F)));
		}
	}

	@Override
	public void unsetFromStack() {
		setDisplayEntity(null);
	}

	public Entity getDisplayEntity() {
		return this.displayEntity;
	}

	public void setDisplayEntity(Entity displayEntity) {
		this.displayEntity = displayEntity;
		this.entityType = displayEntity == null ? null : displayEntity.getType();
		this.setChanged();
	}

	public static void tick(Level world, BlockPos blockPos, BlockState state, EntityDisplayBlockEntity blockEntity) {
		if (blockEntity.displayEntity == null && blockEntity.entityType != null) {
			blockEntity.setDisplayEntity(blockEntity.entityType.create(world, EntitySpawnReason.EVENT));
		}
		if (blockEntity.getDisplayEntity() != null && blockEntity.getDisplayEntity().getType().is(TICK)) {
			++blockEntity.displayEntity.tickCount;
		}
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);
		if (entityType != null) {
			view.store("type", EntityType.CODEC, entityType);
		}
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);

		this.entityType = view.read("type", EntityType.CODEC).orElse(null);
		this.displayEntity = null;
	}
}
