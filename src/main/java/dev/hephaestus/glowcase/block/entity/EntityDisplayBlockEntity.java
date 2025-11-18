package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class EntityDisplayBlockEntity extends DisplayBlockEntity implements StackInteractable {
	public static final TagKey<EntityType<?>> TICK = TagKey.of(RegistryKeys.ENTITY_TYPE, Glowcase.id("tick_in_display"));

	protected Entity displayEntity = null;
	protected EntityType<?> entityType = null;

	public EntityDisplayBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.ENTITY_DISPLAY_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	public boolean matchesStack(ItemStack stack) {
		return (stack.isEmpty() && displayEntity == null) || (stack.getItem() instanceof SpawnEggItem eggItem && eggItem.isOfSameEntityType(stack, entityType));
	}

	@Override
	public void setFromStack(ItemStack stack) {
		if (stack.getItem() instanceof SpawnEggItem eggItem) {
			setDisplayEntity(eggItem.getEntityType(stack).create(world, SpawnReason.EVENT));
			setScale(new Vector3f(Math.clamp(Math.round(Math.min(1F / displayEntity.getHeight(), 1F / displayEntity.getWidth()) * 8F) / 8F, 0.125F, 10F)));
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
		this.markDirty();
	}

	public static void tick(World world, BlockPos blockPos, BlockState state, EntityDisplayBlockEntity blockEntity) {
		if (blockEntity.displayEntity == null && blockEntity.entityType != null) {
			blockEntity.setDisplayEntity(blockEntity.entityType.create(world, SpawnReason.EVENT));
		}
		if (blockEntity.getDisplayEntity() != null && blockEntity.getDisplayEntity().getType().isIn(TICK)) {
			++blockEntity.displayEntity.age;
		}
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		if (entityType != null) {
			view.put("type", EntityType.CODEC, entityType);
		}
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);

		this.entityType = view.read("type", EntityType.CODEC).orElse(null);
		this.displayEntity = null;
	}
}
